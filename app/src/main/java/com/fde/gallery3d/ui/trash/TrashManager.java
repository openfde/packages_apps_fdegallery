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

        // 2. 删除媒体库记录（此时原路径文件已经不存在，MediaProvider 只会移除记录）。
        //    注意：对非应用自有的媒体（例如系统截图），ContentResolver.delete() 会抛出
        //    RecoverableSecurityException / SecurityException。这种情况下必须先把文件
        //    移回原位并向上抛出该异常，由界面层（Fragment）用系统对话框请求删除授权；
        //    用户授权后会在 onActivityResult 中重试，从而保证“移入回收站”完整生效。
        //    其它非安全异常则记录日志后继续（文件已成功移入回收站，仍可被恢复）。
        try {
            FileUtils.deleteMediaStoreEntry(context, media.getMediaType(), media.getPath());
        } catch (SecurityException e) {
            LogTools.e("moveToTrash: delete MediaStore record needs permission, restore file: " + e);
            FileUtils.moveFile(dest, src);        // 恢复原位，避免数据丢失
            FileUtils.scanFile(context, media.getPath());
            throw e;                               // 让调用方弹窗请求授权后重试
        } catch (Exception e) {
            LogTools.e("moveToTrash: delete MediaStore record failed (file kept in trash): " + e);
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
