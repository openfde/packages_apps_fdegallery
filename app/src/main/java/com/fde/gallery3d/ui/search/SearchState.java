package com.fde.gallery3d.ui.search;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 左侧搜索框与右侧搜索页面之间的通信桥梁。
 */
public final class SearchState {

    public interface Listener {
        void onSearchQueryChanged(String query);
    }

    private static final SearchState INSTANCE = new SearchState();

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private String query = "";

    private SearchState() {
    }

    public static SearchState getInstance() {
        return INSTANCE;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        for (Listener listener : listeners) {
            listener.onSearchQueryChanged(this.query);
        }
    }

    public void register(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(Listener listener) {
        listeners.remove(listener);
    }
}
