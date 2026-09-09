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
package com.fde.gallery3d.ui.base;

public abstract  class BasePresenter<V> {
    public BasePresenter(V mView) {
        this.mView = mView;
    }

    /**
     * 抽象出的界面层
     */
    protected V mView;

    public void attachView(V view) {
        mView = view;
    }

    protected V getView() {
        return mView;
    }

    public boolean isViewAtteached() {
        return mView != null;
    }

    public void detachView() {
        if (null != mView) {
            mView = null;
        }
    }

}
