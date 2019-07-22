package org.opencv.samples.tutorial2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

import static android.content.ContentValues.TAG;
import static org.opencv.samples.tutorial2.MainActivity.detectPassportZone;


public class CustomSurfaceView extends SurfaceView implements Camera.PreviewCallback, SurfaceHolder.Callback {
    private Disposable mDisposable;
    private Bitmap cropBitmap;
    private TessBaseAPI tessBaseApi;
    private Camera camera;

    public CustomSurfaceView(Context context) {
        super(context);
    }

    public CustomSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CustomSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (mDisposable == null || mDisposable.isDisposed()) {
            camera.setPreviewCallback(null);
            Mat orig = byteToMat(bytes);
            if (orig != null) {
                mDisposable = processMRZObservable(orig)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<Bitmap>() {
                            @Override
                            public void onNext(Bitmap value) {
                                cropBitmap = value;
                                //setImagePreview(value);
                            }

                            @Override
                            public void onError(Throwable e) {
                                mDisposable.dispose();
                            }

                            @Override
                            public void onComplete() {
                                //if (getRecord() != null) {
                                    //Storage.saveBitmapResult(PassportCameraActivity.this, "Passport", cropBitmap);
                                  //  goBack(mPassportCameraPresenter.getRecord());
                                //}
                                saveImage(cropBitmap);
                                mDisposable.dispose();
                            }
                        });
            }
        }
    }
    private void saveImage(Bitmap finalBitmap) {

        File myDir = new File(getContext().getExternalFilesDir(null) , "saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Bitmap cropCameraViewFrame(Mat cvCameraViewFrame){
        byte[] cropResult = detectPassportZone(cvCameraViewFrame.getNativeObjAddr());
        if (cropResult != null) {
            int offset = 0;
            return BitmapFactory.decodeByteArray(cropResult, offset, cropResult.length);
        }
        return null;
    }
    private String extractText(Bitmap bitmap) {
        try {
            tessBaseApi = new TessBaseAPI();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if (tessBaseApi == null) {
                Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.");
            }
        }
        //tessBaseApi.init(DATA_PATH, lang);
        Log.d(TAG, "Training file loaded");
        tessBaseApi.setImage(bitmap);
        String extractedText = "empty result";
        try {
            extractedText = tessBaseApi.getUTF8Text();
        } catch (Exception e) {
            Log.e(TAG, "Error in recognizing text.");
        }
        tessBaseApi.end();
        return extractedText;
    }
    public String getOCR(Bitmap bitmap){
        if(bitmap != null) {
            return extractText(bitmap);
        }
        return null;
    }
    public Observable<Bitmap> processMRZObservable(final Mat cvCameraViewFrame) {
        return Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(ObservableEmitter observableEmitter) throws Exception {
                Bitmap bitmap;
                try {
                    bitmap = cropCameraViewFrame(cvCameraViewFrame);
                    if (bitmap != null) {
//show Preview crop
                        observableEmitter.onNext(bitmap);
                        String result = getOCR(bitmap);
                        try {
                            //record = getMrz(result);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    observableEmitter.onError(e);
                    Log.e(TAG, e.getMessage());
                }
                observableEmitter.onComplete();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            this.camera.setPreviewCallback(this);
            this.camera.setPreviewDisplay(surfaceHolder);
            this.camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.camera.stopPreview();
        this.camera.release();
    }

    private Mat byteToMat(byte[] data) {
        Camera.Parameters parameters = camera.getParameters();
        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), 200, 200, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, 200, 200), 100, out);
        byte[] bytes = out.toByteArray();
        final Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bmp != null) {
            Mat orig = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC1);
//Bitmap myBitmap32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp, orig);
            Imgproc.resize(orig, orig, new Size(200, 200));
            return orig;
        }
        return null;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Camera getCamera() {
        return camera;
    }
}
