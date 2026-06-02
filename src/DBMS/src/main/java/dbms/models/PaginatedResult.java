package dbms.models;

import java.util.List;

public class PaginatedResult<T> {
    public List<T> data;
    public int totalCount;
    public int pageSize;
    public int currentPage;

    public PaginatedResult() {}

    public PaginatedResult(List<T> data, int totalCount, int pageSize, int offset) {
        this.data = data;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.currentPage = (offset / pageSize) + 1;
    }
}
