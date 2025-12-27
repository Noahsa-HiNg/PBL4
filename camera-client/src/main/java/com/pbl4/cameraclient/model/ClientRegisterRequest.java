package com.pbl4.cameraclient.model;

public class ClientRegisterRequest {
    private String machineId;
    private String clientName;
    
    // Getters and Setters
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public ClientRegisterRequest(String machineId,String clientName) {
    	this.machineId = machineId;
    	this.clientName = clientName;
		// TODO Auto-generated constructor stub
	}
}