package com.pbl4.cameraclient.model;
public class UpdateCameraActiveRequest {
    private boolean active;
    private String machineId;
    public UpdateCameraActiveRequest() {} // Cho Jackson
    public UpdateCameraActiveRequest(boolean active, String machineId) {
        this.active = active;
        this.machineId = machineId;
    }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getMachineId() { return machineId; }
}