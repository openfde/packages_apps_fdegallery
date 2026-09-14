package com.fde.gallery3d.ui.common;

import android.content.Context;
import android.text.TextUtils;

import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 媒体数据仓库：统一读取图片 / 视频，供搜索、收藏等页面使用。
 */
public class MediaRepository {

    private MediaRepository() {
    }

    /**
     * 读取设备上全部图片与视频，按时间倒序
     */
    public static List<Multimedia> loadAll(Context context) {
        List<Multimedia> list = new ArrayList<>();
        if (context == null) {
            return list;
        }
        try {
            list.addAll(FileUtils.getAllImages(context));
        } catch (Exception e) {
            LogTools.e("loadAll images failed: " + e);
        }
        try {
            list.addAll(FileUtils.getAllVideos(context));
        } catch (Exception e) {
            LogTools.e("loadAll videos failed: " + e);
        }
        Collections.sort(list, new Comparator<Multimedia>() {
            @Override
            public int compare(Multimedia o1, Multimedia o2) {
                return Long.compare(o2.getDateTaken(), o1.getDateTaken());
            }
        });
        return list;
    }

    /**
     * 关键字搜索：匹配标题或者完整路径（忽略大小写）
     */
    public static List<Multimedia> search(Context context, String query) {
        List<Multimedia> result = new ArrayList<>();
        if (TextUtils.isEmpty(query)) {
            return result;
        }
        return filter(loadAll(context), query);
    }

    /**
     * 在已有列表中过滤，避免每次都重新扫描媒体库
     */
    public static List<Multimedia> filter(List<Multimedia> source, String query) {
        List<Multimedia> result = new ArrayList<>();
        if (source == null || source.isEmpty() || TextUtils.isEmpty(query)) {
            return result;
        }
        String keyword = query.trim().toLowerCase(Locale.getDefault());
        for (Multimedia media : source) {
            if (matches(media, keyword)) {
                result.add(media);
            }
        }
        return result;
    }

    public static boolean matches(Multimedia media, String lowerCaseKeyword) {
        if (media == null || TextUtils.isEmpty(lowerCaseKeyword)) {
            return false;
        }
        String title = media.getTitle() == null ? "" : media.getTitle().toLowerCase(Locale.getDefault());
        if (title.contains(lowerCaseKeyword)) {
            return true;
        }
        String path = media.getPath() == null ? "" : media.getPath().toLowerCase(Locale.getDefault());
        return path.contains(lowerCaseKeyword);
    }

    /**
     * 按收藏顺序读取收藏的媒体，已经被删除的条目会被忽略
     */
    public static List<Multimedia> loadFavorites(Context context) {
        List<Multimedia> result = new ArrayList<>();
        if (context == null) {
            return result;
        }
        List<FavoriteStore.Item> favorites = FavoriteStore.getAll(context);
        if (favorites.isEmpty()) {
            return result;
        }
        Map<String, Multimedia> map = new HashMap<>();
        for (Multimedia media : loadAll(context)) {
            map.put(media.getPath(), media);
        }
        for (FavoriteStore.Item item : favorites) {
            Multimedia media = map.get(item.path);
            if (media != null) {
                result.add(media);
            }
        }
        return result;
    }
}
