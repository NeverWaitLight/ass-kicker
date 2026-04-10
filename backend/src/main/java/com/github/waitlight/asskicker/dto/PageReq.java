package com.github.waitlight.asskicker.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageReq {
    private int page = 1;
    private int size = 10;
    private String keyword;

    public PageReq(int page, int size, String keyword) {
        this.page = Math.max(page, 1);
        this.size = Math.max(size, 1);
        this.keyword = keyword;
    }
}
