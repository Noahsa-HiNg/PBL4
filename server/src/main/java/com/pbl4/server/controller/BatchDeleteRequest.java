package com.pbl4.server.controller;

import java.util.List;

/**
 * Lớp này dùng để nhận JSON body từ client khi xóa hàng loạt.
 * Client sẽ gửi: { "photoIds": [1, 5, 10] }
 */
public class BatchDeleteRequest {

    private List<Long> photoIds;

    // Jackson (thư viện JSON) cần getter để đọc dữ liệu
    public List<Long> getPhotoIds() {
        return photoIds;
    }

    // (Tùy chọn: Thêm setter nếu cần)
    public void setPhotoIds(List<Long> photoIds) {
        this.photoIds = photoIds;
    }
}
