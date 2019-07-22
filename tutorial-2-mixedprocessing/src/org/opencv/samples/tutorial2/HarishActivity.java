package org.opencv.samples.tutorial2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HarishActivity extends Activity {
    static {
        System.loadLibrary("mixed_sample");
    }

    class MyColor {
        MyColor(int red, int blue, int green) {
            this.red = red;
            this.blue = blue;
            this.green = green;
        }

        int red, green, blue;

        @Override
        public int hashCode() {
            return ("" + red + "," + blue + "," + green).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            MyColor myColor = (MyColor) obj;

            return (red == myColor.red && blue == myColor.blue && green == myColor.green);
        }
    }

    private ImageView img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11, img12;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadOpenCV();
        initViews();
        int pixel;
        if (true) {
            Bitmap bitmap = getBitmap();
            showBitMapForDebugging(bitmap, img1);
            HashMap<MyColor, Integer> map = new HashMap<>();


            byte[] bitmapData = getImageBytes();
            Mat mat = Imgcodecs.imdecode(new MatOfByte(bitmapData), Imgcodecs.IMREAD_UNCHANGED);

            Mat result2 = step1CvtColor(mat);
            showBitMapForDebugging(result2, img2);

            Mat result8 = step7Threshold(result2);
            showBitMapForDebugging(result8, img3);

            bitmap=getBitMap(result8);
            for (int i = bitmap.getHeight() - 1; i >= 0; i--) {
                pixel = bitmap.getPixel(0, i);
                map.put(new MyColor(Color.red(pixel), Color.blue(pixel), Color.green(pixel)), pixel);
            }
            Log.d("done", "done");
            return;
        }

        byte[] bitmapData = getImageBytes();
        Mat mat = Imgcodecs.imdecode(new MatOfByte(bitmapData), Imgcodecs.IMREAD_UNCHANGED);


        showBitMapForDebugging(mat, img1);

        Mat rectKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13, 5));
        Mat sqKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(34, 34));

        Mat result2 = step1CvtColor(mat);
        showBitMapForDebugging(result2, img2);

        Mat result3 = step2GaussianBlur(result2);
        showBitMapForDebugging(result3, img3);

        Mat result4 = step3MorphologyExBlackHat(result3, rectKernel);
        showBitMapForDebugging(result4, img4);

        Mat result5 = step4Sobel(result4);
        showBitMapForDebugging(result5, img5);


        Mat result6 = step5AbsoluteGradX(result5);
        showBitMapForDebugging(result6, img6);

        Mat result7 = step6MorphologyExMorphClose(result6, rectKernel);
        showBitMapForDebugging(result7, img7);


        Mat result8 = step7Threshold(result7);
        showBitMapForDebugging(result8, img8);

        Mat result9 = step8MorphologyMorphCloseSqKernel(result8, sqKernel);
        showBitMapForDebugging(result9, img9);

        Mat result10 = step9Ercode(result9);
        showBitMapForDebugging(result10, img10);


        Mat hierarchy = new Mat();
        List<MatOfPoint> cnts = step10Contours(result10, hierarchy);
        Mat result11 = result10;
        showBitMapForDebugging(result11, img11);
        LinearLayout linearLayout = findViewById(R.id.linear);
        Mat roi = new Mat();
        // loop over the contours
        for (int i = 0; i < cnts.size(); i++) {
            org.opencv.core.Rect rect = Imgproc.boundingRect(cnts.get(i));
            float ar = rect.width / (float) (rect.height);
            float crWidth = rect.width / (float) (result2.size().height);

            // check to see if the aspect ratio and coverage width are within
            // acceptable criteria
            //if (ar > 5 && crWidth > 0.75) {
            // pad the bounding box since we applied erosions and now need
            // to re – grow it
            int pX = (int) ((rect.x + rect.width) * 0.03);
            int pY = (int) ((rect.y + rect.height) * 0.03);
            int x = rect.x - pX;
            int y = rect.y - pY;
            int w = rect.width + (pX * 2);
            int h = rect.height + (pY * 2);

            // extract the ROI from the image and draw a bounding box
            // surrounding the MRZ
            try {
                mat.submat(new Range(y, y + h), new Range(x, x + w)).copyTo(roi);
//                    image(new Range(y, y + h), new Range(x, x + w)).copyTo(roi);

                Imgproc.rectangle(result11, new Point(x, y), new Point(x + w, y + h), new Scalar(0, 255, 0), 2, Imgproc.LINE_8, 0);
                ImageView imageView = new ImageView(HarishActivity.this);
                imageView.setLayoutParams(img1.getLayoutParams());
                linearLayout.addView(imageView);
                showBitMapForDebugging(result11, imageView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            break;
            //}
        }
    }

    private List<MatOfPoint> step10Contours(Mat input, Mat hierarchy) {
        // find contours in the thresholded image and sort them by their
        // sizes
        List<MatOfPoint> cnts = new ArrayList<MatOfPoint>();
        Imgproc.findContours(input, cnts, hierarchy, Imgproc.CV_RETR_EXTERNAL, Imgproc.CV_CHAIN_APPROX_SIMPLE);
        return cnts;
    }

    private Mat step9Ercode(Mat input) {
        Mat output = new Mat();
        Mat emptyMat = new Mat();
        Imgproc.erode(input, output, emptyMat, new Point(-1, -1), 4);
        return output;
    }

    private Mat step8MorphologyMorphCloseSqKernel(Mat input, Mat sqKernel) {
        Mat output = new Mat();
        Imgproc.morphologyEx(input, output, Imgproc.MORPH_CLOSE, sqKernel);
        return output;
    }

    private Mat step7Threshold(Mat input) {
        double threshValue = 0;
        double maxValue = 255;
        Mat output = new Mat();
        Imgproc.threshold(input, output, threshValue, maxValue, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        return output;
    }

    private Mat step6MorphologyExMorphClose(Mat input, Mat rectKernel) {
        Mat output = new Mat();
        Imgproc.morphologyEx(input, output, Imgproc.MORPH_CLOSE, rectKernel);
        return output;
    }

    private Mat step5AbsoluteGradX(Mat input) {
        Mat output = new Mat();
        Core.convertScaleAbs(input, output);
        return output;
    }

    private Mat step4Sobel(Mat input) {
        int scale = 1;
        int delta = 0;
        Mat output = new Mat();
        Imgproc.Sobel(input, output, CvType.CV_8UC1, 1, 0, -1, scale, delta, Core.BORDER_DEFAULT);
        return output;
    }

    private Mat step3MorphologyExBlackHat(Mat input, Mat rectKernel) {
        Mat output = new Mat();
        Imgproc.morphologyEx(input, output, Imgproc.MORPH_BLACKHAT, rectKernel);
        return output;
    }

    private Mat step2GaussianBlur(Mat input) {
        Mat output = new Mat();
        Imgproc.GaussianBlur(input, output, new Size(3, 3), 0);
        return output;
    }

    private Mat step1CvtColor(Mat mat) {
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    private void initViews() {
        img1 = findViewById(R.id.img1);
        img2 = findViewById(R.id.img2);
        img3 = findViewById(R.id.img3);
        img4 = findViewById(R.id.img4);
        img5 = findViewById(R.id.img5);
        img6 = findViewById(R.id.img6);
        img7 = findViewById(R.id.img7);
        img8 = findViewById(R.id.img8);
        img9 = findViewById(R.id.img9);
        img10 = findViewById(R.id.img10);
        img11 = findViewById(R.id.img11);
        img12 = findViewById(R.id.img12);

    }

    private Bitmap getBitmap() {
        File file = new File(getExternalFilesDir(null), "image45.jpeg");

        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    private byte[] getImageBytes() {

        Bitmap bitmap = getBitmap();
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        return blob.toByteArray();
    }

    private void loadOpenCV() {
        if (OpenCVLoader.initDebug()) {
            Toast.makeText(this, "loaded", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "not loaded", Toast.LENGTH_LONG).show();
        }
    }

    private void showBitMapForDebugging(Bitmap bmp, ImageView img) {
        Log.d("Ank", "" + bmp);
        img.setImageBitmap(bmp);
    }

    private void showBitMapForDebugging(Mat original, ImageView img) {
        Bitmap bmp = getBitMap(original);
        Log.d("Ank", "" + bmp);
        img.setImageBitmap(bmp);
    }

    public static Bitmap getBitMap(Mat srcMat) {
        Bitmap bitmap = Bitmap.createBitmap(srcMat.cols(), srcMat.rows(), Bitmap.Config.ARGB_8888);
        try {
            Utils.matToBitmap(srcMat, bitmap);
        } catch (Exception e) {

        }
        return bitmap;
    }
}
