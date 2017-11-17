package com.example.yjing.vlc_trans;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yjing on 2017/11/9.
 */

public class Transmitter extends Activity{

    Activity activity;
    static String filePath;
    String encode_info;
    String final_encode;
    int ind;

    Point size;
    int grid_size_x;
    int grid_size_y;
    Bitmap icon_white;
    Bitmap bmFrame;
    ImageView background_image;

    ImageView[][] commViews;

    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    int grey;
    private static String TAG = "Transmitter";

    private static int timeInt = 500;

    double[][] gauss2d_matrix;
    double[][] alpha_level_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;

        Bundle extras = getIntent().getExtras();

        //extract intent message from Main Activity
        if (extras != null) {
            filePath = extras.getString("filepath");
            encode_info = extras.getString("encode_info");
        }

        final_encode = "";
        if (encode_info.equals("")) {
            encode_info = "10110100";
        }
        ind = 0;
        encode();

        setContentView(R.layout.media_metadata);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.mainLayout);

        Display display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        grid_size_x = width / 6;
        grid_size_y = height / 6;
        grey = 0;

        commViews = new ImageView[6][6];
        gauss2d_matrix = new double[1][1];
        alpha_level_image = new double[6][6];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                alpha_level_image[i][j] = 1;
            }
        }
        getGaussian2d();
        getAlphaLevel();

        //Get black background for the foreground imageview
        Bitmap icon = decodeSampledBitmapFromResource(this.getResources(), R.drawable.single_color_img, grid_size_x, grid_size_y);
        icon = Bitmap.createScaledBitmap(icon, grid_size_x, grid_size_y, true);
        icon_white = decodeSampledBitmapFromResource(this.getResources(), R.drawable.single_color_img_white, grid_size_x, grid_size_y);
        icon_white = Bitmap.createScaledBitmap(icon_white, grid_size_x, grid_size_y, true);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        bmFrame = BitmapFactory.decodeFile(filePath);
        background_image = new ImageView(this);
        background_image.setImageBitmap(bmFrame);
        background_image.setLayoutParams(params);
        layout.addView(background_image);


        //add communication layer
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                commViews[i][j] = new ImageView(this);
                commViews[i][j].setId(1 + i * 6 + j);
                commViews[i][j].setImageAlpha(0);
                params = new RelativeLayout.LayoutParams(width / 6, height / 6);
                if(j == 0) {
                    if(i == 0) {
                        //动态布局
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        params.addRule((RelativeLayout.ALIGN_PARENT_TOP));
                        commViews[i][j].setLayoutParams(params);
                    } else {
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        params.addRule(RelativeLayout.RIGHT_OF, commViews[i - 1][j].getId());
                        commViews[i][j].setLayoutParams(params);
                    }
                } else {
                    params.addRule(RelativeLayout.BELOW, commViews[i][j - 1].getId());
                    if(i == 0) {
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    } else {
                        params.addRule(RelativeLayout.RIGHT_OF, commViews[i - 1][j].getId());
                    }
                    commViews[i][j].setLayoutParams(params);
                }

                commViews[i][j].setImageBitmap(icon);
                commViews[i][j].bringToFront();
                layout.addView(commViews[i][j]);
            }
        }

        startTimer();
    }


    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    protected void onResume() {
        super.onResume();

        //onResume we start our timer so it can start when the app comes from the background
    }

    private void encode() {
        final_encode += "11";
        /*
        String tmp = "";
        //偶数倍 奇数添1
        int len = encode_info.length();
        if(len % 2 == 1) encode_info += "1";

        for(int i = 0; i < encode_info.length(); i+=2) {
            tmp = encode_info.substring(i, i + 2);
            if(tmp.equals("00")) {
                final_encode += "01";
            }
            else if(tmp.equals("01")) {
                final_encode += "001";
            }
            else if(tmp.equals("11")) {
                final_encode += "0001";
            }
            else {
                final_encode += "00001";
            }
        }
        */
        for(int i = 0; i < encode_info.length(); i++) {
            if(encode_info.charAt(i) == '1') {
                final_encode += "01";
            }
            else {
                final_encode += "001";
            }
        }

        //final_encode += "00";
        Log.d(TAG, "final_code: " + final_encode);
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 1000ms the TimerTask will run every 16ms
        timer.schedule(timerTask, 2000, timeInt);
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(ind >= final_encode.length()) return;
                        if(final_encode.charAt(ind) == '1') grey = 1;
                        else grey = 0;

                        for (int i = 0; i < 6; i++) {
                            for (int j = 0; j < 6; j++) {
                                if(i == 0 || i == 5)
                                    commViews[i][j].setImageAlpha((int)(grey*20*alpha_level_image[i][j]));
                                else
                                    commViews[i][j].setImageAlpha(grey*10);
                            }
                        }

                        ind++;
                    }
                });
            }
        };
    }

    //Gaussian filter
    public void getGaussian2d() {
        int centre_x = (int) Math.ceil(1.0 / 2);
        int centre_y = (int) Math.ceil(1.0 / 2);
        int normalized_index = 3;
        double exponent;
        int i, j;
        for (i = 0; i < 1; i++) {
            for (j = 0; j < 1; j++) {
                exponent = (Math.pow((i + 1 - centre_x) * 1.0 / 1 * normalized_index, 2.0) + Math.pow((j + 1 - centre_y) * 1.0 / 1 * normalized_index, 2.0)) / 2;
                gauss2d_matrix[i][j] = Math.exp(-exponent);
            }
        }
    }

    public void getAlphaLevel(){
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int i1 = 0; i1 < 1; i1++) {
                    for (int j1 = 0; j1 < 1; j1++) {
                        alpha_level_image[i + i1][j + j1] = gauss2d_matrix[i1][j1];
                    }
                }
            }
        }
    }

    public void stoptimertask(View v) {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    //响应后退键
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}
