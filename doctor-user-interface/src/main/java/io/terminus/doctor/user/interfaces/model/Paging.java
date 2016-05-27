package io.terminus.doctor.user.interfaces.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Paging<T> implements Serializable {
    private static final long serialVersionUID = 8154701850585775441L;

    private Long total;

    private List<T> data;

    public Paging() {
    }

    public Paging(Long total, List<T> data) {
        this.data = data;
        this.total = total;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Boolean isEmpty() {
        return Objects.equals(0L, total) || data == null || data.isEmpty();
    }

    @SuppressWarnings("all")
    public static <T> Paging<T> empty(Class<T> clazz) {
        List<T> emptyList = Collections.emptyList();
        return new Paging<T>(0L, emptyList);
    }

    public static <T> Paging<T> empty() {
        return new Paging<T>(0L, Collections.<T>emptyList());
    }

}
