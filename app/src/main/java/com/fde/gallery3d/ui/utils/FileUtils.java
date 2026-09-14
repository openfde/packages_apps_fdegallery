package com.fde.gallery3d.ui.utils;

import android.annotation.SuppressLint;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import wseemann.media.FFmpegMediaMetadataRetriever;


import com.fde.gallery3d.ui.bean.Multimedia;
import com.fde.gallery3d.ui.common.Constant;
import com.fde.imageeditlibrary.editimage.utils.BitmapUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    public static final String FOLDER_NAME = "fde";

    /**
     * file delete
     */
    public static boolean delete(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            boolean isSuccess = file.delete();
            return isSuccess;
        }
        return false;
    }

    /**
     * delete pic
     *
     * @param context
     * @param imagePath
     * @throws RecoverableSecurityException
     */
    @SuppressLint("NewApi")
    public static void deleteImage(Context context, String imagePath) throws RecoverableSecurityException {
        // Use the MediaStore to find the ID of the image
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Images.Media.DATA + "=?";
        String[] selectionArgs = {imagePath};
        Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                if (id != -1) {
                    Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    LogTools.i("deleteImage " + imagePath + " , " + deleteUri.getPath());
                    context.getContentResolver().delete(deleteUri, null, null);
                }
            }
            cursor.close();
        }
    }


    /**
     * delete video
     *
     * @param context
     * @param imagePath
     * @throws RecoverableSecurityException
     */
    @SuppressLint("NewApi")
    public static void deleteVideo(Context context, String imagePath) throws RecoverableSecurityException {
        // Use the MediaStore to find the ID of the image
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Video.Media.DATA + "=?";
        String[] selectionArgs = {imagePath};
        Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                Uri deleteUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                LogTools.i("deleteVideo " + imagePath + " , " + deleteUri.getPath());
                context.getContentResolver().delete(deleteUri, null, null);
            }
            cursor.close();
        }
    }

    /***
     * get all picture
     * @param context
     */
    public static List<Multimedia> getAllImages(Context context) {
        List<Multimedia> list = new ArrayList<>();
        try {
            // Query the content provider
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Cursor cursor = context.getContentResolver().query(
                    uri, // The content URI of the words table
                    null,
                    null,         // Selection criteria
                    null,         // Selection criteria
                    MediaStore.Images.Media.DATE_MODIFIED + " desc");        // The sort order for the returned rows

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                if (idColumn != -1) {
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    int dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);
                    int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
                    int widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH);
                    int heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                    int dateAddDateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

                    while (cursor.moveToNext()) {
                        Multimedia picture = new Multimedia();
                        long id = cursor.getLong(idColumn);
                        Uri contentUri = ContentUris.withAppendedId(uri, id);
                        picture.setId(cursor.getLong(idColumn));
                        picture.setPath(cursor.getString(dataColumn));
                        long date = cursor.getLong(dateTakenColumn);
                        picture.setDateTaken(date > 0 ? date : cursor.getLong(dateAddDateColumn));
                        picture.setTitle(cursor.getString(titleColumn));
                        picture.setSize(cursor.getLong(sizeColumn));
                        int w = cursor.getInt(widthColumn);
                        int h = cursor.getInt(heightColumn);
                        if (w == 0) {
                            BitmapUtils.BitmapSize bitmapSize = BitmapUtils.getBitmapSize(picture.getPath());
                            w = bitmapSize.width;
                            h = bitmapSize.height;
//                            updatePicSize(context,w,h,cursor.getLong(idColumn));
                        }
                        picture.setWidth(w);
                        picture.setHeight(h);
                        picture.setSelected(false);
                        picture.setShowCheckbox(false);
                        picture.setMediaType(Constant.MEDIA_PIC);
//                        picture.setUri(contentUri);
                        if (w > 0) {
                            list.add(picture);
                        }
                    }
                }
                 LogTools.i("picture list size " + list.toString());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    private static void updatePicSize(Context context, int w, int h, long id) {
        final long token = Binder.clearCallingIdentity();
        try {
            ContentResolver contentResolver = context.getContentResolver();
//        String selection = MediaStore.Images.Media._ID + "=?";
            String selection = null;
//        String[] selectionArgs = new String[]{String.valueOf(id)};
            String[] selectionArgs = null;
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DESCRIPTION, "Edited image description");
            values.put(MediaStore.Images.Media.WIDTH, w);
            values.put(MediaStore.Images.Media.HEIGHT, h);
            values.put(MediaStore.Images.Media.TITLE, "fde_" + System.currentTimeMillis());
//        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            int updatedRows = contentResolver.update(imageUri, values, selection, selectionArgs);
            if (updatedRows > 0) {
                LogTools.i("Image database updated successfully ");
            } else {
                LogTools.i("Image database selection " + selection + ",id " + id);
                LogTools.i("Failed to update image database  " + " ,w " + w + " ,updatedRows " + updatedRows);
            }

            try {
                Cursor cursorQ = contentResolver.query(imageUri, null, selection, selectionArgs, null);
                int widthColumn = cursorQ.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH);
                int heightColumn = cursorQ.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT);
                int dataColumn = cursorQ.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                while (cursorQ.moveToNext()) {
                    int ww = cursorQ.getInt(widthColumn);
                    int hh = cursorQ.getInt(heightColumn);
                    String path = cursorQ.getString(dataColumn);
                }
                cursorQ.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * get all video
     *
     * @param context
     */
    public static List<Multimedia> getAllVideos(Context context) {
        List<Multimedia> list = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                uri, // The content URI of the words table
                null,   // The columns to return for each row
                null,         // Selection criteria
                null,         // Selection criteria
                MediaStore.Images.Media.DATE_MODIFIED + " desc");        // The sort order for the returned rows
        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
            int heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int dateAddDateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

            while (cursor.moveToNext()) {
                Multimedia video = new Multimedia();
                long id = cursor.getLong(idColumn);
                Uri contentUri = ContentUris.withAppendedId(uri, id);
                video.setId(id);
                video.setPath(cursor.getString(dataColumn));
                video.setSize(cursor.getLong(sizeColumn));
                video.setDuration(cursor.getInt(durationColumn));
                video.setTitle(cursor.getString(titleColumn));
                long date = cursor.getLong(dateTakenColumn);
                video.setDateTaken(date > 0 ? date : cursor.getLong(dateAddDateColumn));
                video.setMediaType(Constant.MEDIA_VIDEO);
//                video.setUri(contentUri);
                list.add(video);
            }
//            LogTools.i("video list size " + list.toString());
            cursor.close();
        }
        return list;
    }
    
    public static Multimedia getMultimediaByPath(Context context, String titleName) {
       try {
           List<Multimedia> list =  getAllVideos(context);
           for (Multimedia multimedia : list) {
               if (titleName.equals(multimedia.getTitle())) {
                   return multimedia;
               }
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
       return null;
    }

    public static Multimedia getNextPicture(Context context) {
        String[] projection = new String[]{MediaStore.Images.Media._ID};

        // 按时间降序排序
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            int imageIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

            if (imageIdIndex != -1) {
                long currentImageId = cursor.getLong(imageIdIndex);

                // 移动光标到下一张图片位置
                if (!cursor.isLast() || !cursor.moveToNext()) {
                    return null; // 没有更多图片
                } else {
                    long nextImageId = cursor.getLong(imageIdIndex);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, nextImageId);
                    Multimedia pic = new Multimedia();
                    pic.setPath(uri.getPath());
                    return pic;
                }
            }
            cursor.close();
        }

        return null; // 无法获取任何图片
    }

    /**
     * @return
     */
    public static File createFolders() {
        File baseDir;
        if (android.os.Build.VERSION.SDK_INT < 8) {
            baseDir = Environment.getExternalStorageDirectory();
        } else {
            baseDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        }
        if (baseDir == null)
            return Environment.getExternalStorageDirectory();
        File aviaryFolder = new File(baseDir, FOLDER_NAME);
        if (aviaryFolder.exists())
            return aviaryFolder;
        if (aviaryFolder.isFile())
            aviaryFolder.delete();
        if (aviaryFolder.mkdirs())
            return aviaryFolder;
        return Environment.getExternalStorageDirectory();
    }

    /**
     * @return
     */
    public static File genEditFile() {
        return FileUtils.getEmptyFile(""
                + System.currentTimeMillis() + ".png");
    }

    /**
     * @param name
     * @return
     */
    public static File getEmptyFile(String name) {
        File folder = FileUtils.createFolders();
        if (folder != null) {
            if (folder.exists()) {
                File file = new File(folder, name);
                return file;
            }
        }
        return null;
    }

    /**
     * @param uri
     * @return
     */
    public static String getFilePathFromUri(Context context, Uri uri) {
        String filePath = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        }

        return filePath;
    }

    // 获取 MediaStore.MediaColumns._ID
    public static String getMediaStoreIdFromUri(Context context, Uri uri) {
        String mediaId = null;

        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];

                Uri contentUri = null;
                if ("primary".equalsIgnoreCase(type)) {
                    // 构建与 MediaStore 兼容的 URI
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }

                // 查询 _ID
                String[] projection = {MediaStore.MediaColumns._ID};
                String selection = MediaStore.Images.Media.DATA + "=?";
                String[] selectionArgs = new String[]{"/storage/emulated/0/" + split[1]};

                try (Cursor cursor = context.getContentResolver().query(contentUri, projection, selection, selectionArgs, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                        mediaId = cursor.getString(idColumnIndex);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mediaId;
    }

    public static Bitmap getFlvBitmap(String path) {
        Bitmap bitmap = null;
        try {
            FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
            mmr.setDataSource(path);
            bitmap = mmr.getFrameAtTime(0, FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            mmr.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static void rotateImage90AndSave(String inputPath, String outputPath) {
        // 1. 读取图片
        Bitmap bitmap = BitmapFactory.decodeFile(inputPath);
        if (bitmap == null) {
            System.out.println("图片加载失败: " + inputPath);
            return;
        }

        // 2. 创建旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        // 3. 创建旋转后的 Bitmap
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );

        // 4. 保存到文件
        File outFile = new File(outputPath);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 5. 回收 Bitmap
        bitmap.recycle();
        rotatedBitmap.recycle();
    }

    public static String getRealPathFromUriT(Context context, Uri uri) {

        if (DocumentsContract.isDocumentUri(context, uri)) {

            String docId = DocumentsContract.getDocumentId(uri);
            // image/467

            String[] split = docId.split(":");
            String type;
            String id;

            if (split.length == 2) {
                type = split[0];
                id = split[1];
            } else {
                // image/467 这种格式
                split = docId.split("/");
                type = split[0];
                id = split[1];
            }

            Uri contentUri = null;

            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }else {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }

            String selection = "_id=?";
            String[] selectionArgs = new String[]{id};

            Cursor cursor = context.getContentResolver().query(
                    contentUri,
                    new String[]{MediaStore.MediaColumns.DATA},
                    selection,
                    selectionArgs,
                    null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int column =
                                cursor.getColumnIndexOrThrow(
                                        MediaStore.MediaColumns.DATA);

                        return cursor.getString(column);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        return null;
    }

    // ==================== 回收站相关工具 ====================

    /**
     * 回收站目录（隐藏目录，避免被媒体扫描到）。
     * 优先使用公共目录 Pictures/fde/.trash，不可写时退化到应用私有目录。
     */
    public static File getTrashDir(Context context) {
        File publicDir = null;
        try {
            File base = createFolders();
            if (base != null) {
                publicDir = new File(base, ".trash");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ensureWritableDir(publicDir)) {
            return publicDir;
        }

        File appDir = null;
        if (context != null) {
            File ext = context.getExternalFilesDir(null);
            appDir = new File(ext != null ? ext : context.getFilesDir(), ".trash");
        }
        if (ensureWritableDir(appDir)) {
            return appDir;
        }
        LogTools.e("getTrashDir: no writable trash directory");
        return null;
    }

    private static boolean ensureWritableDir(File dir) {
        if (dir == null) {
            return false;
        }
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                return false;
            }
            return dir.isDirectory() && dir.canWrite();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 移动文件，rename 失败时退化为拷贝 + 删除
     */
    public static boolean moveFile(File src, File dst) {
        if (src == null || dst == null || !src.exists()) {
            LogTools.e("moveFile: src not exists " + (src == null ? "null" : src.getAbsolutePath()));
            return false;
        }
        try {
            File parent = dst.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (src.renameTo(dst)) {
            return true;
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            LogTools.e("moveFile copy failed: " + e);
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return src.delete();
    }

    /**
     * 通知媒体库刷新某个文件
     */
    public static void scanFile(Context context, String path) {
        try {
            MediaScannerConnection.scanFile(context, new String[]{path}, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询媒体库中某个文件的 _ID，找不到返回 -1
     */
    public static long queryMediaStoreId(Context context, int mediaType, String path) {
        Uri uri = mediaType == Constant.MEDIA_VIDEO
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = {path};
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 删除媒体库记录（不删除文件本身）
     */
    public static void deleteMediaStoreEntry(Context context, int mediaType, String path) {
        long id = queryMediaStoreId(context, mediaType, path);
        if (id < 0) {
            return;
        }
        Uri baseUri = mediaType == Constant.MEDIA_VIDEO
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        context.getContentResolver().delete(ContentUris.withAppendedId(baseUri, id), null, null);
    }

}
