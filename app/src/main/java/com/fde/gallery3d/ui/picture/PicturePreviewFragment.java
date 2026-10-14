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
package com.fde.gallery3d.ui.picture;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.fde.baselib.Animation.AnimDrawablePlayer;
import com.fde.baselib.Animation.AnimFactory;
import com.fde.gallery3d.R;
import com.fde.gallery3d.ui.PicturePreviewActivity;
import com.fde.gallery3d.ui.PicturePreviewPersenter;
import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.gallery3d.ui.favorite.FavoriteStore;
import com.fde.gallery3d.ui.utils.FileUtils;
import com.fde.gallery3d.ui.utils.LogTools;
import com.fde.gallery3d.ui.utils.SPUtils;
import com.fde.gallery3d.ui.utils.StringUtils;
import com.fde.imageeditlibrary.editimage.EditImageActivity;
import com.fde.imageeditlibrary.editimage.utils.BitmapUtils;
import com.fde.imageeditlibrary.editimage.utils.Utils;
import com.fde.imageeditlibrary.editimage.view.RotateImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片预览页面（Fragment 版，替代原来的 PicturePreviewActivity）。
 * 逻辑与原 Activity 保持一致：缩放、旋转、编辑、OCR、详情、设为壁纸、分享、删除、上一张/下一张。
 */
public class PicturePreviewFragment extends Fragment implements View.OnClickListener {

    /** 双击图片列表时传入的图片 */
    public static final String ARG_PICTURE = "picture_data";

    /** 以覆盖层方式显示时使用的 Fragment tag */
    public static final String TAG = "picture_preview_overlay";

    /** 预览页修改了图片（旋转 / 编辑 / 删除）时通知列表刷新 */
    public static final String RESULT_PREVIEW_CHANGED = "picture_preview_changed";
    public static final String KEY_CHANGED_PATH = "changed_path";

    private static final String ACTION_EDIT = "android.intent.action.EDIT";

    private Context context;

    private Multimedia picture;
    private View layoutBottomBtn;
    private RotateImageView imageView;
    private ImageView imgDetails;
    private ImageView txtOcr;
    private ImageView imgLeft;
    private ImageView imgRight;
    private ImageView imgBack;

    private ImageView txtEdit;
    private ImageView txtRotate;
    private ImageView imgZoomIn;
    private ImageView imgZoomOut;

    private EditText editOcrText;
    private TextView txtScale;

    private ImageView txtDelete;
    private ImageView txtShare;
    private ImageView txtFavorite;
    private ImageView txtDetails;
    private ImageView txtSetWallpage;

    private PicturePreviewPersenter picturePreviewPersenter;

    private boolean isShowBottomBtn = true;

    private int rotateAngle = 0;
    private float currentScale = 1;
    private final float scaleMax = 10.0f;
    private final float scaleMin = 0.1f;
    private final float step = 1.1f;

    private AnimDrawablePlayer animDrawablePlayer;
    private Bitmap rotatedBitmap;

    private NavController.OnDestinationChangedListener destinationListener;

    public PicturePreviewFragment() {
    }

    /**
     * 图片列表中双击图片时使用
     */
    public static PicturePreviewFragment newInstance(Multimedia picture) {
        PicturePreviewFragment fragment = new PicturePreviewFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PICTURE, picture);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 以“覆盖层”的方式打开预览：把预览 Fragment 直接 add 到内容容器上，
     * 而不是替换列表 Fragment。这样退出预览时列表的 View、滚动位置、
     * 已经加载好的缩略图都不会被销毁重建，只会刷新真正变化的图片。
     */
    public static void open(Fragment source, Multimedia picture) {
        if (source == null || picture == null) {
            return;
        }
        FragmentActivity activity = source.getActivity();
        if (activity == null) {
            return;
        }
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.isStateSaved()
                || fragmentManager.findFragmentByTag(TAG) != null) {
            return;
        }
        fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.preview_host, newInstance(picture), TAG)
                .addToBackStack(TAG)
                .commit();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_picture_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();
        initView(view);
        setupInputEvents(view);
        animDrawablePlayer = AnimFactory.INSTANCE.loading(context, imgDetails);
        startAnimation();
        loadPicture();
        registerBackPressedCallback();
        observeDestinationChange();
    }

    @Override
    public void onDestroyView() {
        // 与原 Activity 的 onDestroy 保持一致：旋转过就保存
        if (rotatedBitmap != null && picture != null) {
            if (rotateAngle % 360 != 0) {
                BitmapUtils.saveBitmap(context, rotatedBitmap, picture.getPath());
                notifyPreviewChanged(picture.getPath());
            }
        }
        removeDestinationListener();
        if (animDrawablePlayer != null) {
            animDrawablePlayer.stop();
            animDrawablePlayer = null;
        }
        super.onDestroyView();
    }

    /**
     * 系统返回键：关闭预览回到列表，避免命中底层页面的返回逻辑
     */
    private void registerBackPressedCallback() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        closePreview(Activity.RESULT_CANCELED);
                    }
                });
    }

    /**
     * 预览覆盖在列表上方时左侧导航依然可见，用户切到其它页面时自动关闭预览，
     * 避免覆盖层一直停留在最上面。
     */
    private void observeDestinationChange() {
        final NavController navController = findNavController();
        if (navController == null) {
            return;
        }
        NavDestination current = navController.getCurrentDestination();
        final int openedDestination = current == null ? 0 : current.getId();
        destinationListener = new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller,
                                             @NonNull NavDestination destination,
                                             @Nullable Bundle arguments) {
                if (destination.getId() != openedDestination) {
                    closePreview(Activity.RESULT_CANCELED);
                }
            }
        };
        navController.addOnDestinationChangedListener(destinationListener);
    }

    private void removeDestinationListener() {
        if (destinationListener == null) {
            return;
        }
        NavController navController = findNavController();
        if (navController != null) {
            navController.removeOnDestinationChangedListener(destinationListener);
        }
        destinationListener = null;
    }

    /**
     * 预览是 add 到内容容器上的，不在 NavHostFragment 内部，
     * 所以不能直接用 viewId 找 NavController（该 id 同时属于外层容器和预览自身）。
     */
    @Nullable
    private NavController findNavController() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return null;
        }
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
            if (fragment instanceof NavHostFragment) {
                return ((NavHostFragment) fragment).getNavController();
            }
        }
        return null;
    }

    /**
     * 数据来源：双击图片列表（参数）或者外部 Intent（VIEW / EDIT）
     */
    private void loadPicture() {
        Multimedia fromArgs = null;
        if (getArguments() != null) {
            fromArgs = (Multimedia) getArguments().getSerializable(ARG_PICTURE);
        }
        List<Multimedia> tempList = FileUtils.getAllImages(context);

        if (fromArgs != null) {
            String path = SPUtils.getUserInfo(context, "curPicPath");
            Multimedia media = findPicture(tempList, path);
            picture = media != null ? media : fromArgs;
            picturePreviewPersenter = new PicturePreviewPersenter(this, picture);
            initData();
            return;
        }
        resolveFromIntent(tempList);
    }

    /**
     * 宿主 Activity 收到新的 Intent 时刷新
     */
    public void reloadFromIntent() {
        if (picturePreviewPersenter == null) {
            return;
        }
        resolveFromIntent(FileUtils.getAllImages(context));
    }

    private void resolveFromIntent(List<Multimedia> tempList) {
        Intent intent = getActivity() == null ? null : getActivity().getIntent();
        if (intent == null) {
            closePreview(Activity.RESULT_CANCELED);
            return;
        }
        Uri imageUri = intent.getData();
        if (imageUri == null) {
            closePreview(Activity.RESULT_CANCELED);
            return;
        }
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, imageUri);
        String realPath = documentFile == null ? "" : StringUtils.ToString(documentFile.getUri());
        if ("".equals(realPath)) {
            closePreview(Activity.RESULT_CANCELED);
            return;
        }

        String actionStr = intent.getAction();
        LogTools.d("realPath   " + realPath + " ,documentFile " + documentFile + ",actionStr " + actionStr);

        if (ACTION_EDIT.equals(actionStr)) {
            String filePath = FileUtils.getFilePathFromUri(context, documentFile.getUri());
            picture = findPicture(tempList, filePath);
        } else {
            picture = findPicture(tempList, realPath);
        }

        if (picture == null) {
            picture = new Multimedia();
            Uri uri = Uri.parse(realPath);
            String documentId = DocumentsContract.getDocumentId(uri).replaceAll("image:", "");
            boolean isNumeric = documentId != null && documentId.matches("\\d+");
            LogTools.d("isNumeric：    " + isNumeric + ",documentId  " + documentId);
            if (isNumeric) {
                picture.setId(StringUtils.ToInt(documentId));
                if (realPath.contains("content://com.android.providers.media")) {
                    String realPa = FileUtils.getRealPathFromUriT(context, imageUri);
                    if (realPa != null) {
                        realPath = realPa;
                    }
                }
                picture.setPath(realPath);
            } else {
                String relativePath = documentId.substring(documentId.indexOf(':') + 1);
                LogTools.d("relativePath：    " + relativePath);
                if (relativePath.contains("volumes")) {
                    picture.setPath(relativePath);
                } else {
                    picture.setPath("/storage/emulated/0/" + relativePath);
                }
                picture.setId(-1);
            }
        }

        picturePreviewPersenter = new PicturePreviewPersenter(this, picture);
        if (picture == null && picturePreviewPersenter.getCurPic() != null) {
            picture = picturePreviewPersenter.getCurPic();
        }
        initData();

        // 外部 EDIT 请求：直接进入编辑页
        if (ACTION_EDIT.equals(actionStr)) {
            picturePreviewPersenter.editImageClick();
        }
    }

    /**
     * 关闭预览页：内嵌在导航里就出栈，独立 Activity 宿主就 finish
     */
    public void closePreview(int resultCode) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (activity instanceof PicturePreviewActivity) {
            activity.setResult(resultCode);
            activity.finish();
            return;
        }
        // 覆盖层模式：只弹出预览自身的回退栈，列表 Fragment 不会被重建
        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    /**
     * 通知列表：某张图片的内容发生了变化（旋转 / 编辑 / 删除）
     */
    public void notifyPreviewChanged(String path) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        Bundle bundle = new Bundle();
        if (path != null) {
            bundle.putString(KEY_CHANGED_PATH, path);
        }
        activity.getSupportFragmentManager().setFragmentResult(RESULT_PREVIEW_CHANGED, bundle);
    }

    /**
     * 供 Presenter 调用的主线程工具
     */
    public void runOnUi(Runnable runnable) {
        Activity activity = getActivity();
        if (activity != null && runnable != null) {
            activity.runOnUiThread(runnable);
        }
    }

    /**
     * 供 Presenter 调用的编辑入口，结果回到本 Fragment
     */
    public void startEditImage(String editImagePath, String outputPath, int requestCode) {
        if (TextUtils.isEmpty(editImagePath)) {
            showShortToast(getString(R.string.can_not_choose_empty));
            return;
        }
        Intent intent = new Intent(requireContext(), EditImageActivity.class);
        intent.putExtra(EditImageActivity.FILE_PATH, editImagePath);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EditImageActivity.EXTRA_OUTPUT, outputPath);
        startActivityForResult(intent, requestCode);
    }

    private void initView(View root) {
        imageView = root.findViewById(R.id.imageView);
        imgDetails = root.findViewById(R.id.imgDetails);
        txtOcr = root.findViewById(R.id.txtOcr);
        imgLeft = root.findViewById(R.id.imgLeft);
        imgRight = root.findViewById(R.id.imgRight);
        imgBack = root.findViewById(R.id.imgBack);
        txtDelete = root.findViewById(R.id.txtDelete);
        txtShare = root.findViewById(R.id.txtShare);
        txtFavorite = root.findViewById(R.id.txtFavorite);
        txtDetails = root.findViewById(R.id.txtDetails);
        txtSetWallpage = root.findViewById(R.id.txtSetWallpage);
        txtEdit = root.findViewById(R.id.txtEdit);
        txtRotate = root.findViewById(R.id.txtRotate);
        imgZoomIn = root.findViewById(R.id.imgZoomIn);
        imgZoomOut = root.findViewById(R.id.imgZoomOut);
        txtScale = root.findViewById(R.id.txtScale);
        editOcrText = root.findViewById(R.id.editOcrText);
        layoutBottomBtn = root.findViewById(R.id.layoutBottomBtn);

        txtDetails.setOnClickListener(this);
        txtSetWallpage.setOnClickListener(this);
        txtDelete.setOnClickListener(this);
        txtShare.setOnClickListener(this);
        if (txtFavorite != null) {
            txtFavorite.setOnClickListener(this);
        }
        txtEdit.setOnClickListener(this);
        txtOcr.setOnClickListener(this);
        txtRotate.setOnClickListener(this);
        imgLeft.setOnClickListener(this);
        imgRight.setOnClickListener(this);
        imgZoomOut.setOnClickListener(this);
        imgZoomIn.setOnClickListener(this);
        if (imgBack != null) {
            // 显隐交给布局（fragment_picture_preview.xml 里的 imgBack），这里只挂点击
            imgBack.setOnClickListener(this);
        }
        imageView.setOnDoubleTapListener(null);
    }

    /**
     * 原来 Activity 里的 onKeyDown / onGenericMotionEvent（方向键翻页、滚轮缩放）
     * 在 Fragment 里改为挂在根布局的监听器上。
     */
    private void setupInputEvents(View root) {
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        prePic();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        nextPic();
                        return true;
                    }
                }
                return false;
            }
        });
        root.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_SCROLL
                        && event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                    float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                    if (vScroll > 0) {
                        setImgZoomIn();
                    } else if (vScroll < 0) {
                        setImgZoomOut();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void initData() {
        if (picture == null) {
            closePreview(Activity.RESULT_CANCELED);
            return;
        }
        imageView.setMaximumScale(scaleMax);
        imageView.setMinimumScale(scaleMin);
        showPic(picture);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture != null && picture.getId() > 0) {
                    editOcrText.setVisibility(View.GONE);
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogTools.i("onActivityResult requestCode: " + requestCode + " ,resultCode: " + resultCode);
        if (requestCode == Constant.REQUEST_PERMISSION_DELETE_PHOTO) {
            if (resultCode == Activity.RESULT_OK && picturePreviewPersenter != null) {
                picturePreviewPersenter.deleteImage(context);
            }
        } else {
            if (resultCode == Activity.RESULT_OK
                    && requestCode == Constant.ACTION_REQUEST_EDITIMAGE
                    && picturePreviewPersenter != null) {
                picturePreviewPersenter.handleEditorImage(data);
                String changedPath = data == null
                        ? null : data.getStringExtra(EditImageActivity.EXTRA_OUTPUT);
                if (changedPath == null && picture != null) {
                    changedPath = picture.getPath();
                }
                notifyPreviewChanged(changedPath);
                closePreview(Activity.RESULT_OK);
            }
        }
    }

    private void startAnimation() {
        if (imgDetails == null || imageView == null || animDrawablePlayer == null) {
            return;
        }
        imgDetails.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        animDrawablePlayer.start();
    }

    private void stopAnimation() {
        if (animDrawablePlayer != null) {
            animDrawablePlayer.stop();
        }
        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE);
        }
        if (imgDetails != null) {
            imgDetails.setVisibility(View.GONE);
        }
    }

    public void showPic(Multimedia multimedia) {
        if (multimedia == null || imageView == null) {
            return;
        }
        picture = multimedia;
        updateFavoriteIcon();
        if (!"".equals(multimedia.getPath())) {
            try {
                LogTools.d("multimedia getPath " + multimedia.getPath());
                RequestOptions options = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(StringUtils.ToInt(Utils.getSystemProperty("openfde.display_width")),
                                StringUtils.ToInt(Utils.getSystemProperty("openfde.display_height")));

                Glide.with(context)
                        .load(multimedia.getPath())
                        .error(R.mipmap.ic_launcher)
                        .apply(options)
                        .fitCenter()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                stopAnimation();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource,
                                                           boolean isFirstResource) {
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

    private void updateFavoriteIcon() {
        if (txtFavorite == null) {
            return;
        }
        boolean favorite = picture != null && FavoriteStore.isFavorite(context, picture.getPath());
        txtFavorite.setImageResource(
                favorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        if (favorite) {
            txtFavorite.setColorFilter(ContextCompat.getColor(context, R.color.badge_gold));
        } else {
            txtFavorite.clearColorFilter();
        }
    }

    @Override
    public void onClick(View view) {
        editOcrText.setVisibility(View.GONE);
        editOcrText.setText("");

        int id = view.getId();

        if (id == R.id.imgBack) {

            closePreview(Activity.RESULT_CANCELED);

        } else if (id == R.id.txtFavorite) {

            if (picture != null) {
                boolean favorite = FavoriteStore.toggle(context, picture);
                updateFavoriteIcon();
                Toast.makeText(
                        context,
                        favorite ? R.string.favorite_added : R.string.favorite_removed,
                        Toast.LENGTH_SHORT).show();
            }
            return;

        } else if (id == R.id.txtDelete) {

            picturePreviewPersenter.showDelDlg();

        } else if (id == R.id.txtShare) {

            ArrayList<Uri> imageUris = new ArrayList<>();
            imageUris.add(
                    FileProvider.getUriForFile(
                            context,
                            Constant.getProviderAuthority(context),
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

            Multimedia curPicture = picturePreviewPersenter.getCurPic();
            if (curPicture != null) {
                BitmapFactory.decodeFile(curPicture.getPath());
            }

        } else if (id == R.id.txtDetails) {

            picturePreviewPersenter.showDetailsDlg();

        } else if (id == R.id.txtSetWallpage) {

            setSetWallpage();

        }
    }

    private void setSetWallpage() {
        if (context == null || picturePreviewPersenter == null) {
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
            // 5 个选项的索引与 PicturePreviewPersenter 的 WALLPAPER_SCALE_* 常量一一对应
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    context, com.fde.imageeditlibrary.R.style.RoundedAlertDialog);
            builder.setTitle(R.string.wallpaper_scale_title);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    final int scaleMode = which;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            picturePreviewPersenter.setWallpage(1, scaleMode);
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

    private void setRotate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Multimedia curPicture = picturePreviewPersenter.getCurPic();
                if (curPicture == null) {
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeFile(curPicture.getPath());
                if (bitmap == null) {
                    return;
                }
                Matrix matrix = new Matrix();
                rotateAngle += 90;
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

                runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        if (imageView != null) {
                            imageView.setImageBitmap(rotatedBitmap);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 上一张
     */
    public void prePic() {
        if (picturePreviewPersenter == null) {
            return;
        }
        Multimedia prePic = picturePreviewPersenter.getPrePic();
        if (prePic == null) {
            stopAnimation();
            showShortToast(getString(R.string.is_first_pic));
            return;
        }
        startAnimation();
        showPic(prePic);
    }

    /**
     * 下一张
     */
    public void nextPic() {
        if (picturePreviewPersenter == null) {
            return;
        }
        Multimedia nextPic = picturePreviewPersenter.getNextPic();
        if (nextPic == null) {
            stopAnimation();
            showShortToast(getString(R.string.no_more_pic));
            return;
        }
        startAnimation();
        showPic(nextPic);
    }

    private void setImgZoomIn() {
        if (editOcrText.getVisibility() == View.VISIBLE) {
            return;
        }
        currentScale = currentScale * step;
        if (currentScale >= scaleMax) {
            currentScale = scaleMax;
        }
        imageView.setScale(currentScale);
        txtScale.setText(String.format("%.0f%%", currentScale * 100));
    }

    private void setImgZoomOut() {
        if (editOcrText.getVisibility() == View.VISIBLE) {
            return;
        }
        currentScale = currentScale / step;
        if (currentScale <= scaleMin) {
            currentScale = scaleMin;
        }
        imageView.setScale(currentScale);
        txtScale.setText(String.format("%.0f%%", currentScale * 100));
    }

    public Multimedia findPicture(List<Multimedia> listM, String path) {
        int curPos = -1;
        try {
            if (listM == null || path == null) {
                return null;
            }
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

    private void showShortToast(String text) {
        if (!TextUtils.isEmpty(text) && getActivity() != null) {
            Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
        }
    }
}
