package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class GoalListResponseDto {
    private List<GoalTaskDto> items;
    private long total;
    private int page;
    private int size;

    public GoalListResponseDto(List<GoalTaskDto> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<GoalTaskDto> getItems() {
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
