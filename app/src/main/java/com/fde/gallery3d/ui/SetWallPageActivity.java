package com.fde.gallery3d.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.adapter.PictureListAdapter;
import com.fde.gallery3d.ui.adapter.SetWallPageAdapter;
import com.fde.gallery3d.ui.base.BaseActivity;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.event.ViewEvent;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.utils.WallpaperUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SetWallPageActivity extends BaseActivity implements ViewEvent {
    ImageView imageView;
    TextView txtPicTitle;
    TextView txtOk;
    TextView txtCancel;

    RecyclerView recyclerView;

    List<Multimedia> list;
    int numberOfColumns = 3;
    SetWallPageAdapter pictureListAdapter;
    int curPos = 0;

    private final static String WALLPKG = "com.android.wallpaper";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_wall_page);
        imageView = (ImageView) findViewById(R.id.imageView);
        txtOk = (TextView) findViewById(R.id.txtOk);
        txtCancel = (TextView) findViewById(R.id.txtCancel);
        txtPicTitle = (TextView) findViewById(R.id.txtPicTitle);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        list = new ArrayList<>();
        LinearLayoutManager gridLayoutManager = new GridLayoutManager(context,2,RecyclerView.HORIZONTAL,false);
        gridLayoutManager.setOrientation(RecyclerView.HORIZONTAL);

        recyclerView.setLayoutManager(gridLayoutManager);
        list = new ArrayList<>();
        pictureListAdapter = new SetWallPageAdapter(context, list, numberOfColumns, this);
        recyclerView.setAdapter(pictureListAdapter);
        getAllImages();
        txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        txtOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Multimedia pic = list.get(curPos);
                if(pic.getWidth() > 3999 && pic.getHeight()> 3999){
                    Toast.makeText(context,R.string.set_pic_tips,Toast.LENGTH_SHORT).show();
                }
                ComponentName callingActivity = getCallingActivity();
                if (callingActivity == null) {
                    return;
                }
                String callerPackage = callingActivity.getPackageName();
                if (WALLPKG.equals(callerPackage)) {
                    // 壁纸设置调用方：先让用户选择显示方式（平铺/居中/拉伸等 5 种）
                    showWallpaperScaleDialog(pic);
                } else {
                    // 其它调用方：不设置壁纸，直接返回图片 URI
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Uri uri = getUri(pic.getPath());
                                Intent resultIntent = new Intent();
                                resultIntent.setData(uri);
                                resultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                resultIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                setResult(Activity.RESULT_OK, resultIntent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            });
                        }
                    }).start();
                }
            }
        });

    }

    /**
     * 选择壁纸显示方式（平铺/居中/居中裁剪/等比适应/拉伸），
     * 与预览页共用 {@link WallpaperUtils} 的同一套逻辑。
     */
    private void showWallpaperScaleDialog(final Multimedia pic) {
        if (pic == null || context == null) {
            return;
        }
        try {
            final CharSequence[] items = new CharSequence[]{
                    getString(R.string.wallpaper_scale_stretch),
                    getString(R.string.wallpaper_scale_center),
                    getString(R.string.wallpaper_scale_center_crop),
                    getString(R.string.wallpaper_scale_fit),
                    getString(R.string.wallpaper_scale_tile)
            };
            // 5 个选项的索引与 WallpaperUtils 的 WALLPAPER_SCALE_* 常量一一对应
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    this, com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
            builder.setTitle(R.string.wallpaper_scale_title);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    final int scaleMode = which;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean success = WallpaperUtils.setWallpaper(
                                    context, pic.getPath(), 1, scaleMode);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context,
                                            success ? R.string.set_wallpage_success : R.string.set_wallpage_failed,
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
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


    public Uri getUri(String filePath){
        ContentResolver resolver = context.getContentResolver();
        Uri contentUri = null;
        String[] projection = {
                MediaStore.Images.Media._ID
        };
        String selection = MediaStore.Images.Media.DATA + "=?";
        try (Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                new String[]{filePath},
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                MediaStore.Images.Media._ID));

                contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id);
            }
        }
        return contentUri;
    }



    /***
     * get all picture
     * @param context
     */
    public void getAllImages() {
        if (list != null) {
            list.clear();
        }

        List<Multimedia> tempList = FileUtils.getAllImages(context);
        for(Multimedia m: tempList){
            if(m.getWidth() >= 800 && m.getHeight() >= 600){
                list.add(m);
            }
        }
        if (pictureListAdapter == null) {
            LogTools.i("pictureListAdapter is null");
        } else {
            pictureListAdapter.notifyDataSetChanged();
        }
        if(list == null || list.size() == 0){
            Toast.makeText(context,R.string.not_pic,Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Multimedia picture = list.get(0);
        setPic(picture);
    }

    public void setPic(Multimedia picture) {
        txtPicTitle.setText(picture.getTitle()+"("+picture.getWidth() +"x"+picture.getHeight()+")");
        Glide.with(context)
                .load(Uri.fromFile(new File(picture.getPath())))
                .error(R.mipmap.ic_launcher)
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    @Override
    public void onRightEvent(int pos, int groupPos) {

    }

    @Override
    public void onSelectEvent(int pos, int groupPos, boolean isSelect) {
        curPos = pos;
        Multimedia picture = list.get(pos);
        setPic(picture);
    }

    @Override
    public void onJumpEvent(Multimedia multimedia) {

    }
}