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

import java.util.ArrayList;
import java.util.List;

public class MultGroup {
    private String title;
    private List<Multimedia> list;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Multimedia> getList() {
        return list;
    }

    public void setList(List<Multimedia> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "MultGroup{" +
                "title='" + title + '\'' +
                ", list=" + list +
                '}';
    }
}
