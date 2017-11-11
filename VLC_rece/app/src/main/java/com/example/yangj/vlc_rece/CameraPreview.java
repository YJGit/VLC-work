package com.example.yangj.vlc_rece;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by yangJ on 2017/11/11.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Handler mHandler;
    private static String TAG = "CameraPreview";

    private DTask mTask = null;

    public CameraPreview(Context context, Camera camera, Handler handler) {
        super(context);
        mCamera = camera;
        mHandler = handler;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }
        // make any resize, rotate or reformatting changes here
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d("CP", "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        if(mCamera != null) {
            mCamera.setOneShotPreviewCallback(null);
            mCamera.release();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);//new

            //调用PreviewCallback
            //mCamera.setOneShotPreviewCallback(new MyCamera());
            Thread s = new Thread(new ScanThread());
            s.start();
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    class MyCamera implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            /*
            *data 实时预览的帧
            * 按理说只要在onPreviewFrame这个函数里写你的处理程序就可以了。但是通常不这么做，因为处理实时预览帧视频的算法
            * 可能比较复杂，这就需要借助AsyncTask开启一个线程在后台处理数据
            */

            if(null != mTask){
                switch(mTask.getStatus()){
                    case RUNNING:
                        return;
                    case PENDING:
                        mTask.cancel(false);
                        break;
                }
            }
            mTask = new DTask(data);
            mTask.execute((Void) null);
        }
    }


    class DTask extends AsyncTask<Void, Void, Void> {
        private byte[] mData;
        //构造函数
        public DTask(byte[] data){
            this.mData = data;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            Camera.Size size = mCamera.getParameters().getPreviewSize(); //获取预览大小
            final int w = size.width;  //宽度
            final int h = size.height;
            final YuvImage image = new YuvImage(mData, ImageFormat.NV21, w, h, null);
            ByteArrayOutputStream os = new ByteArrayOutputStream(mData.length);
            if(!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)){
                return null;
            }
            byte[] tmp = os.toByteArray();
            Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
            //bmp.getPixels(pixels, 0, w, 0, 0, w, h);
            String data = decode(bmp);
            mHandler.sendMessage(mHandler.obtainMessage(0, data));
            return null;
        }

        //解码
        private String decode(Bitmap bmp) {
            String data = "";
            data = "110120110120";
            return data;
        }
    }

    class ScanThread implements Runnable{
        public void run() {
            // TODO Auto-generated method stub
            while(!Thread.currentThread().isInterrupted()){
                try {
                    mCamera.setOneShotPreviewCallback(new MyCamera());
                    Thread.sleep(100);    //1s
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
