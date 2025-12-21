package com.pbl4.server.dto;

// Interface projection
public interface DailyImageStats {
    String getDateStr(); // Ngày tháng (dạng chuỗi dd/MM)
    Long getCountVal();  // Số lượng
}