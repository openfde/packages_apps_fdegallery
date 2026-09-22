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
package com.fde.gallery3d.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.fde.baselib.Animation.AnimDrawablePlayer;
import com.fde.baselib.Animation.AnimFactory;
import com.fde.gallery3d.MainActivity;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.base.BaseActivity;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.utils.DeviceUtils;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.utils.StringUtils;
import com.fde.imageeditlibrary.editimage.EditImageActivity;
import com.fde.imageeditlibrary.editimage.utils.BitmapUtils;
import com.fde.imageeditlibrary.editimage.utils.Utils;
import com.fde.imageeditlibrary.editimage.view.RotateImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.xinlan.imageeditlibrary.editimage.fliter.PhotoProcessing;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PicturePreviewActivity extends BaseActivity implements View.OnClickListener ,View.OnTouchListener{
    Multimedia picture;
    View layoutBottomBtn;
    RotateImageView imageView;
    ImageView imgDetails;
    ImageView txtOcr;
    ImageView imgLeft;
    ImageView imgRight;

    ImageView txtEdit;
    ImageView txtRotate;
    ImageView imgZoomIn;
    ImageView imgZoomOut;

    EditText editOcrText;

    TextView txtScale ;

    ImageView txtDelete;
    ImageView txtShare;

    PicturePreviewPersenter picturePreviewPersenter;

    boolean isShowBottomBtn = true;

//    PopupWindow popupWindow;
//    View bottomSheetView;
    ImageView txtDetails;
    ImageView txtSetWallpage;
    TextView txtSetWallpageLock;

    private static int rotateAngle = 0;
    private static float currentScale = 1;
    private  float scaleMax = 10.0f;
    private  float scaleMin = 0.1f;
    private  float step = 1.1f;

    AnimDrawablePlayer animDrawablePlayer ;

    Bitmap rotatedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        initView();
        animDrawablePlayer = AnimFactory.INSTANCE.loading(this,imgDetails);
        startAnimation();
//        View view = getLayoutInflater().inflate(R.layout.activity_picture_preview,null);
        handlePreview();

    }

    private void handlePreview(){
        picture = (Multimedia) getIntent().getSerializableExtra("picture_data");
        List<Multimedia> tempList = FileUtils.getAllImages(context);




        if (picture == null) {
            Uri imageUri = getIntent().getData();
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, imageUri);
            String realPath = StringUtils.ToString(documentFile.getUri());
            if ("".equals(realPath)) {
                startActivity(new Intent(context, MainActivity.class));
                finish();
            } else {
                String actionStr = getIntent().getAction();
                LogTools.d("realPath   " + realPath + " ,documentFile " + documentFile +",actionStr "+actionStr);

                if ("android.intent.action.EDIT".equals(actionStr)) {
                    String filePath = FileUtils.getFilePathFromUri(context, documentFile.getUri());
                    picture = findPicture(tempList, filePath);
                } else {
                    picture = findPicture(tempList, realPath);
                }

                if(picture == null){
                    picture= new Multimedia();
                    Uri uri = Uri.parse(realPath);
                    String documentId = DocumentsContract.getDocumentId(uri).replaceAll("image:","");
                    boolean isNumeric = documentId != null && documentId.matches("\\d+");
                    LogTools.d("isNumeric：    " + isNumeric  +",documentId  "+documentId);
                    if(isNumeric){
                        picture.setId(StringUtils.ToInt(documentId));
                        if(realPath.contains("content://com.android.providers.media")){
                            String realPa = FileUtils.getRealPathFromUriT(context, imageUri);
                            if(realPa !=null){
                                realPath = realPa;
                            }
                        }
                        picture.setPath(realPath);
                    }else {
                        String relativePath = documentId.substring(documentId.indexOf(':') + 1);
                        LogTools.d("relativePath：    " + relativePath);
                        if(relativePath.contains("volumes")){
                            picture.setPath(relativePath);
                        }else {
                            picture.setPath("/storage/emulated/0/"+relativePath);
                        }
                        picture.setId(-1);
                    }

                }

                picturePreviewPersenter = new PicturePreviewPersenter(this, picture);
                if(picture == null && picturePreviewPersenter.getCurPic() !=null){
                    picture = picturePreviewPersenter.getCurPic();
                }
                initData();

                //if action is edit to go to edit page
                if ("android.intent.action.EDIT".equals(actionStr)) {
                    picturePreviewPersenter.editImageClick();
                }
            }
        } else {
            String path = SPUtils.getUserInfo(context, "curPicPath");
            Multimedia m = findPicture(tempList, path);
            picture = m;
            picturePreviewPersenter = new PicturePreviewPersenter(this, picture);
            initData();
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(rotatedBitmap !=null && picture !=null){
            if(rotateAngle % 360 == 0){
                return;
            }
            BitmapUtils.saveBitmap(context, rotatedBitmap, picture.getPath());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
            prePic();
        }else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
            nextPic();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL &&
                event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            float hScroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            if (vScroll > 0) {
                setImgZoomIn();
            } else if (vScroll < 0) {
                setImgZoomOut();
            }
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private float startX = 0f; // 按下起点
    private float startY = 0f; // 按下起点
    private boolean dragging = false;

    public void initView() {
        imageView = (RotateImageView) findViewById(R.id.imageView);
        imgDetails = (ImageView) findViewById(R.id.imgDetails);
        txtOcr = (ImageView) findViewById(R.id.txtOcr);
        imgLeft = (ImageView) findViewById(R.id.imgLeft);
        imgRight = (ImageView) findViewById(R.id.imgRight);
        txtDelete = (ImageView) findViewById(R.id.txtDelete);
        txtShare = (ImageView) findViewById(R.id.txtShare);
        txtDetails = (ImageView) findViewById(R.id.txtDetails);
        txtSetWallpage = (ImageView) findViewById(R.id.txtSetWallpage);
        txtEdit = (ImageView) findViewById(R.id.txtEdit);
        txtRotate = (ImageView) findViewById(R.id.txtRotate);
        imgZoomIn = (ImageView) findViewById(R.id.imgZoomIn);
            imgZoomOut = (ImageView) findViewById(R.id.imgZoomOut);
        txtScale = (TextView) findViewById(R.id.txtScale);
        editOcrText = (EditText) findViewById(R.id.editOcrText);
        layoutBottomBtn = (View) findViewById(R.id.layoutBottomBtn);
        txtDetails.setOnClickListener(this);
        txtSetWallpage.setOnClickListener(this);
        txtDelete.setOnClickListener(this);
        txtShare.setOnClickListener(this);
        txtEdit.setOnClickListener(this);
        txtOcr.setOnClickListener(this);
        txtRotate.setOnClickListener(this);
        imgLeft.setOnClickListener(this);
        imgRight.setOnClickListener(this);
        imgZoomOut.setOnClickListener(this);
        imgZoomIn.setOnClickListener(this);
        imageView.setOnDoubleTapListener(null);
//        imageView.setOnTouchListener(this);
    }

    private void initData(){
        imageView.setMaximumScale(scaleMax);
        imageView.setMinimumScale(scaleMin);
        showPic(picture);
        float scale = imageView.getMaximumScale();
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture.getId() > 0) {
                    editOcrText.setVisibility(View.GONE);
//                    isShowBottomBtn = !isShowBottomBtn;
//                    layoutBottomBtn.setVisibility(isShowBottomBtn ? View.VISIBLE : View.GONE);
                }
            }
        });

        if (picture.getId() <= 0) {
            isShowBottomBtn = false;
            imgLeft.setVisibility(View.GONE);
            imgRight.setVisibility(View.GONE);
            layoutBottomBtn.setVisibility(View.GONE);
        } else {
            imgLeft.setVisibility(View.VISIBLE);
            imgRight.setVisibility(View.VISIBLE);
            layoutBottomBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogTools.i("onActivityResult requestCode: " + requestCode + " ,resultCode: " + resultCode);
        if (requestCode == Constant.REQUEST_PERMISSION_DELETE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                picturePreviewPersenter.deleteImage(context);
            }
        } else {
            if (resultCode == RESULT_OK) {
                if (requestCode == Constant.ACTION_REQUEST_EDITIMAGE) {
                    picturePreviewPersenter.handleEditorImage(data);
                    finish();
                }
            }

        }
    }

    private void startAnimation(){
        imgDetails.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        animDrawablePlayer.start();
    }

    private void stopAnimation(){
        animDrawablePlayer.stop();
        imageView.setVisibility(View.VISIBLE);
        imgDetails.setVisibility(View.GONE);
    }

    public void showPic(Multimedia multimedia) {
        if (multimedia != null && !"".equals(multimedia.getPath())) {
            try {
                LogTools.d("multimedia getPath "+multimedia.getPath());
                RequestOptions options = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(StringUtils.ToInt(Utils.getSystemProperty("openfde.display_width")),StringUtils.ToInt(Utils.getSystemProperty("openfde.display_height")));

                Glide.with(context) // replace 'this' with your context
                        .load(multimedia.getPath())
                        .error(R.mipmap.ic_launcher)
                        .apply(options)
                        .fitCenter()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                stopAnimation();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                stopAnimation();
                                return false;
                            }
                        })
                        .into(imageView);

            } catch (Exception e) {
                e.printStackTrace();
           stopAnimation();
            }
        }
    }


    @Override
    public void onClick(View view) {
        editOcrText.setVisibility(View.GONE);
        editOcrText.setText("");

        int id = view.getId();

        if (id == R.id.txtDelete) {

            picturePreviewPersenter.showDelDlg();

        } else if (id == R.id.txtShare) {

            ArrayList<Uri> imageUris = new ArrayList<>();
            imageUris.add(
                    FileProvider.getUriForFile(
                            context,
                            Constant.PKG_PROVIDER,
                            new File(picture.getPath())
                    )
            );

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
            intent.setType("image/*");
            intent.putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    imageUris
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            context.getString(R.string.share)
                    )
            );

        } else if (id == R.id.txtEdit) {

            picturePreviewPersenter.editImageClick();

        } else if (id == R.id.txtRotate) {

            setRotate();

        } else if (id == R.id.imgZoomIn) {

            setImgZoomIn();

        } else if (id == R.id.imgZoomOut) {

            setImgZoomOut();

        } else if (id == R.id.imgLeft) {

            prePic();

        } else if (id == R.id.imgRight) {

            nextPic();

        } else if (id == R.id.txtOcr) {

            editOcrText.setVisibility(View.VISIBLE);

            Multimedia picture =
                    picturePreviewPersenter.getCurPic();

            Bitmap bitmap =
                    BitmapFactory.decodeFile(picture.getPath());

            InputImage image =
                    InputImage.fromBitmap(bitmap, 0);

            TextRecognizer recognizer =
                    TextRecognition.getClient(
                            new ChineseTextRecognizerOptions.Builder()
                                    .build()
                    );

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String text = result.getText();

                        if (TextUtils.isEmpty(text)) {
                            editOcrText.setText(
                                    getString(R.string.no_text)
                            );
                        } else {
                            editOcrText.setText(text);
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                    });

        } else if (id == R.id.txtDetails) {

            picturePreviewPersenter.showDetailsDlg();

        } else if (id == R.id.txtSetWallpage) {

            setSetWallpage();

        } else if (id == R.id.txtSetWallpageLock) {

            picturePreviewPersenter.setWallpage(2);
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            return false;
        }

        int button = event.getButtonState();
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                // 只处理左键按下
                if ((button & MotionEvent.BUTTON_PRIMARY) != 0) {
                    startX = x;
                    startX = y;
                    dragging = true;
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // 拖拽过程中，可以加动画或滑动效果
                if (dragging) {
                    float dx = x - startX;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (dragging) {
                    float dx = x - startX;
                    float dy = y - startY;
                    dragging = false;
                    // 设置阈值，比如 50px 才算翻页
                    float threshold = 50f;

                    if (dx > threshold) {
                        prePic();
                    } else if (dx < - threshold) {
                        nextPic();
                    }
                    return true;
                }
                break;
        }
        return  false;
    }

    private void setSetWallpage(){
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
            builder.setTitle(R.string.is_set_wallpaper);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            picturePreviewPersenter.setWallpage(1);
                        }
                    }).start();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setRotate(){
        new Thread(new Runnable() {
            @Override
            public void run() {
//                Multimedia pic = list.get(curPos);
                Multimedia picture = picturePreviewPersenter.getCurPic();
                Bitmap bitmap = BitmapFactory.decodeFile(picture.getPath());
                Matrix matrix = new Matrix();
                rotateAngle+=90;
                matrix.postRotate(rotateAngle);

                rotatedBitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        true
                );

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(rotatedBitmap);
                    }
                });

            }
        }).start();
    }

    /**
     * 上一页
     */
    public void prePic() {
        Multimedia prePic = picturePreviewPersenter.getPrePic();
        if(prePic == null){
            stopAnimation();
            showShortToast(getString(R.string.is_first_pic));
            return;
        }
        startAnimation();
        showPic(prePic);
    }

    /**
     * 下一页
     */
    public void nextPic() {
        Multimedia nextPic = picturePreviewPersenter.getNextPic();
        if(nextPic == null){
            stopAnimation();
            showShortToast(getString(R.string.no_more_pic));
            return;
        }
        startAnimation();
        showPic(nextPic);
    }

    private  void setImgZoomIn(){
        if(editOcrText.getVisibility() == View.VISIBLE){
            return;
        }
        currentScale = currentScale * step;
        if(currentScale >= scaleMax){
            currentScale = scaleMax;
        }
        imageView.setScale(currentScale);
        txtScale.setText(String.format("%.0f%%", currentScale * 100));
    }

    private  void setImgZoomOut(){
        if(editOcrText.getVisibility() == View.VISIBLE){
            return;
        }
        currentScale = currentScale / step;
        if(currentScale <= scaleMin){
            currentScale = scaleMin;
        }
        imageView.setScale(currentScale);
        txtScale.setText(String.format("%.0f%%", currentScale * 100));
    }

    public Multimedia findPicture(List<Multimedia> listM, String path) {
        int curPos = -1;
        try {
            for (int i = 0; i < listM.size(); i++) {
                if (path.equals(listM.get(i).getPath())) {
                    curPos = i;
                }
            }
            return listM.get(curPos);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }



}