package com.pbl4.server.dto;

public class ClientRegisterRequest {
    private String machineId; // ID duy nhất của máy (MAC Address)
    private String clientName; // Tên gợi nhớ cho client

    // Getters và Setters (Bắt buộc để Jackson hoạt động)
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
}
