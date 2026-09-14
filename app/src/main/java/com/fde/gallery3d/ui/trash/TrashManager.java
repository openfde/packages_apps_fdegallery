package com.fde.gallery3d.ui.trash;

import android.content.Context;
import android.text.TextUtils;

import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;

import java.io.File;
import java.util.List;

/**
 * 回收站的文件操作：移入回收站、恢复、彻底删除。
 */
public class TrashManager {

    private TrashManager() {
    }

    /**
     * 将媒体文件移入回收站。
     * 注意：ContentResolver.delete() 会连同真实文件一起删除，
     * 所以必须先把文件移动到回收站，再删除媒体库记录。
     *
     * @return true 表示移动成功
     */
    public static boolean moveToTrash(Context context, Multimedia media) {
        if (context == null || media == null || TextUtils.isEmpty(media.getPath())) {
            return false;
        }
        File src = new File(media.getPath());
        if (!src.exists() || !src.isFile()) {
            LogTools.e("moveToTrash: file not found " + media.getPath());
            return false;
        }
        File trashDir = FileUtils.getTrashDir(context);
        if (trashDir == null) {
            return false;
        }

        // 1. 先移动文件到回收站目录，失败则什么都不做，保证数据安全
        File dest = new File(trashDir, System.nanoTime() + "_" + src.getName());
        if (!FileUtils.moveFile(src, dest)) {
            LogTools.e("moveToTrash: move failed " + src + " -> " + dest);
            return false;
        }

        // 2. 删除媒体库记录（此时原路径文件已经不存在，MediaProvider 只会移除记录）
        try {
            FileUtils.deleteMediaStoreEntry(context, media.getMediaType(), media.getPath());
        } catch (Exception e) {
            // 删除记录失败，回滚文件
            LogTools.e("moveToTrash: delete record failed, rollback: " + e);
            FileUtils.moveFile(dest, src);
            FileUtils.scanFile(context, media.getPath());
            return false;
        }

        // 3. 记录到回收站
        TrashItem item = new TrashItem();
        item.id = System.currentTimeMillis() * 1000 + (System.nanoTime() % 1000);
        item.path = media.getPath();
        item.trashPath = dest.getAbsolutePath();
        item.title = !TextUtils.isEmpty(media.getTitle()) ? media.getTitle() : src.getName();
        item.mediaType = media.getMediaType();
        item.size = media.getSize() > 0 ? media.getSize() : dest.length();
        item.deletedTime = System.currentTimeMillis();
        TrashStore.add(context, item);
        return true;
    }

    public static int moveToTrash(Context context, List<Multimedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Multimedia media : mediaList) {
            if (moveToTrash(context, media)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 从回收站恢复到原路径
     */
    public static boolean restore(Context context, TrashItem item) {
        if (context == null || item == null || TextUtils.isEmpty(item.trashPath)) {
            return false;
        }
        File src = new File(item.trashPath);
        if (!src.exists()) {
            TrashStore.remove(context, item.id);
            return false;
        }
        File dest = new File(item.path);
        if (!FileUtils.moveFile(src, dest)) {
            return false;
        }
        FileUtils.scanFile(context, dest.getAbsolutePath());
        TrashStore.remove(context, item.id);
        return true;
    }

    /**
     * 彻底删除回收站中的文件
     */
    public static boolean deleteForever(Context context, TrashItem item) {
        if (item == null) {
            return false;
        }
        boolean deleted = true;
        if (!TextUtils.isEmpty(item.trashPath)) {
            File file = new File(item.trashPath);
            if (file.exists()) {
                deleted = file.delete();
            }
        }
        if (deleted) {
            TrashStore.remove(context, item.id);
        }
        return deleted;
    }

    /**
     * 清空回收站
     *
     * @return 成功删除的文件数量
     */
    public static int clear(Context context) {
        int count = 0;
        for (TrashItem item : TrashStore.getAll(context)) {
            if (deleteForever(context, item)) {
                count++;
            }
        }
        return count;
    }
}
