package com.example.laiji.camerasensor;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;

public class MyApplication extends Application {

    public static int mScreenWidth = 0;
    public static int mScreenHeight = 0;

    public static MyApplication CONTEXT;

    private Bitmap mCameraBitmap;

    @Override
    public void onCreate() {
        super.onCreate();

        // DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        DisplayMetrics mDisplayMetrics = getApplicationContext().getResources()
                .getDisplayMetrics();
        MyApplication.mScreenWidth = mDisplayMetrics.widthPixels;
        MyApplication.mScreenHeight = mDisplayMetrics.heightPixels;

        CONTEXT = this;

        //  FileUtil.initFolder();
    }

    public Bitmap getCameraBitmap() {
        return mCameraBitmap;
    }

    public void setCameraBitmap(Bitmap mCameraBitmap) {
        if (mCameraBitmap != null) {
            recycleCameraBitmap();
        }
        this.mCameraBitmap = mCameraBitmap;
    }

    public void recycleCameraBitmap() {
        if (mCameraBitmap != null) {
            if (!mCameraBitmap.isRecycled()) {
                mCameraBitmap.recycle();
            }
            mCameraBitmap = null;
        }
    }
}