package com.github.waitlight.asskicker.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PageReq {
    private int page = 1;
    private int size = 10;
    private String keyword;

    public int getPage() {
        return Math.max(page, 1);
    }

    public int getSize() {
        return Math.max(size, 1);
    }
}
