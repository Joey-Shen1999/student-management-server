package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class InfoListResponseDto {
    private List<InfoTaskDto> items;
    private long total;
    private int page;
    private int size;

    public InfoListResponseDto(List<InfoTaskDto> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<InfoTaskDto> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
