package com.pbl4.cameraclient.service;

import com.pbl4.cameraclient.model.DiscoveredCamera;
import com.pbl4.cameraclient.network.NetworkUtils;

import javafx.concurrent.Task;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CameraDiscoveryService - WS-Discovery (MulticastSocket) + Active LAN scan (tối ưu).
 */
public class CameraDiscoveryService {

    private static final String DISCOVERY_ADDRESS = "239.255.255.250";
    private static final int DISCOVERY_PORT = 3702;
    private static final int WS_TIMEOUT_MS = 4000; // tổng thời gian chờ WS-Discovery
    private static final Pattern XADDRS_PATTERN = Pattern.compile("<(?:d:)?XAddrs>(.*?)</(?:d:)?XAddrs>");
    private static final String PROBE_MESSAGE_TEMPLATE =
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">" +
                    "  <s:Header>" +
                    "    <a:Action s:mustUnderstand=\"1\">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</a:Action>" +
                    "    <a:MessageID>uuid:{0}</a:MessageID>" +
                    "    <a:ReplyTo>" +
                    "      <a:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address>" +
                    "    </a:ReplyTo>" +
                    "    <a:To s:mustUnderstand=\"1\">urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To>" +
                    "  </s:Header>" +
                    "  <s:Body>" +
                    "    <Probe xmlns=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\">" +
                    "      <Types xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\">dn:NetworkVideoTransmitter</Types>" +
                    "    </Probe>" +
                    "  </s:Body>" +
                    "</s:Envelope>";

    // Cổng thường dùng cho camera
//    private static final int[] COMMON_CAMERA_PORTS = {80, 554, 8000, 8080, 8081, 81, 82, 88, 8554, 9000, 8001};
    private static final int[] COMMON_CAMERA_PORTS = {554, 8000};

    // Từ khóa nhận diện camera (mở rộng)
    private static final String[] CAMERA_KEYWORDS = {
            "onvif", "ipcam", "ip-camera", "camera", "webcam", "hikvision", "dahua", "axis",
            "rtsp", "dvr", "nvr", "goahead", "netwave", "vivotek", "gstreamer", "surveillance", "network camera"
    };

    // Thời gian timeout cho kết nối/đọc
    private static final int CONNECT_TIMEOUT_MS = 800;
    private static final int READ_TIMEOUT_MS = 800;

    // Pool thread sizing: tận dụng CPU nhưng không quá lớn
    private static final int THREAD_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors() * 3);

    /**
     * Tạo Task quét: WS-Discovery (multicast) + Active LAN scan (optimized).
     */
    public Task<List<DiscoveredCamera>> createDiscoveryTask() {
        return new Task<>() {
            @Override
            protected List<DiscoveredCamera> call() throws Exception {
                updateMessage("Bắt đầu quét WS-Discovery và LAN scan...");
                System.out.println("=== START CAMERA DISCOVERY ===");

                final String messageId = UUID.randomUUID().toString();
                final byte[] probeBytes = PROBE_MESSAGE_TEMPLATE.replace("{0}", messageId)
                        .getBytes(StandardCharsets.UTF_8);

                // Thread-safe set để tránh trùng lặp IP
                Set<String> foundIps = ConcurrentHashMap.newKeySet();
                List<DiscoveredCamera> foundCameras = Collections.synchronizedList(new ArrayList<>());

                // ----------- 1) WS-Discovery bằng MulticastSocket (joinGroup trên từng NIC) -----------
                List<MulticastSocket> multicastSockets = new ArrayList<>();
                InetAddress group = InetAddress.getByName(DISCOVERY_ADDRESS);

                try {
                    Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                    for (NetworkInterface netIf : Collections.list(nets)) {
                        try {
                            if (!netIf.isUp() || netIf.isLoopback() || netIf.isVirtual()) continue;
                        } catch (SocketException se) {
                            continue;
                        }

                        // Lấy IPv4 hợp lệ trên interface
                        List<InetAddress> addrs = Collections.list(netIf.getInetAddresses()).stream()
                                .filter(a -> a instanceof Inet4Address)
                                .filter(a -> !a.isLoopbackAddress())
                                .collect(Collectors.toList());

                        for (InetAddress localAddr : addrs) {
                            try {
                                MulticastSocket ms = new MulticastSocket(null);
                                ms.setReuseAddress(true);
                                ms.bind(new InetSocketAddress(localAddr, 0));
                                ms.setSoTimeout(300);
                                ms.setNetworkInterface(netIf);
                                // join nhóm multicast cho interface này
                                ms.joinGroup(new InetSocketAddress(group, DISCOVERY_PORT), netIf);
                                multicastSockets.add(ms);

                                DatagramPacket packet = new DatagramPacket(probeBytes, probeBytes.length, group, DISCOVERY_PORT);
                                ms.send(packet);
                                System.out.println("[WS] Probe sent from " + localAddr.getHostAddress() + " on " + netIf.getDisplayName());
                            } catch (Exception e) {
                                System.err.println("[WS] Can't send probe from " + netIf.getDisplayName() + " (" + e.getMessage() + ")");
                            }
                        }
                    }

                    long wsStart = System.currentTimeMillis();
                    while (System.currentTimeMillis() - wsStart < WS_TIMEOUT_MS) {
                        boolean any = false;
                        for (MulticastSocket ms : multicastSockets) {
                            try {
                                byte[] buf = new byte[8192];
                                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                                ms.receive(recv); // đợi tối đa soTimeout
                                any = true;
                                String xml = new String(recv.getData(), 0, recv.getLength(), StandardCharsets.UTF_8);
                                DiscoveredCamera dc = parseDiscoveryResponse(xml);
                                if (dc != null && foundIps.add(dc.getIpAddress())) {
                                    foundCameras.add(dc);
                                    updateMessage("Tìm thấy (ONVIF): " + dc.getIpAddress());
                                    System.out.println("[WS] Found " + dc.getIpAddress());
                                }
                            } catch (SocketTimeoutException ste) {
                                // no data on this socket
                            } catch (IOException ioe) {
                                System.err.println("[WS] recv error: " + ioe.getMessage());
                            }
                        }
                        if (!any) Thread.sleep(50);
                        if (isCancelled()) break;
                    }
                } finally {
                    for (MulticastSocket ms : multicastSockets) {
                        try {
                            ms.leaveGroup(new InetSocketAddress(group, DISCOVERY_PORT), ms.getNetworkInterface());
                        } catch (Exception ignored) {}
                        try { ms.close(); } catch (Exception ignored) {}
                    }
                }

                // ----------- 2) Active LAN scan -----------
                updateMessage("Đang quét dải LAN (active scan)...");
                List<String> localIPv4s = getLocalIPv4Addresses();
                if (localIPv4s.isEmpty()) {
                    System.out.println("⚠️ Không tìm thấy IPv4 cục bộ — dùng subnet mặc định 192.168.0.* và 192.168.1.*");
                }

                ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
                List<Future<?>> submitted = new ArrayList<>();

                // Build list of subnets to scan (unique)
                Set<String> subnets = new LinkedHashSet<>();
                for (String ip : localIPv4s) {
                	
                    subnets.add(getSubnetPrefix(ip));
                    System.out.println(ip);
                }

                // For each subnet, submit one task per host (1..254)
                for (String subnet : subnets) {
                    for (int i = 1; i <= 254; i++) {
                        if (isCancelled()) break;
                        final String target = subnet + i;
//                        boolean canPing = NetworkUtils.isReachable(target, 5);
//                        if(canPing == false) {
//                        	continue;
//                        }
                        // Quick skip if already discovered by WS-Discovery
                        if (foundIps.contains(target)) continue;

                        submitted.add(pool.submit(() -> {
                            // check cancel & existing
                            if (isCancelled() || foundIps.contains(target)) return;

                            boolean anyOpen = false;
                            for (int port : COMMON_CAMERA_PORTS) {
                                if (isCancelled() || foundIps.contains(target)) return;
                                try (Socket s = new Socket()) {
                                    s.connect(new InetSocketAddress(target, port), CONNECT_TIMEOUT_MS);
                                    anyOpen = true;
                                    s.setSoTimeout(READ_TIMEOUT_MS);

                                    // Read banner: try to read some bytes quickly
//                                    String banner = readBanner(s, 8192, 30);
////                                    String lower = banner == null ? "" : banner.toLowerCase();
//                                    if(banner.isEmpty() == false) {
//                                    	banner =banner.toLowerCase();
//                                    }
//                                    else {
//                                        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
//                                        out.print("GET / HTTP/1.0\r\n\r\n");
//                                        out.flush();
//
//                                        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
//                                        StringBuilder sb = new StringBuilder();
//                                        String line;
//                                        int lines = 0;
//                                        while ((line = in.readLine()) != null && lines++ < 20) {
//                                            sb.append(line.toLowerCase()).append('\n');
//                                        }
//                                        banner = sb.toString();
//                                    }
//                                    if(banner != null) {
//                                    	for (String kw : CAMERA_KEYWORDS) {
//                                            if (banner.contains(kw)) {
//                                                if (foundIps.add(target)) {
//                                                    foundCameras.add(new DiscoveredCamera(target, "/"));
//                                                    updateMessage("Tìm thấy (scan): " + target + ":" + port);
//                                                    System.out.println("[SCAN] Found " + target + ":" + port + " (kw=" + kw + ")");
//                                                }
//                                                return; // Đã tìm thấy, thoát
//                                            }
//                                        }
//                                    	
//                                    }


                                    // If RTSP port, try quick OPTIONS
                                    try {
                                        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                                        out.print("OPTIONS rtsp://" + target + ":" + port + "/ RTSP/1.0\r\nCSeq: 1\r\n\r\n");
                                        out.flush();
                                        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                        String resp = br.readLine();
                                        if (resp != null && resp.toUpperCase().contains("RTSP")) {
                                            if (foundIps.add(target)) {
                                                foundCameras.add(new DiscoveredCamera(target, "/"));
                                                updateMessage("Tìm thấy (rtsp): " + target);
                                                System.out.println("[RTSP] Found " + target + ":" + port);
                                            }
                                            return;
                                        }
                                    } 
                                    catch (Exception ignored) {}

                                } 
                                catch (IOException ignored) {
                                    // not open or timed out -> continue to next port
                                }
                            } // end ports loop

                            // if some port was open but not matched, optionally add as open device
                            if (anyOpen) {
                                // We don't auto-add devices without keyword to avoid false positives
                            }
                        }));
                    }
                    if (isCancelled()) break;
                }

                // Wait for scan completion with a sensible timeout
                pool.shutdown();
                try {
                    boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);
                    if (!finished) {
                        pool.shutdownNow();
                    }
                } catch (InterruptedException ie) {
                    pool.shutdownNow();
                }

                // Build unique result list preserving insertion
                Map<String, DiscoveredCamera> uniq = new LinkedHashMap<>();
                for (DiscoveredCamera dc : foundCameras) {
                    uniq.putIfAbsent(dc.getIpAddress(), dc);
                }
                List<DiscoveredCamera> result = new ArrayList<>(uniq.values());

                updateMessage("Tìm thấy " + result.size() + " camera.");
                System.out.println("=== DISCOVERY COMPLETE: " + result.size() + " devices ===");
                return result;
            }
        };
    }

    // Hỗ trợ đọc banner nhanh: read up to maxBytes, wait optionally smallMillis to allow data arrival
    private static String readBanner(Socket s, int maxBytes, int smallMillis) {
        try {
            InputStream in = s.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int r = in.read(buf);
            if (r > 0) {
                baos.write(buf, 0, r);
                try { Thread.sleep(smallMillis); } catch (InterruptedException ignored) {}
                while (in.available() > 0 && baos.size() < maxBytes) {
                    r = in.read(buf);
                    if (r <= 0) break;
                    baos.write(buf, 0, r);
                }
                return baos.toString(StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {}
        return "";
    }

    // Lấy danh sách IPv4 local hợp lệ
    private List<String> getLocalIPv4Addresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nets)) {
                try {
                    // Lấy tên card mạng và chuyển sang chữ thường
                    String displayName = ni.getDisplayName().toLowerCase();

                    // Thêm điều kiện kiểm tra tên
                    if (!ni.isUp() || ni.isLoopback() || ni.isVirtual() ||
                        displayName.contains("vmware") || 
                        displayName.contains("virtualbox") ||
                        displayName.contains("hyper-v")) { // Thêm các trình ảo hóa khác nếu muốn
                        
                        continue; // Bỏ qua card mạng này
                    }
                } catch (SocketException se) {
                    continue;
                }
                
                // (Phần còn lại giữ nguyên)
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        result.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {}
        return result;
    }

    private String getSubnetPrefix(String ipv4) {
        String[] p = ipv4.split("\\.");
        return (p.length == 4) ? p[0] + "." + p[1] + "." + p[2] + "." : ipv4;
    }

    // Parse XAddrs -> DiscoveredCamera (như trước)
    private DiscoveredCamera parseDiscoveryResponse(String xml) {
        Matcher m = XADDRS_PATTERN.matcher(xml);
        if (!m.find()) return null;
        String xaddrs = m.group(1).trim();
        if (xaddrs.isEmpty()) return null;
        String first = xaddrs.split("\\s+")[0];
        try {
            URL u = new URL(first);
            String ip = u.getHost();
            String path = u.getPath();
            return new DiscoveredCamera(ip, (path != null && !path.isEmpty()) ? path : "/");
        } catch (Exception e) {
            System.err.println("Không parse được XAddrs: " + first);
        }
        return null;
    }
 // Lấy tất cả IPv4 (kể cả nếu interface down hoặc virtual)
    private List<String> getAllIPv4Addresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface ni = nets.nextElement();
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (!ip.startsWith("127.") && !ip.equals("0.0.0.0")) {
                            result.add(ip);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Không thể lấy IPv4: " + e.getMessage());
        }
        return result;
    }

}
