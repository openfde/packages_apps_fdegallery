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
package com.fde.imageeditlibrary.editimage.view;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;

/**
 * 禁用ViewPager滑动事件
 * 
 * @author openfde
 * 
 */
public class CustomViewPager extends ViewPager {
	private boolean isCanScroll = false;

	public CustomViewPager(Context context) {
		super(context);
	}

	@Override
	public void setCurrentItem(int item, boolean smoothScroll) {
		isCanScroll = true;
		super.setCurrentItem(item, smoothScroll);
		isCanScroll = false;
	}

	@Override
	public void setCurrentItem(int item) {
		setCurrentItem(item, false);
	}

	public CustomViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setScanScroll(boolean isCanScroll) {
		this.isCanScroll = isCanScroll;
	}

	@Override
	public void scrollTo(int x, int y) {
		if (isCanScroll) {
			super.scrollTo(x, y);
		}
	}
}// end class
