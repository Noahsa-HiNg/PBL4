package com.pbl4.cameraclient.service;

import com.pbl4.cameraclient.model.DiscoveredCamera;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CameraDiscoveryService - tích hợp WS-Discovery (ONVIF) và Active LAN scan.
 */
public class temp {

    private static final String DISCOVERY_ADDRESS = "239.255.255.250";
    private static final int DISCOVERY_PORT = 3702;
    private static final int TIMEOUT_MS = 3000; // tổng thời gian chờ phản hồi WS-Discovery
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

    // Ports để thử trong Active LAN scan
    private static final int[] COMMON_CAMERA_PORTS = {80, 554, 8000, 8080, 81, 82, 88, 8554, 9000};

    // Từ khoá dùng để xác định server là camera trong HTTP banner
    private static final String[] CAMERA_KEYWORDS = {
            "onvif", "ipcam", "rtsp", "goahead", "surveillance", "hikvision", "dahua", "camera", "netwave", "vivotek"
    };

    // Số luồng cho LAN scan (tùy cấu hình máy, 100-300 hợp lý)
    private static final int SCAN_THREADS = 150;
    private static final int SOCKET_TIMEOUT_MS = 500; // timeout cho kết nối port scan

    /**
     * Tạo Task quét: chạy WS-Discovery trên tất cả interface, đồng thời chạy Active LAN scan.
     */
    public Task<List<DiscoveredCamera>> createDiscoveryTask() {
        return new Task<>() {
            @Override
            protected List<DiscoveredCamera> call() throws Exception {
                updateMessage("Bắt đầu quét WS-Discovery và LAN scan...");
                System.out.println("Bắt đầu quét WS-Discovery + LAN scan trên tất cả interface...");

                final String messageId = UUID.randomUUID().toString();
                final byte[] probeBytes = PROBE_MESSAGE_TEMPLATE.replace("{0}", messageId)
                        .getBytes(StandardCharsets.UTF_8);

                // Thread-safe set để lưu IP đã tìm thấy (tránh trùng)
                Set<String> foundIps = ConcurrentHashMap.newKeySet();
                List<DiscoveredCamera> foundCameras = Collections.synchronizedList(new ArrayList<>());

                // 1) WS-Discovery trên tất cả network interfaces (gửi probe từ từng địa chỉ nguồn)
                List<DatagramSocket> wsSockets = new ArrayList<>();
                try {
                    Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                    for (NetworkInterface netIf : Collections.list(nets)) {
                        try {
                            if (!netIf.isUp() || netIf.isLoopback() || netIf.isVirtual()) continue;
                        } catch (SocketException se) {
                            continue;
                        }

                        List<InetAddress> addrs = Collections.list(netIf.getInetAddresses())
                                .stream()
                                .filter(a -> a instanceof Inet4Address)
                                .filter(a -> !a.isLoopbackAddress())
                                .collect(Collectors.toList());

                        for (InetAddress localAddr : addrs) {
                            try {
                                DatagramSocket sock = new DatagramSocket(new InetSocketAddress(localAddr, 0));
                                sock.setSoTimeout(200);
                                wsSockets.add(sock);

                                DatagramPacket packet = new DatagramPacket(probeBytes, probeBytes.length,
                                        InetAddress.getByName(DISCOVERY_ADDRESS), DISCOVERY_PORT);
                                sock.send(packet);
                                System.out.println("Sent Probe from " + localAddr.getHostAddress() + " on iface " + netIf.getDisplayName());
                            } catch (Exception e) {
                                System.err.println("Không thể gửi WS-Discovery trên " + localAddr + " (" + e.getMessage() + ")");
                            }
                        }
                    }

                    // Lắng nghe responses trên tất cả socket trong TIMEOUT_MS tổng
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < TIMEOUT_MS) {
                        boolean gotAny = false;
                        for (DatagramSocket s : wsSockets) {
                            try {
                                byte[] buf = new byte[8192];
                                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                                s.receive(recv);
                                gotAny = true;

                                String responseXml = new String(recv.getData(), 0, recv.getLength(), StandardCharsets.UTF_8);
                                DiscoveredCamera cam = parseDiscoveryResponse(responseXml);
                                if (cam != null && foundIps.add(cam.getIpAddress())) {
                                    foundCameras.add(cam);
                                    updateMessage("Tìm thấy (ONVIF): " + cam.getIpAddress());
                                    System.out.println("WS-Discovery found: " + cam.getIpAddress() + " " + cam.getOnvifServiceUrl());
                                }
                            } catch (SocketTimeoutException ste) {
                                // không có gói trên socket này
                            } catch (Exception ex) {
                                System.err.println("Lỗi khi nhận WS-Discovery: " + ex.getMessage());
                            }
                        }
                        if (!gotAny) {
                            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                        }
                    }
                } finally {
                    for (DatagramSocket s : wsSockets) {
                        try { s.close(); } catch (Exception ignored) {}
                    }
                }

                // 2) Active LAN scan cho các interface (quét dải /24 cho mỗi IPv4 hiện có)
                updateMessage("Đang quét dải LAN (active scan)...");
                List<String> localIPv4s = getLocalIPv4Addresses();
                ExecutorService scanPool = Executors.newFixedThreadPool(SCAN_THREADS);
                List<Future<?>> futures = new ArrayList<>();

                for (String localIp : localIPv4s) {
                    if (isCancelled()) break;
                    String subnet = getSubnetPrefix(localIp);
                    // quét 1..254 (bỏ .0 và .255)
                    for (int i = 1; i <= 254; i++) {
                        final String target = subnet + i;
                        // Skip if it's own IP
                        if (target.equals(localIp)) continue;
                        // If already found by WS-Discovery, skip quickly
                        if (foundIps.contains(target)) continue;

                        futures.add(scanPool.submit(() -> {
                            if (isCancelled()) return;
                            // For each port try to detect camera
                            for (int port : COMMON_CAMERA_PORTS) {
                                if (isCancelled()) return;
                                try (Socket sock = new Socket()) {
                                    sock.connect(new InetSocketAddress(target, port), SOCKET_TIMEOUT_MS);
                                    sock.setSoTimeout(SOCKET_TIMEOUT_MS);

                                    // Try HTTP GET to read banner
                                    try {
                                        PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                                        out.print("GET / HTTP/1.0\r\n\r\n");
                                        out.flush();

                                        BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                                        StringBuilder sb = new StringBuilder();
                                        String line;
                                        int lines = 0;
                                        while ((line = in.readLine()) != null && lines++ < 20) {
                                            sb.append(line.toLowerCase()).append('\n');
                                        }
                                        String banner = sb.toString();
                                        for (String kw : CAMERA_KEYWORDS) {
                                            if (banner.contains(kw)) {
                                                // Nếu IP mới -> thêm
                                                if (foundIps.add(target)) {
                                                    // path unknown -> sử dụng "/" tạm
                                                    foundCameras.add(new DiscoveredCamera(target, "/"));
                                                    updateMessage("Tìm thấy (scan): " + target + ":" + port);
                                                    System.out.println("Active-scan found: " + target + ":" + port + " (kw=" + kw + ")");
                                                }
                                                return;
                                            }
                                        }
                                    } 
                                    catch (SocketTimeoutException ste) {
                                        // không đọc được banner -> vẫn có thể là RTSP service
                                    } 
                                    catch (Exception e) {
                                        // some devices close connection immediately; ignore
                                    }

                                    // Try RTSP OPTIONS if port is 554 or banner empty
                                    if (port == 554) {
                                        try {
                                            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                                            // A simple RTSP OPTIONS request (not fully compliant, but many servers reply)
                                            out.print("OPTIONS rtsp://" + target + ":" + port + "/ RTSP/1.0\r\nCSeq: 1\r\n\r\n");
                                            out.flush();

                                            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                                            String resp = in.readLine();
                                            if (resp != null && resp.toUpperCase().contains("RTSP")) {
                                                if (foundIps.add(target)) {
                                                    foundCameras.add(new DiscoveredCamera(target, "/"));
                                                    updateMessage("Tìm thấy (rtsp): " + target);
                                                    System.out.println("RTSP found: " + target + ":" + port);
                                                }
                                                return;
                                            }
                                        } catch (Exception ignored) {}
                                    }

                                } catch (Exception connEx) {
                                    // kết nối thất bại -> không phải service trên port này
                                }
                            } // end ports loop
                        }));
                    } // end host loop
                } // end localIp loop

                // chờ hoàn thành (có timeout tổng để tránh treo)
                scanPool.shutdown();
                try {
                    boolean finished = scanPool.awaitTermination(60, TimeUnit.SECONDS);
                    if (!finished) {
                        scanPool.shutdownNow();
                    }
                } catch (InterruptedException ie) {
                    scanPool.shutdownNow();
                }

                // Rút gọn lại danh sách: đảm bảo mỗi IP chỉ 1 DiscoveredCamera (nếu có nhiều)
                Map<String, DiscoveredCamera> unique = new LinkedHashMap<>();
                for (DiscoveredCamera dc : foundCameras) {
                    unique.putIfAbsent(dc.getIpAddress(), dc);
                }
                List<DiscoveredCamera> result = new ArrayList<>(unique.values());

                updateMessage("Tìm thấy " + result.size() + " camera.");
                System.out.println("Quét hoàn tất, tìm thấy " + result.size() + " camera.");
                return result;
            }
        };
    }

    // Lấy danh sách IPv4 local (loại loopback và ảo)
    private List<String> getLocalIPv4Addresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nets)) {
                try {
                    if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                } catch (SocketException se) {
                    continue;
                }
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        result.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getSubnetPrefix(String ipv4) {
        String[] p = ipv4.split("\\.");
        if (p.length != 4) return ipv4;
        return p[0] + "." + p[1] + "." + p[2] + ".";
    }

    /**
     * Phân tích XML phản hồi WS-Discovery để lấy XAddrs -> chuyển thành DiscoveredCamera
     */
    private DiscoveredCamera parseDiscoveryResponse(String xml) {
        Matcher xaddrsMatcher = XADDRS_PATTERN.matcher(xml);
        if (!xaddrsMatcher.find()) return null;

        String xaddrs = xaddrsMatcher.group(1).trim();
        if (xaddrs.isEmpty()) return null;

        String firstUrl = xaddrs.split("\\s+")[0];

        try {
            URL url = new URL(firstUrl);
            String ip = url.getHost();
            String path = url.getPath();
            if (ip != null && !ip.isEmpty() && path != null) {
                return new DiscoveredCamera(ip, path);
            } else if (ip != null && !ip.isEmpty()) {
                return new DiscoveredCamera(ip, "/");
            }
        } catch (Exception e) {
            System.err.println("Không thể phân tích URL từ XAddrs: " + firstUrl + " (" + e.getMessage() + ")");
        }
        return null;
    }
}

