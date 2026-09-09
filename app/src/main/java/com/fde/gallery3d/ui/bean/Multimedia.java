/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fde.gallery3d.ui.bean;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * multi-media bean class
 */
public class Multimedia implements Serializable {
    private long id;
    private String path;
    private String title;
    private int width;
    private int height;
    private long size;
    private long dateTaken;
    private long duration;
    private boolean isSelected;
    private boolean isShowCheckbox;
    private int mediaType;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(long dateTaken) {
        this.dateTaken = dateTaken;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isShowCheckbox() {
        return isShowCheckbox;
    }

    public void setShowCheckbox(boolean showCheckbox) {
        isShowCheckbox = showCheckbox;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    // 用于获取当前图片的格式化日期
    public String getDate() {
        return new SimpleDateFormat("yyyy年MM月").format(new Date(dateTaken * 1000L));
    }

    @Override
    public String toString() {
        return "Multimedia{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", title='" + title + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", size=" + size +
                ", dateTaken=" + dateTaken +
                ", duration=" + duration +
                ", isSelected=" + isSelected +
                ", isShowCheckbox=" + isShowCheckbox +
                ", mediaType=" + mediaType +
                ", getDate=" + getDate() +
                '}';
    }
}
