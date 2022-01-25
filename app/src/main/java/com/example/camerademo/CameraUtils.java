package com.example.camerademo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @作者 xhlm
 * @创建日期 2021/8/14
 * @类名 AlbumUtils
 * @所在包 com\example\filemanage\AlbumUtils.java
 * @描述 Android系统相机工具类，适配到Android11
 */
public class CameraUtils {

    private static final String TAG = "AlbumUtils";

    public static final int CAMERA_TAKE_PHOTO_PERMISSION = 2000;//相机权限申请码
    public static final int CAMERA_SELECT_PHOTO_PERMISSION = 2001;//相册权限申请码
    public static final int CAMERA_CROP_PERMISSION = 2002;//裁剪权限申请码

    public static final int CAMERA_TAKE_PHOTO = 3000;//跳转相机
    public static final int CAMERA_SELECT_PHOTO = 3001;//跳转相册
    public static final int CAMERA_CROP = 3002;//跳转裁剪

    private static final boolean isAndroidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;//是否Android10(Q)

    public static final String AUTHORITY = "com.example.camerademo.fileprovider2";//fileProvider声明路径

    //=============================================操作=============================================

    /**
     * 打开相机
     * AndroidQ以上：图片保存进公共目录内(公共目录/picture/子文件夹)
     * AndroidQ以下：相片保存进沙盒目录内(沙盒目录/picture/子文件夹)
     * @param activity activity
     * @param name 相片名
     * @param child 存放的子文件夹
     * @return 成功即为uri，失败为null，等到相机拍照后，该uri即为照片
     */
    public static Uri openCamera(Activity activity, String name, String child) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            //无相机
            Log.e(TAG, "无相机");
            return null;
        }
        if (name == null || name.equals("")) {
            name = System.currentTimeMillis() + ".png";
        } else {
            name = name + ".png";
        }
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "不存在存储卡或没有读写权限");
            return null;
        }
        Uri uri;
        if (isAndroidQ) {
            uri = createImageUriAboveAndroidQ(activity, name, child);
        } else {
            uri = createImageCameraUriBelowAndroidQ(activity, name, child);
        }
        if (uri == null) {
            Log.e(TAG, "用于存放照片的uri创建失败");
            return null;
        }
        Log.e(TAG, "cameraUri：" + uri);
        //指定图片保存位置
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        activity.startActivityForResult(intent, CAMERA_TAKE_PHOTO);
        return uri;
    }

    //打开相册
    public static void openAlbum(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        activity.startActivityForResult(intent, CAMERA_SELECT_PHOTO);
    }

    /**
     * 图片裁剪，裁剪后存放在沙盒目录下(沙盒目录/picture/子文件夹)
     * @param activity activity
     * @param uri 图片uri
     * @param name 裁剪后的图片名
     * @param child 子文件夹
     * @return 裁剪后的图片uri
     */
    public static Uri openCrop(Activity activity, Uri uri, String name, String child) {
        if (uri == null) {
            Log.e(TAG, "uri为空");
            return null;
        }
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //未挂在存储设备或者没有读写权限
            return null;
        }
        if (name != null && !name.equals("")) {
            name = name + ".png";
        } else {
            name = System.currentTimeMillis() + ".png";
        }

        Uri resultUri;
        if (isAndroidQ) {
            resultUri = createImageUriAboveAndroidQ(activity, name, child);
        } else {
            resultUri = createImageCropUriBelowAndroidQ(activity, name, child);
        }
        if (resultUri == null) {
            Log.e(TAG, "用于存放照片的uri创建失败");
            return null;
        }
        Log.e(TAG, "cropUri：" + resultUri);
        Intent intent = new Intent("com.android.camera.action.CROP");
        //下面那句话如果添加的话，小米调用系统相册则无法裁剪，暂时备注掉，未测试其他机型
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(uri, "image/*");
        // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, resultUri);
        // 图片格式
        intent.putExtra("outputFormat", "png");
        intent.putExtra("noFaceDetection", true);// 取消人脸识别
        intent.putExtra("return-data", true);// true:不返回uri，false：返回uri
        activity.startActivityForResult(intent, CAMERA_CROP);
        return resultUri;
    }

    /**
     * 将uri转换为file
     * uri类型为file的直接转换出路径
     * uri类型为content的将对应的文件复制到沙盒内的cache目录下进行操作
     * @param context 上下文
     * @param uri uri
     * @return file
     */
    public static File uriToFile(Context context, Uri uri) {
        if (uri == null) {
            Log.e(TAG, "uri为空");
            return null;
        }
        File file = null;
        if (uri.getScheme() != null) {
            Log.e(TAG, "uri.getScheme()：" + uri.getScheme());
            if (uri.getScheme().equals(ContentResolver.SCHEME_FILE) && uri.getPath() != null) {
                //此uri为文件，并且path不为空(保存在沙盒内的文件可以随意访问，外部文件path则为空)
                file = new File(uri.getPath());
            } else if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                //此uri为content类型，将该文件复制到沙盒内
                ContentResolver resolver = context.getContentResolver();
                @SuppressLint("Recycle")
                Cursor cursor = resolver.query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    String fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    try {
                        InputStream inputStream = resolver.openInputStream(uri);
                        if (context.getExternalCacheDir() != null) {
                            //该文件放入cache缓存文件夹中
                            File cache = new File(context.getExternalCacheDir(), fileName);
                            FileOutputStream fileOutputStream = new FileOutputStream(cache);
                            if (inputStream != null) {
//                                    FileUtils.copy(inputStream, fileOutputStream);
                                //上面的copy方法在低版本的手机中会报java.lang.NoSuchMethodError错误，使用原始的读写流操作进行复制
                                byte[] len = new byte[Math.min(inputStream.available(), 1024 * 1024)];
                                int read;
                                while ((read = inputStream.read(len)) != -1) {
                                    fileOutputStream.write(len, 0, read);
                                }
                                file = cache;
                                fileOutputStream.close();
                                inputStream.close();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return file;
    }

    /**
     * 更新系统相册
     * @param uri uri
     */
    public static void updateSystem(Context context, Uri uri) {
        if (uri == null) {
            Log.e(TAG, "uri为空");
            return;
        }
        Log.e(TAG, "更新系统相册uri：" + uri);
        if (uri.getScheme() == null) {
            return;
        }
        if (uri.getScheme().equals(ContentResolver.SCHEME_FILE) && uri.getPath() != null) {
            File file = new File(uri.getPath());
            String[] paths = new String[]{file.getAbsolutePath()};
            Log.e(TAG, "paths：" + Arrays.toString(paths));
            MediaScannerConnection.scanFile(context, paths, null, null);
        }
    }

    //=============================================权限=============================================

    /**
     * 检查是否拥有打开相机所需的所有权限
     * @param context 上下文
     * @return true：已拥有，false：存在未拥有的权限
     */
    public static boolean checkTakePhotoPermission(Context context) {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) //打开相机权限
                && (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) //写权限
                && (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED); //读权限
    }

    /**
     * 检查是否拥有打开相册所需的所有权限
     * @param context 上下文
     * @return true：已拥有，false：存在未拥有的权限
     */
    public static boolean checkSelectPhotoPermission(Context context) {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) //写权限
                && (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED); //读权限
    }

    /**
     * 检查是否拥有打开裁剪所需的所有权限
     * @param context 上下文
     * @return true：已拥有，false：存在未拥有的权限
     */
    public static boolean checkCropPermission(Context context) {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) //写权限
                && (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED); //读权限
    }

    /**
     * 申请相机所需权限
     * 申请回调请通过传入的activity中的{@link android.app.Activity#onRequestPermissionsResult}方法判断
     * 判断可再次使用{@link CameraUtils#checkTakePhotoPermission}
     * @param activity activity
     */
    public static void requestTakePhotoPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, CAMERA_TAKE_PHOTO_PERMISSION);
    }

    /**
     * 申请相册所需权限
     * 申请回调请通过传入的activity中的{@link android.app.Activity#onRequestPermissionsResult}方法判断
     * 判断可再次使用{@link CameraUtils#checkSelectPhotoPermission}
     * @param activity activity
     */
    public static void requestSelectPhotoPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, CAMERA_SELECT_PHOTO_PERMISSION);
    }

    /**
     * 申请裁剪所需权限
     * 申请回调请通过传入的activity中的{@link android.app.Activity#onRequestPermissionsResult}方法判断
     * 判断可再次使用{@link CameraUtils#checkTakePhotoPermission}
     * @param activity activity
     */
    public static void requestCropPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, CAMERA_CROP_PERMISSION);
    }

    //=============================================内部方法=============================================

    /**
     * AndroidQ以上创建用于保存相片的uri，(公有目录/pictures/child)
     * @param activity activity
     * @param name 文件名
     * @param child 子文件夹
     * @return uri
     */
    private static Uri createImageUriAboveAndroidQ(Activity activity, String name, String child) {
        ContentValues contentValues = new ContentValues();//内容
        ContentResolver resolver = activity.getContentResolver();//内容解析器
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, name);//文件名
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*");//文件类型
        if (child != null && !child.equals("")) {
            //存放子文件夹
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + child);
        } else {
            //存放picture目录
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    /**
     * AndroidQ以下创建用于保存拍照的照片的uri，(沙盒目录/pictures/child)
     * 拍照传入的intent中
     * Android7以下：file类型的uri
     * Android7以上：content类型的uri
     * @param activity activity
     * @param name 文件名
     * @param child 子文件夹
     * @return content uri
     */
    private static Uri createImageCameraUriBelowAndroidQ(Activity activity, String name, String child) {
        File pictureDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);//标准图片目录
        assert pictureDir != null;//获取沙盒内标准目录是不会为null的
        if (getDir(pictureDir)) {
            if (child != null && !child.equals("")) {//存放子文件夹
                File childDir = new File(pictureDir + "/" + child);
                if (getDir(childDir)) {
                    File picture = new File(childDir, name);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        //适配Android7以上的path转uri
                        return FileProvider.getUriForFile(activity, AUTHORITY, picture);
                    } else {
                        //Android7以下
                        return Uri.fromFile(picture);
                    }
                } else {
                    return null;
                }
            } else {//存放当前目录
                File picture = new File(pictureDir, name);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //适配Android7以上的path转uri，该方法得到的uri为content类型的
                    return FileProvider.getUriForFile(activity, AUTHORITY, picture);
                } else {
                    //Android7以下，该方法得到的uri为file类型的
                    return Uri.fromFile(picture);
                }
            }
        } else {
            return null;
        }
    }

    /**
     * AndroidQ以下创建用于保存裁剪的uri，(沙盒目录/pictures/child)
     * 裁剪传入intent的uri跟拍照不同
     * 在AndroidQ以下统一使用file类型的uri，所以统一用Uri.fromFile()方法返回
     * @param activity activity
     * @param name 文件名
     * @param child 子文件夹
     * @return file uri
     */
    private static Uri createImageCropUriBelowAndroidQ(Activity activity, String name, String child) {
        File pictureDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);//标准图片目录
        assert pictureDir != null;//获取沙盒内标准目录是不会为null的
        if (getDir(pictureDir)) {
            if (child != null && !child.equals("")) {//存放子文件夹
                File childDir = new File(pictureDir + "/" + child);
                if (getDir(childDir)) {
                    File picture = new File(childDir, name);
                    return Uri.fromFile(picture);
                } else {
                    return null;
                }
            } else {//存放当前目录
                File picture = new File(pictureDir, name);
                return Uri.fromFile(picture);
            }
        } else {
            return null;
        }
    }

    /**
     * 获取文件夹
     * @param file 文件夹
     * @return true：已存在/不存在，创建成功，false：不存在，创建失败
     */
    private static boolean getDir(File file) {
        if (file.exists()) {
            return true;
        } else {
            return file.mkdir();
        }
    }
}
