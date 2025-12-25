package com.pbl4.server.controller;

import java.util.List;


public class BatchDeleteRequest {

    private List<Long> photoIds;

    public List<Long> getPhotoIds() {
        return photoIds;
    }

    public void setPhotoIds(List<Long> photoIds) {
        this.photoIds = photoIds;
    }
}
