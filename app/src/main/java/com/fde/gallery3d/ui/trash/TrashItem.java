package com.fde.gallery3d.ui.trash;

import com.fde.gallery3d.ui.common.Constant;

/**
 * 回收站中的一个条目。
 */
public class TrashItem {
    public long id;
    /** 文件原始路径，用于恢复 */
    public String path;
    /** 回收站内的真实路径 */
    public String trashPath;
    public String title;
    public int mediaType = Constant.MEDIA_PIC;
    public long size;
    public long deletedTime;

    public boolean isVideo() {
        return mediaType == Constant.MEDIA_VIDEO;
    }
}
