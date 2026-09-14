package com.fde.gallery3d.ui.trash;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.fde.gallery3d.ui.common.Constant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 回收站数据的本地存储（SharedPreferences + JSON）。
 */
public class TrashStore {

    private static final String KEY_TRASH = "trash_items";

    private TrashStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(Constant.SP_MEDIA, Context.MODE_PRIVATE);
    }

    public static synchronized List<TrashItem> getAll(Context context) {
        List<TrashItem> list = new ArrayList<>();
        String raw = prefs(context).getString(KEY_TRASH, "");
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
                TrashItem item = new TrashItem();
                item.id = object.optLong("id");
                item.path = object.optString("path");
                item.trashPath = object.optString("trash");
                item.title = object.optString("title");
                item.mediaType = object.optInt("type", Constant.MEDIA_PIC);
                item.size = object.optLong("size");
                item.deletedTime = object.optLong("time");
                if (!TextUtils.isEmpty(item.trashPath)) {
                    list.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(list, new Comparator<TrashItem>() {
            @Override
            public int compare(TrashItem o1, TrashItem o2) {
                return Long.compare(o2.deletedTime, o1.deletedTime);
            }
        });
        return list;
    }

    public static synchronized void add(Context context, TrashItem item) {
        if (item == null) {
            return;
        }
        List<TrashItem> list = getAll(context);
        list.add(item);
        save(context, list);
    }

    public static synchronized void remove(Context context, long id) {
        List<TrashItem> list = getAll(context);
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id == id) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed) {
            save(context, list);
        }
    }

    public static synchronized void clear(Context context) {
        prefs(context).edit().putString(KEY_TRASH, "").apply();
    }

    public static synchronized int count(Context context) {
        return getAll(context).size();
    }

    private static void save(Context context, List<TrashItem> list) {
        JSONArray array = new JSONArray();
        for (TrashItem item : list) {
            try {
                JSONObject object = new JSONObject();
                object.put("id", item.id);
                object.put("path", item.path);
                object.put("trash", item.trashPath);
                object.put("title", item.title);
                object.put("type", item.mediaType);
                object.put("size", item.size);
                object.put("time", item.deletedTime);
                array.put(object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        prefs(context).edit().putString(KEY_TRASH, array.toString()).apply();
    }
}
