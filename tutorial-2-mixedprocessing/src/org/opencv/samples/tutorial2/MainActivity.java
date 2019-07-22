package org.opencv.samples.tutorial2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private Camera camera;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("mixed_sample");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CustomSurfaceView customSurfaceView = findViewById(R.id.surface_view);

        // Example of a call to a native method
        //tv.setText(stringFromJNI());
        if(OpenCVLoader.initDebug()){
            Toast.makeText(this,"loaded", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"not loaded", Toast.LENGTH_LONG).show();
        }
        camera = checkDeviceCamera();
        customSurfaceView.setCamera(camera);
        customSurfaceView.getHolder().addCallback(customSurfaceView);
    }
    private Camera checkDeviceCamera(){
        Camera mCamera = null;
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mCamera;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native static byte[] detectPassportZone(long image);
    private Bitmap cropCameraViewFrame(Mat cvCameraViewFrame){
        byte[] cropResult = detectPassportZone(cvCameraViewFrame.getNativeObjAddr());
        if (cropResult != null) {
            int offset = 0;
            return BitmapFactory.decodeByteArray(cropResult, offset, cropResult.length);
        }
        return null;
    }



}
