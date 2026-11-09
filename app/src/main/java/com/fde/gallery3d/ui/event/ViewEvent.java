package com.fde.gallery3d.ui.event;

import com.fde.gallery3d.ui.bean.Multimedia;

public interface ViewEvent {
    void onRightEvent(int pos ,int groupPos);

//    void onDeleteEvent(int pos ,String content);

    void onSelectEvent(int pos ,int groupPos,boolean isSelect);

    void onJumpEvent(Multimedia multimedia);

    /**
     * 带键盘修饰键的单击选中：
     * <ul>
     *   <li>普通单击：只选中当前项，取消其它已选项；</li>
     *   <li>Ctrl + 单击：追加 / 取消单个多选；</li>
     *   <li>Shift + 单击：从上次锚点到当前位置的范围选择。</li>
     * </ul>
     * 默认实现忽略修饰键，走原有 {@link #onSelectEvent(int, int, boolean)} 逻辑。
     *
     * @param ctrlPressed 是否按住 Ctrl
     * @param shiftPressed 是否按住 Shift
     */
    default void onSelectEvent(int pos, int groupPos, boolean isSelect,
                               boolean ctrlPressed, boolean shiftPressed) {
        onSelectEvent(pos, groupPos, isSelect);
    }
}
