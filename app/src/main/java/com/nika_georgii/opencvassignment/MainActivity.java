package com.nika_georgii.opencvassignment;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private CascadeClassifier faceClassifier;
    private CascadeClassifier noseClassifier;
    private Size faceClassifierMinSize = new Size(100, 100);
    private Size faceClassifierMaxSize = new Size(600, 600);
    private Size noseClassifierMinSize = new Size(50, 50);
    private Size noseClassifierMaxSize = new Size(200, 200);

    private Mat result;
    private Mat gray;
    private MatOfRect faces;
    private MatOfRect noses;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                    faceClassifier = new CascadeClassifier();
                    faceClassifier.load(initAssetFile("haarcascade_frontalface_default.xml"));

                    noseClassifier = new CascadeClassifier();
                    noseClassifier.load(initAssetFile("haarcascade_mcs_nose.xml"));
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // before opening the CameraBridge, we need the Camera Permission on newer Android versions

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0x123);
        } else {
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        // Had some performance and memory issues and decided to initialiye the values here
        result = new Mat();
        gray = new Mat();
        faces = new MatOfRect();
        noses = new MatOfRect();
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();

        rgba = rotateMatCW(rgba);

        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_BGR2GRAY);

        if (faceClassifier != null) {
            faceClassifier.detectMultiScale(gray, faces, 1.3, 1, 1, faceClassifierMinSize, faceClassifierMaxSize);
        }

        for (Rect face : faces.toArray()) {

//            Imgproc.rectangle(
//                    rgba,
//                    new Point(face.x, face.y),
//                    new Point(face.x + face.width, face.y + face.height),
//                    new Scalar(255, 255, 0),
//                    5
//            );

            Mat faceArea = gray.submat(new Rect(face.x, face.y, face.width, face.height));

            noseClassifier.detectMultiScale(faceArea, noses, 1.3, 1, 1, noseClassifierMinSize, noseClassifierMaxSize);

            for (Rect nose : noses.toArray()) {

                int noseX = nose.x + face.x;
                int noseY = nose.y + face.y;

                Imgproc.circle(
                        rgba,
                        new Point(noseX + nose.width / 2, noseY + nose.height / 2),
                        nose.width / 2,
                        new Scalar(255, 0, 0),
                        -1
                );
            }
        }

        return rgba;
    }


    public String initAssetFile(String filename) {
        File file = new File(getFilesDir(), filename);
        if (!file.exists()) try {
            InputStream is = getAssets().open(filename);
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "prepared local file: " + filename);
        return file.getAbsolutePath();
    }

    // Thanks to https://github.com/mmbuw-courses/mis-2019-exercise-3b-opencv/pull/1
    Mat rotateMatCW(Mat src) {
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(src.cols() / 2, src.rows() / 2), 270, 1);

        //Rotating the given image
        Imgproc.warpAffine(src, result, rotationMatrix, new Size(src.cols(), src.rows()));
        return result;
    }
}
