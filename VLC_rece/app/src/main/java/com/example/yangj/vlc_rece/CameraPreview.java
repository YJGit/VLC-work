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
import android.view.TextureView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by yangJ on 2017/11/11.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private TextView mTextView;
    private static String TAG = "CameraPreview";

    private DTask mTask = null;

    private double[] bmpPool;
    static int num = 2;
    int begin, end;

    private String data;
    String lastBit = "0";

    //停止解码  连续endNum个0
    boolean stop = false;
    //int conti_zero = 0;
    int conti_one = 0;
    int endNum = 8;

    public CameraPreview(Context context, Camera camera, TextView textView) {
        super(context);
        mCamera = camera;
        mTextView = textView;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        bmpPool = new double[num];
        for (int i = 0; i < num; i++) {
            bmpPool[0] = 0.0;
        }
        begin = end = 0;
        data = "";
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        // make any resize, rotate or reformatting changes here
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d("CP", "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        if (mCamera != null) {
            mCamera.setOneShotPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);//new

            //调用PreviewCallback
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

            if (null != mTask) {
                Log.d(TAG, "onPreviewFrame: " + mTask.getStatus());
                switch (mTask.getStatus()) {
                    case RUNNING:
                        return;
                    case PENDING:
                        mTask.cancel(false);
                        break;
                }
            }

            //解码没结束 继续task
            if(!stop) {
                mTask = new DTask(data);
                mTask.execute((Void) null);
            }

        }
    }


    class DTask extends AsyncTask<Void, Void, String> {
        private byte[] mData;

        //构造函数
        public DTask(byte[] data) {
            this.mData = data;
        }

        //onPostExecute方法用于在执行完后台任务后更新UI,显示结果
        protected void onPostExecute(String result) {
            Log.d(TAG, "onPostExecute(Result result) called");
            mTextView.setText(result);
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub
            Camera.Size size = mCamera.getParameters().getPreviewSize(); //获取预览大小
            final int w = size.width;  //宽度
            final int h = size.height;
            final YuvImage image = new YuvImage(mData, ImageFormat.NV21, w, h, null);
            ByteArrayOutputStream os = new ByteArrayOutputStream(mData.length);
            if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
                return null;
            }
            byte[] tmp = os.toByteArray();
            Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);

            try {
                Double sRgb = getSumRGB(bmp, w, h);
                bmpPool[end] = sRgb;

                int end_after = (end + 1) % num;
                if(end_after == begin) {
                    decode();
                }

                end = end_after;
                if(end == begin) begin = (begin + 1) % num;

                //是否结束
                if(/*conti_zero >= endNum || */conti_one >= endNum)
                    stop = true;

                return data;
            }catch (Exception e){
                Log.i(TAG, e.getMessage());
            }

            return null;
        }

        //解码数据
        private void decode(){
            if(bmpPool[end] > bmpPool[begin] * 1.05) {
                data += "0";
                lastBit = "1";
                //conti_zero++;
                conti_one = 0;
            }
            else if(bmpPool[end] < bmpPool[begin] * 0.95) {
                data += "1";
                lastBit = "0";
                //conti_zero = 0;
                conti_one++;
            }
            else {
                data += lastBit;
                if(lastBit.equals("0")) {
                    //conti_zero++;
                    conti_one = 0;
                }
                else {
                    //conti_zero = 0;
                    conti_one++;
                }
            }
        }

        //获取像素和
        private Double getSumRGB(Bitmap bmp, int w, int h) {

        //sum rgb
        int []pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h); //640 480
        double res = 0.0f;

        for(int i = 0; i < h; i++) {
            for(int j = 0; j < w; j++) {
                int grey = pixels[w * i + j];

                int red = ((grey  & 0x00FF0000 ) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                //grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                //grey = alpha | (grey << 16) | (grey << 8) | grey;
                //double t = (double) red * 0.3 + (double)green * 0.59 + (double)blue * 0.11;
                double t = red + green + blue;
                if(t < 205 * 3) t = (t / (3 * 255)) * 2.1;   //亮的地方加权重
                else t = (t / (3 * 255)) * 0.1;
                res += t;
            }
        }
        res = res / (w*h);
        res *= 1000;    //放大差距
        String output = Double.toString(res);
        Log.d(TAG, output);

        return res;
    }
}

    class ScanThread implements Runnable {
        public void run() {
            // TODO Auto-generated method stub
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if(mCamera != null) {
                        mCamera.setOneShotPreviewCallback(new MyCamera());
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
