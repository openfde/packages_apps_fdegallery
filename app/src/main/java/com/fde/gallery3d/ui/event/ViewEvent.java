package com.fde.gallery3d.ui.event;

import com.fde.gallery3d.ui.bean.Multimedia;

public interface ViewEvent {
    void onRightEvent(int pos ,int groupPos);

//    void onDeleteEvent(int pos ,String content);

    void onSelectEvent(int pos ,int groupPos,boolean isSelect);

    void onJumpEvent(Multimedia multimedia);
}
