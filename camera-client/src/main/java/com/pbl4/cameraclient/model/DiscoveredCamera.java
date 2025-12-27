package com.pbl4.cameraclient.model;

/**
 * Lớp POJO đơn giản để lưu trữ thông tin camera được phát hiện trên mạng.
 */
public class DiscoveredCamera {
    private String ipAddress;
    private String onvifServiceUrl; // Đường dẫn dịch vụ ONVIF (ví dụ: /onvif/device_service)

    public DiscoveredCamera(String ipAddress, String onvifServiceUrl) {
        this.ipAddress = ipAddress;
        this.onvifServiceUrl = onvifServiceUrl;
    }

    // Getters
    public String getIpAddress() { return ipAddress; }
    public String getOnvifServiceUrl() { return onvifServiceUrl; }

    /**
     * Hiển thị trong ListView cho thân thiện.
     */
    @Override
    public String toString() {
        return "IP: " + ipAddress + "\n(ONVIF: " + onvifServiceUrl + ")";
    }
}