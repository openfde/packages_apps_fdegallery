package com.fde.gallery3d.ui.favorite;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 收藏数据的本地存储（SharedPreferences + JSON）。
 */
public class FavoriteStore {

    public static class Item {
        public String path;
        public int mediaType = Constant.MEDIA_PIC;
        public long time;
    }

    private static final String KEY_FAVORITE = "favorite_items";

    private FavoriteStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(Constant.SP_MEDIA, Context.MODE_PRIVATE);
    }

    public static synchronized List<Item> getAll(Context context) {
        List<Item> list = new ArrayList<>();
        String raw = prefs(context).getString(KEY_FAVORITE, "");
        if (TextUtils.isEmpty(raw)) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                Item item = new Item();
                item.path = object.optString("path");
                item.mediaType = object.optInt("type", Constant.MEDIA_PIC);
                item.time = object.optLong("time");
                if (!TextUtils.isEmpty(item.path)) {
                    list.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(list, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return Long.compare(o2.time, o1.time);
            }
        });
        return list;
    }

    public static synchronized Set<String> getPaths(Context context) {
        Set<String> set = new HashSet<>();
        for (Item item : getAll(context)) {
            set.add(item.path);
        }
        return set;
    }

    public static synchronized boolean isFavorite(Context context, String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        return getPaths(context).contains(path);
    }

    /**
     * @return true 表示新增成功，false 表示已经收藏过或者参数非法
     */
    public static synchronized boolean add(Context context, Multimedia media) {
        if (media == null || TextUtils.isEmpty(media.getPath())) {
            return false;
        }
        List<Item> list = getAll(context);
        for (Item item : list) {
            if (media.getPath().equals(item.path)) {
                return false;
            }
        }
        Item item = new Item();
        item.path = media.getPath();
        item.mediaType = media.getMediaType();
        item.time = System.currentTimeMillis();
        list.add(item);
        save(context, list);
        return true;
    }

    public static synchronized boolean remove(Context context, String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        List<Item> list = getAll(context);
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (path.equals(list.get(i).path)) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed) {
            save(context, list);
        }
        return removed;
    }

    /**
     * @return true 表示操作后处于已收藏状态
     */
    public static synchronized boolean toggle(Context context, Multimedia media) {
        if (media == null || TextUtils.isEmpty(media.getPath())) {
            return false;
        }
        if (isFavorite(context, media.getPath())) {
            remove(context, media.getPath());
            return false;
        }
        add(context, media);
        return true;
    }

    public static synchronized int count(Context context) {
        return getAll(context).size();
    }

    private static void save(Context context, List<Item> list) {
        JSONArray array = new JSONArray();
        for (Item item : list) {
            try {
                JSONObject object = new JSONObject();
                object.put("path", item.path);
                object.put("type", item.mediaType);
                object.put("time", item.time);
                array.put(object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        prefs(context).edit().putString(KEY_FAVORITE, array.toString()).apply();
    }
}
