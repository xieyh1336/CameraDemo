package com.example.camerademo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    ImageView ivPicture;
    TextView tvCapture, tvAlbum, tvCrop, tvFile, tvFilePath;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivPicture = findViewById(R.id.iv_picture);
        tvCapture = findViewById(R.id.tv_capture);
        tvAlbum = findViewById(R.id.tv_album);
        tvCrop = findViewById(R.id.tv_crop);
        tvFile = findViewById(R.id.tv_file);
        tvFilePath = findViewById(R.id.tv_file_path);
        tvCapture.setOnClickListener(this);
        tvAlbum.setOnClickListener(this);
        tvCrop.setOnClickListener(this);
        tvFile.setOnClickListener(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_capture:
                //相机
                if (CameraUtils.checkTakePhotoPermission(this)) {//检查权限
                    //有权限，打开相机
                    openCamera();
                } else {
                    //无权限，申请
                    CameraUtils.requestTakePhotoPermissions(this);
                }
                break;
            case R.id.tv_album:
                //相册
                if (CameraUtils.checkSelectPhotoPermission(this)) {//检查权限
                    //有权限，打开相册
                    openAlbum();
                } else {
                    //无权限，申请
                    CameraUtils.requestSelectPhotoPermissions(this);
                }
                break;
            case R.id.tv_crop:
                //裁剪
                if (CameraUtils.checkCropPermission(this)) {//检查权限
                    //有权限，打开裁剪
                    openCrop();
                } else {
                    //无权限，申请
                    CameraUtils.requestCropPermissions(this);
                }
                break;
            case R.id.tv_file:
                //转换
                if (uri != null) {
                    File file = CameraUtils.uriToFile(this, uri);
                    if (file != null) {
                        tvFilePath.setText("路径：" + file.getPath());
                    } else {
                        tvFilePath.setText("file:null");
                    }
                } else {
                    tvFilePath.setText("null");
                }
                break;
        }
    }

    //打开相机
    private void openCamera() {
        uri = CameraUtils.openCamera(this, "test", "albumDir");
    }

    //打开相册
    private void openAlbum() {
        CameraUtils.openAlbum(this);
    }

    private void openCrop() {
        if (uri == null) {
            return;
        }
        uri = CameraUtils.openCrop(this, uri, "testCrop", "cropDir");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //权限申请回调
        if (requestCode == CameraUtils.CAMERA_TAKE_PHOTO_PERMISSION) {
            //相机权限申请回调
            if (CameraUtils.checkTakePhotoPermission(this)) {
                openCamera();
            } else {
                //权限不足
                Log.e(TAG, "相机权限不足");
            }
        } else if (requestCode == CameraUtils.CAMERA_SELECT_PHOTO_PERMISSION) {
            //相册权限申请回调
            if (CameraUtils.checkSelectPhotoPermission(this)) {
                openAlbum();
            } else {
                //权限不足
                Log.e(TAG, "相册权限不足");
            }
        } else if (requestCode == CameraUtils.CAMERA_CROP_PERMISSION) {
            //裁剪权限申请回调
            if (CameraUtils.checkCropPermission(this)) {
                openCrop();
            } else {
                //权限不足
                Log.e(TAG, "裁剪权限不足");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //activity跳转回调
        if (uri != null) {
            Log.e(TAG, "activity回调，uri：" + uri);
        }
        if (data != null) {
            Log.e(TAG, "activity回调，data：" + data);
        }
        if (requestCode == CameraUtils.CAMERA_TAKE_PHOTO) {
            //相机跳转回调
            ivPicture.setImageURI(uri);//展示图片
            //通知系统相册更新信息
            CameraUtils.updateSystem(this, uri);
        } else if (requestCode == CameraUtils.CAMERA_SELECT_PHOTO) {
            //相册跳转回调
            if (data != null){
                ivPicture.setImageURI(data.getData());
                uri = data.getData();
            }
        } else if (requestCode == CameraUtils.CAMERA_CROP) {
            //裁剪跳转回调
            if (uri == null) {
                return;
            }
            ivPicture.setImageURI(uri);
            //通知系统相册更新信息
            CameraUtils.updateSystem(this, uri);
        }
    }
}