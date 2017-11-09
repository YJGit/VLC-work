package com.example.yjing.vlc_trans;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_SELECT_FROM_GALLERY = 1;
    private static final String IMAGE_UNSPECIFIED = "image/*";
    private static final String URI_INSTANCE_STATE_KEY = "saved_uri";

    private ImageView mImageView;
    private Button mBtChange;
    private Uri mImageCaptureUri;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //关联到ui
        mImageView = (ImageView) findViewById(R.id.transmissionImage);
        mBtChange = (Button) findViewById(R.id.btnChangePhoto);

        if (savedInstanceState != null) {
            mImageCaptureUri = savedInstanceState.getParcelable(URI_INSTANCE_STATE_KEY);
        }

        // Load example image from internal storage
        try {
            FileInputStream fis = openFileInput(getString(R.string.input_file_name));
            Bitmap bmap = BitmapFactory.decodeStream(fis);
            mImageView.setImageBitmap(bmap);
            fis.close();
        } catch (IOException e) {
            // Default photo if no photo saved before.
            mImageView.setImageResource(R.drawable.default_image);
        }
    }

    //Callback for Open buttom
    public void onChangePhotoClicked(View v) {
        // changing the content image
        Intent intent = new Intent();
        intent.setType(IMAGE_UNSPECIFIED);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // REQUEST_CODE_SELECT_FROM_GALLERY is an integer tag you
        // defined to identify the activity in onActivityResult()
        // when it returns
        startActivityForResult(intent, REQUEST_CODE_SELECT_FROM_GALLERY);
    }

    public void startTransmission(View v) {
        String toBeEncoded = "110120110120";
        if (filePath == null){
            Toast.makeText(getApplicationContext(), "Please choose image/video.", Toast.LENGTH_LONG).show();
            return;
        }

        //send intent message to image/video class
        Intent intent = new Intent(this, Transmitter.class);
        Bundle b = new Bundle();
        b.putString("filepath", filePath); //input file path
        b.putString("encode_info", toBeEncoded); //encode data
        intent.putExtras(b);
        startActivity(intent);
    }
    // Handle date after activity returns.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case REQUEST_CODE_SELECT_FROM_GALLERY:
                Bitmap yourSelectedImage = null;
                mImageCaptureUri = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(mImageCaptureUri, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                filePath = cursor.getString(columnIndex);
                cursor.close();
                yourSelectedImage = BitmapFactory.decodeFile(filePath);

                mImageView.setImageBitmap(yourSelectedImage);
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the image capture uri before the activity goes into background
        outState.putParcelable(URI_INSTANCE_STATE_KEY, mImageCaptureUri);
    }

}
