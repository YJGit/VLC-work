package com.liuguilin.pcmsample;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;


import android.content.pm.PackageManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Manifest;

public class MainActivity extends AppCompatActivity implements View.OnClickListener , ActivityCompat.OnRequestPermissionsResultCallback{

    //private static final int AUDIO_REQUEST = 0;

    public static final String TAG = "PCMSample";

    //是否在录制
    private boolean isRecording = false;
    //开始录音
    private Button startAudio;
    //结束录音
    private Button stopAudio;
    //播放录音
    private Button playAudio;
    //删除文件
    private Button deleteAudio;
    //转化pcm到wav
    private Button transAudio;

    private ScrollView mScrollView;
    private TextView tv_audio_succeess;

    //pcm文件
    private File file;

    //文件存储路径
    private String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*在此处插入运行时权限获取的代码*/
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        initView();
    }

    //初始化View
    private void initView() {
        mScrollView = (ScrollView) findViewById(R.id.mScrollView);
        tv_audio_succeess = (TextView) findViewById(R.id.tv_audio_succeess);
        printLog("初始化成功");
        startAudio = (Button) findViewById(R.id.startAudio);
        startAudio.setOnClickListener(this);

        stopAudio = (Button) findViewById(R.id.stopAudio);
        stopAudio.setOnClickListener(this);
        stopAudio.setEnabled(true);

        playAudio = (Button) findViewById(R.id.playAudio);
        playAudio.setOnClickListener(this);

        deleteAudio = (Button) findViewById(R.id.deleteAudio);
        deleteAudio.setOnClickListener(this);

        transAudio = (Button)findViewById(R.id.transAudio);
        transAudio.setOnClickListener(this);
    }

    //点击事件
    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick: " + v.getId());
        switch (v.getId()) {
            case R.id.startAudio:
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StartRecord();
                        Log.e(TAG,"start");
                    }
                });
                thread.start();
                printLog("开始录音");
                break;
            case R.id.stopAudio:
                isRecording = false;
                printLog("停止录音");
                break;
            case R.id.playAudio:

                PlayRecord();
                ButtonEnabled(true, false, false);
                printLog("播放录音");
                break;
            case R.id.deleteAudio:
                deleFile();
                break;
            case R.id.transAudio:
                transFile();
                break;
        }
    }

    //打印log
    private void printLog(final String resultString) {
        tv_audio_succeess.post(new Runnable() {
            @Override
            public void run() {
                tv_audio_succeess.append(resultString + "\n");
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    //获取/失去焦点
    private void ButtonEnabled(boolean start, boolean stop, boolean play) {
        startAudio.setEnabled(start);
        stopAudio.setEnabled(stop);
        playAudio.setEnabled(play);
    }

    //开始录音
    public void StartRecord() {
        Log.i(TAG,"开始录音");
        //设置采样频率
        int frequency = 48000;
        //设置音频采样声道，CHANNEL_IN_STEREO代表双声道，CHANNEL_IN_MONO代表单声道
        int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
        //设置音频数据格式，每个采样数据占16bit
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        //生成PCM文件
        file = new File(filePath + "/myaudio.pcm");
        Log.i(TAG,"生成文件");
        //如果存在，就先删除再创建
        if (file.exists())
            file.delete();
            Log.i(TAG,"删除文件");
        try {
            file.createNewFile();
            Log.i(TAG,"创建文件");
        } catch (IOException e) {
            Log.i(TAG,"未能创建");
            throw new IllegalStateException("未能创建" + file.toString());
        }
        try {
            //输出流
            OutputStream os = new FileOutputStream(file);
            int bufferSize = AudioRecord.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    frequency, channelConfiguration, audioEncoding, bufferSize);
            byte[] buffer = new byte[bufferSize];
            audioRecord.startRecording();
            Log.i(TAG, "开始录音");
            isRecording = true;
            while (isRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                os.write(buffer);
            }
            audioRecord.stop();
            os.close();
        } catch (Throwable t) {
            Log.e(TAG, "录音失败");
        }
    }

    //播放文件
    public void PlayRecord() {
        if(file == null){
            return;
        }
        int musicLength = (int) (file.length() / 2);
        byte[] music = new byte[musicLength];
        try {
            InputStream is = new FileInputStream(file);
            //设置播放参数
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    48000, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    musicLength * 2,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
            //从文件中读取并写入audioTrack
            while(is.read(music)!= -1)
            {
                audioTrack.write(music, 0, musicLength);
            }
            is.close();
            audioTrack.stop();
        } catch (Throwable t) {
            Log.e(TAG, "播放失败");
        }
    }

    //删除文件
    private void deleFile() {
        if(file == null){
            return;
        }
        file.delete();
        printLog("文件删除成功");
    }

    //转换文件
    private void transFile() {
        PcmToWavUtil pcmToWav = new PcmToWavUtil();
        int status = pcmToWav.pcmToWav(filePath + "/myaudio.pcm", filePath + "/myaudio.wav");
        if(status == 0) printLog("转换失败");
        else printLog("转换成功");
    }
    /*private short[] test_Noise(short[] buf, int nb_sample)
    {
        int i = 0;
        for (i = 0; i < nb_sample; i++)
        {
            buf[i] >>= 2;
        }
        return buf;
    }*/

}
