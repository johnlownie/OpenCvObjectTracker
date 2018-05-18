package ca.jetsphere.opencvobjecttracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ToggleButton;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private View mImgGroup, mHsvGroup;

    JavaCameraView javaCameraView;
    Mat mRgba, imgHSV, imgThreshold;
    int iImgType = 0;

    RangeSeekBar rsbHue, rsbSaturation,  rsbValue;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS : javaCameraView.enableView(); break;
                default : super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        tryStartCamera();

        rsbHue = findViewById(R.id.rsbHue);
        rsbHue.setRangeValues(0, 255);
        rsbHue.setSelectedMinValue(40);
        rsbHue.setSelectedMaxValue(80);

        rsbSaturation = findViewById(R.id.rsbSaturation);
        rsbSaturation.setRangeValues(0, 255);
        rsbSaturation.setSelectedMinValue(100);
        rsbSaturation.setSelectedMaxValue(255);

        rsbValue = findViewById(R.id.rsbValue);
        rsbValue.setRangeValues(0, 255);
        rsbValue.setSelectedMinValue(30);
        rsbValue.setSelectedMaxValue(255);

        mImgGroup = findViewById(R.id.imgGroup);
        mHsvGroup = findViewById(R.id.hsvGroup);

        final ToggleButton btnRawImage = findViewById(R.id.btnRawImage);
        final ToggleButton btnHSVImage = findViewById(R.id.btnHSVImage);
        final ToggleButton btnThresholdImage = findViewById(R.id.btnThresholdImage);

        btnRawImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRawImage.setChecked(true);
                btnHSVImage.setChecked(false);
                btnThresholdImage.setChecked(false);
                iImgType = 0;
            }
        });

        btnHSVImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRawImage.setChecked(false);
                btnHSVImage.setChecked(true);
                btnThresholdImage.setChecked(false);
                iImgType = 1;
            }
        });

        btnThresholdImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRawImage.setChecked(false);
                btnHSVImage.setChecked(false);
                btnThresholdImage.setChecked(true);

                mImgGroup.setVisibility(View.GONE);
                mHsvGroup.setVisibility(View.VISIBLE);
                rsbHue.setVisibility(View.VISIBLE);

                iImgType = 2;
            }
        });

        final ToggleButton btnHue = findViewById(R.id.btnHue);
        final ToggleButton btnSaturation = findViewById(R.id.btnSaturation);
        final ToggleButton btnValue = findViewById(R.id.btnValue);
        btnHue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rsbHue.setVisibility(View.VISIBLE);
                rsbSaturation.setVisibility(View.INVISIBLE);
                rsbValue.setVisibility(View.INVISIBLE);

                btnHue.setChecked(true);
                btnSaturation.setChecked(false);
                btnValue.setChecked(false);
            }
        });

        btnSaturation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rsbHue.setVisibility(View.INVISIBLE);
                rsbSaturation.setVisibility(View.VISIBLE);
                rsbValue.setVisibility(View.INVISIBLE);

                btnHue.setChecked(false);
                btnSaturation.setChecked(true);
                btnValue.setChecked(false);
            }
        });

        btnValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rsbHue.setVisibility(View.INVISIBLE);
                rsbSaturation.setVisibility(View.INVISIBLE);
                rsbValue.setVisibility(View.VISIBLE);

                btnHue.setChecked(false);
                btnSaturation.setChecked(false);
                btnValue.setChecked(true);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        toggle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCv loaded successfully");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "OpenCv failed to load");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        imgHSV = new Mat(height, width, CvType.CV_8UC4);
        imgThreshold = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Imgproc.cvtColor(mRgba, imgHSV, Imgproc.COLOR_BGR2HSV);
        Core.inRange(imgHSV, new Scalar(((int) rsbHue.getSelectedMinValue()), ((int) rsbSaturation.getSelectedMinValue()), ((int)rsbValue.getSelectedMinValue())), new Scalar(((int) rsbHue.getSelectedMaxValue()), ((int) rsbSaturation.getSelectedMaxValue()), ((int) rsbValue.getSelectedMaxValue())), imgThreshold);
        morphOps(imgThreshold);
        trackFilteredObject(mRgba, imgThreshold);

        switch (iImgType) {
            case 0 : return mRgba;
            case 1 : return imgHSV;
            case 2 : return imgThreshold;
            default: return mRgba;
        }
    }

    /**
     *
     */
    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(parent.getActivity(),
                                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

    /**
     *
     */
    private void morphOps(Mat threshold) {
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));

        Imgproc.erode(threshold, threshold, erodeElement);
        Imgproc.erode(threshold, threshold, erodeElement);

        Imgproc.dilate(threshold, threshold, dilateElement);
        Imgproc.dilate(threshold, threshold, dilateElement);
    }

    /**
     *
     */
    private void trackFilteredObject(Mat cameraFeed, Mat threshold) {
        Mat temp = new Mat();
        threshold.copyTo(temp);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(temp, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.size() < 50) {
            for (MatOfPoint mop : contours) {
                Moments moment = Imgproc.moments(mop);

                if (moment.m00 < 20 * 20) continue;

                int x = ((int) (moment.m10/moment.m00));
                int y = ((int) (moment.m01/moment.m00));

                Imgproc.putText(cameraFeed, "Tracking Object", new Point(0, 50), 2, 1, new Scalar(0, 255, 0), 2);
                drawObject(x, y, cameraFeed);
            }
        } else Imgproc.putText(cameraFeed, "Too much noise - adjust filter", new Point(0, 50), 1, 2, new Scalar(0, 0, 255), 2);
    }

    /**
     *
     */
    private void drawObject(int x, int y, Mat cameraFeed) {
        Imgproc.circle(cameraFeed, new Point(x, y), 20, new Scalar(0, 255, 0), 2);

        if (y - 25 > 0) Imgproc.line(cameraFeed, new Point(x, y), new Point(x, y - 25), new Scalar(0, 255, 0), 2);
        else Imgproc.line(cameraFeed, new Point(x,y), new Point(x, 0), new Scalar(0, 255, 0), 2);

        Imgproc.putText(cameraFeed, x + ", " + y, new Point(x, y + 30), 1, 1, new Scalar(0, 255, 0), 2);
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(this.getFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void tryStartCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        javaCameraView = findViewById(R.id.java_camera_view);
        javaCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        javaCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
    }

    private void toggle() {
        if (mImgGroup.getVisibility() == View.VISIBLE) {
            mImgGroup.setVisibility(View.GONE);
        } else if (mHsvGroup.getVisibility() == View.VISIBLE) {
            mHsvGroup.setVisibility(View.GONE);
            mImgGroup.setVisibility(View.VISIBLE);
            rsbHue.setVisibility(View.INVISIBLE);
            rsbSaturation.setVisibility(View.INVISIBLE);
            rsbValue.setVisibility(View.INVISIBLE);
        } else {
            mImgGroup.setVisibility(View.VISIBLE);
        }
    }
}
