package com.example.yangj.vlc_rece;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView mTextView;
    android.hardware.Camera mCamera;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //android 6及以后需要权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST","Granted");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }

        mTextView = (TextView)findViewById(R.id.textView);
        mCamera = CreateCamera();
        //显示信息
        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                mTextView.setText((String) msg.obj);
            };
        };

        CameraPreview cp = new CameraPreview(this, mCamera, mHandler);
        FrameLayout preview = (FrameLayout)findViewById(R.id.camera_preview);
        preview.addView(cp);
    }

    public static android.hardware.Camera CreateCamera(){
        android.hardware.Camera c = null;
        try{
            c = android.hardware.Camera.open();
        }
        catch (Exception e){
            //"camera is not avaliable"
        }
        return c;
    }

}
