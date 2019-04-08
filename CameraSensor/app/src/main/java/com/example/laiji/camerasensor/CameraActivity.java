package com.example.laiji.camerasensor;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.example.laiji.camerasensor.view.MoveCameraFoucs;
import com.example.laiji.camerasensor.view.ResizeAbleSurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity implements SensorEventListener {
MoveCameraFoucs mMoveCameraFoucs;
SurfaceView mSurface;
SurfaceHolder mHolder;
Camera.Parameters mParameters;
Camera mCamera;
ResizeAbleSurfaceView mResizeAbleSurfaceView;
private SensorManager mSensorManager;
Calendar mCalendar;

private String TAG = "lylog";
    private int mPrewidth;
    private int mPreHeight;
    private Sensor mSensor;
    private int mX, mY, mZ;
    private long lastStaticStamp = 0;

    boolean isFocusing = false;
    boolean canFocusIn = false;  //内部是否能够对焦控制机制
    boolean canFocus = false;

    public static final int DELEY_DURATION = 500;

    public static final int STATUS_NONE = 0;
    public static final int STATUS_STATIC = 1;
    public static final int STATUS_MOVE = 2;
    private int STATUE = STATUS_NONE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);// 横屏
        iniView();
    }

    private void initCamera() {

        if (mCamera == null){
            mCamera = Camera.open();
            mParameters = mCamera.getParameters();
        }
        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(mParameters);
        Log.d(TAG, "initCamera: w = "+mParameters.getPreviewSize().width + " h = "+mParameters.getPreviewSize().height);
        mPrewidth = mParameters.getPreviewSize().width;
        mPreHeight =mParameters.getPreviewSize().height;
        startPreView();
//        CameraTimerTask cttask = new CameraTimerTask();
//        Timer timer = new Timer(true);
//        timer.schedule(cttask, 0, 2000);//定时每秒执行一次
    }

    private void startPreView() {
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void iniView() {
        mMoveCameraFoucs = findViewById(R.id.focusView);
        mSurface = findViewById(R.id.surfaceView);
        mHolder = mSurface.getHolder();
        mHolder.addCallback(mSurfaceCallBack);
        mResizeAbleSurfaceView = findViewById(R.id.surfaceView);

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Activity.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// TYPE_GRAV

        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        setMoveFocusViewVisble(false);
    }

    private SurfaceHolder.Callback mSurfaceCallBack = new SurfaceHolder.Callback(){

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            initCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged: wight = "+width + "  height"+height);
            //处理surface和presize不一致的问题
            if (width != mPrewidth || height !=mPreHeight) {
                mResizeAbleSurfaceView.resize(mPrewidth,mPreHeight);
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            releasecamera();
        }
    };

    private void releasecamera() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mSensor);
            mSensorManager = null;
        }
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.cancelAutoFocus();
            mCamera.release();
            mCamera =null;
        }
    }

    @Override
    protected void onDestroy() {
        releasecamera();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {


            try{
                List<Camera.Area> mFocusArea = new ArrayList<Camera.Area>();
                mFocusArea.add(new Camera.Area(new Rect(), 1));
                calculateTapArea((int)event.getX(),(int)event.getY(),1f,mFocusArea.get(0).rect);
                mParameters.setFocusAreas(mFocusArea);
                mCamera.setParameters(mParameters);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "onTouchEvent: ACTION_DOWN");
            isFocusing = false;
            setMoveFocusViewVisble(true);
        }

        try {
            mCamera.autoFocus(autoFocus);
        } catch (Exception e) {
            e.printStackTrace();

        }
        return super.onTouchEvent(event);
    }

    public Camera.AutoFocusCallback autoFocus = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                isFocusing = false;
                Log.d(TAG, "onAutoFocus: ");
                mCamera.setOneShotPreviewCallback(null);
                mCamera.cancelAutoFocus();
                mMoveCameraFoucs.setFocused(mHandler);
            } else {
                isFocusing = true;
                mMoveCameraFoucs.setNoFocused();
                mCamera.cancelAutoFocus();
            }
        }
    };

    private void setfocusViewIsGreen() {
        mMoveCameraFoucs.setFocused(mHandler);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setMoveFocusViewVisble(false);
        isFocusing = true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    };

    private final Rect mPreviewRect = new Rect(150, 75, 260, 200);
    private void calculateTapArea(int x, int y, float areaMultiple, Rect rect) {
        int areaSize = (int) (getAreaSize() * areaMultiple);
        int left = clamp(x - areaSize / 2, mPreviewRect.left,
                mPreviewRect.right - areaSize);
        int top = clamp(y - areaSize / 2, mPreviewRect.top,
                mPreviewRect.bottom - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        rectFToRect(rectF, rect);
    }
    private int getAreaSize() {
        // Recommended focus area size from the manufacture is 1/8 of the image
        // width (i.e. longer edge of the image)
        return Math.max(mPreviewRect.width(), mPreviewRect.height()) / 8;
    }
    public static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "onSensorChanged: ");
        if (event.sensor == null) {
            return;
        }

        if (isFocusing) {
            restParams();
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            int x = (int) event.values[0];
            int y = (int) event.values[1];
            int z = (int) event.values[2];
            mCalendar = Calendar.getInstance();
            long stamp = mCalendar.getTimeInMillis();// 1393844912

            int second = mCalendar.get(Calendar.SECOND);// 53

            if (STATUE != STATUS_NONE) {
                int px = Math.abs(mX - x);
                int py = Math.abs(mY - y);
                int pz = Math.abs(mZ - z);
//                Log.d(TAG, "pX:" + px + "  pY:" + py + "  pZ:" + pz + "    stamp:"
//                        + stamp + "  second:" + second);
                double value = Math.sqrt(px * px + py * py + pz * pz);
                if (value > 1.4) {
//                    textviewF.setText("检测手机在移动..");
//                    Log.i(TAG,"mobile moving");
                    STATUE = STATUS_MOVE;
                } else {
//                    textviewF.setText("检测手机静止..");
//                    Log.i(TAG,"mobile static");
                    //上一次状态是move，记录静态时间点
                    if (STATUE == STATUS_MOVE) {
                        lastStaticStamp = stamp;
                        canFocusIn = true;
                    }

                    if (canFocusIn) {
                        if (stamp - lastStaticStamp > DELEY_DURATION) {
                            //移动后静止一段时间，可以发生对焦行为
                            if (!isFocusing) {
                                canFocusIn = false;
//                                onCameraFocus();
                                if (mCamera != null) {
                                    Log.d(TAG, "onSensorChanged: autofocus");
                                    mCamera.autoFocus(autoFocus);
                                }
//                                Log.i(TAG,"mobile focusing");
                            }
                        }
                    }

                    STATUE = STATUS_STATIC;
                }
            } else {
                lastStaticStamp = stamp;
                STATUE = STATUS_STATIC;
            }

            mX = x;
            mY = y;
            mZ = z;
        }
    }

    private void restParams() {
        STATUE = STATUS_NONE;
        canFocusIn = false;
        mX = 0;
        mY = 0;
        mZ = 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    class CameraTimerTask extends TimerTask {
        @Override
        public void run() {
            if (mCamera != null && isFocusing) {
                mCamera.autoFocus(autoFocus);
            }

        }
    }
    public void setMoveFocusViewVisble(boolean isshow){
        Log.d(TAG, "setMoveFocusViewVisble: isshow = "+isshow);
        if (isshow) {
            mMoveCameraFoucs.setVisibility(View.VISIBLE);
        }else {
            mMoveCameraFoucs.setVisibility(View.GONE);
        }
    }

    public void resetFocusUI(){
        mMoveCameraFoucs.resetFocus();
    }

}
