package com.pbl4.server.dto;

public class UpdateCameraActiveRequest {
    private boolean active;
    private String machineId;

    // Getter and Setter
    public String getMachineId() { return machineId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}