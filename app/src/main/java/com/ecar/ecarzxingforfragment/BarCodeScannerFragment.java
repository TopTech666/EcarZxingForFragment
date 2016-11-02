package com.ecar.ecarzxingforfragment;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.AmbientLightManager;
import com.google.zxing.client.android.InactivityTimer;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.qcode.BarCodeScannerHandler;
import com.google.zxing.client.android.qcode.IBarCode;
import com.google.zxing.client.android.qcode.IConstants;

import java.io.IOException;

import static android.content.ContentValues.TAG;


public class BarCodeScannerFragment extends Fragment implements SurfaceHolder.Callback, IBarCode {
    private CameraManager cameraManager;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private BarCodeScannerHandler handler;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private AmbientLightManager ambientLightManager;
    private IResultCallback mCallBack;
    private View view;
    private SurfaceHolder surfaceHolder;
//    private ImageView iv_lamp;
    private String code;
    private boolean isGetting = false;//是否在等待判断结果
    private long time; //记录时间

    /**
     * 震动持续时间
     */
    private static final long VIBRATE_DURATION = 200L;


    public interface IResultCallback {
        void result(Result lastResult);
    }


    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        time=System.currentTimeMillis();
        if (view == null) {
            view = inflater.inflate(R.layout.public_capture, null);
            Window window = getActivity().getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            hasSurface = false;
            inactivityTimer = new InactivityTimer(getActivity());
            ambientLightManager = new AmbientLightManager(getActivity());
            SurfaceView surfaceView = (SurfaceView) view.findViewById(R.id.preview_view);
            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(this);
            surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
            viewfinderView = (ViewfinderView) view.findViewById(R.id.viewfinder_view);
//            iv_lamp = (ImageView) view.findViewById(iv_lamp);
//            iv_lamp.setImageDrawable(getResources().getDrawable(R.drawable.count_scancode_ic_light_off));
//            iv_lamp.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (isLited) {
//                        setTorchOff();
//                    } else {
//                        setTorchOn();
//                    }
//                }
//            });
            setCallBack();
            setScanbarAnim();
            startScan();
        } else {
            //缓存的rootView需要判断是否已经被加过parent， 如果有parent需要从parent删除，要不然会发生这个rootview已经有parent的错误。
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        setScanbarAnim();
        return view;
    }


    //设置扫描回调
    private void setCallBack() {
        this.setmCallBack(new IResultCallback() {
            @Override
            public void result(Result lastResult) {
                if (isGetting) {  //如果正在获取结果就不发请求
                    return;
                }
                code = lastResult.toString();
                if (TextUtils.isEmpty(code)) {
                    return;
                }
                Toast.makeText(getActivity(), code+ "所需时间 = "+(System.currentTimeMillis()-time), Toast.LENGTH_SHORT).show();
                time=System.currentTimeMillis();
                isGetting = false;
            }
        });
    }


    public void stopScan(Object obg) {
        if (cameraManager == null) {
            Log.e(TAG, "stopScan: scan already stopped");
            return;
        }
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }

        setTorchOff();
        inactivityTimer.onPause();
        ambientLightManager.stop();
        cameraManager.closeDriver();
        cameraManager = null;

        if (!hasSurface) {
            surfaceHolder.removeCallback(this);
        }
    }


    //设置来回扫描的动画
    private void setScanbarAnim() {
//        // 扫描线
//        ImageView captureScanLine = (ImageView) view.findViewById(R.id.captureScanLine);
//        // 扫描线的动画
//        TranslateAnimation mAnimation = new TranslateAnimation(
//                TranslateAnimation.ABSOLUTE, -0.9f, TranslateAnimation.ABSOLUTE,
//                0f, TranslateAnimation.RELATIVE_TO_PARENT, -0.9f,
//                TranslateAnimation.RELATIVE_TO_PARENT, 0.9f);
//        // 动画持续时间
//        mAnimation.setDuration(1500);
//        // 无限循环
//        mAnimation.setRepeatCount(-1);
//        // 动画速率：匀速
//        mAnimation.setInterpolator(new LinearInterpolator());
//        // 设置动画
//        captureScanLine.setAnimation(mAnimation);
    }

    @Override
    public void onResume() {
        super.onResume();
        startScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScan(null);
    }

    public void startScan() {
        setCallBack();
        if (cameraManager == null) {
            cameraManager = new CameraManager(getActivity());
        }
        setTorchOff();//关闭
        viewfinderView.setCameraManager(cameraManager);

        handler = null;
        if (hasSurface) {
            initCamera(surfaceHolder);
        }

        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

    }


    public void setTorch(boolean state) {
        if (cameraManager != null) {
            try {
                cameraManager.setTorch(state);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    boolean isLited;//是否显示

    public void setTorchOn() {
        isLited = true;
//        iv_lamp.setImageDrawable(getResources().getDrawable(R.drawable.count_scancode_ic_lights));
        setTorch(true);
    }

    public void setTorchOff() {
        isLited = false;
//        iv_lamp.setImageDrawable(getResources().getDrawable(R.drawable.count_scancode_ic_light_off));
        setTorch(false);
    }

    public void setmCallBack(IResultCallback mCallBack) {
        this.mCallBack = mCallBack;
    }


    @Override
    public void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }


    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, IConstants.DECODE_SUCCEDED, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            drawResultPoints(barcode, scaleFactor, rawResult);
            if (mCallBack != null&&!TextUtils.isEmpty(rawResult.toString())) {
                mCallBack.result(rawResult);
            }

            restartPreviewAfterDelay(500L);
        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager == null || cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new BarCodeScannerHandler(this, cameraManager, viewfinderView);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
        }
    }


    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(IConstants.RESTART_PREVIEW, delayMS);
        }
    }


    boolean isShow;

    public void onGone(Object obg) {
        stopScan(null);
        isShow = false;
    }

    public void onShow() {
        if (!isShow) {
            startScan();
            isShow = true;
        }
    }


    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            // 复位
            mediaPlayer.seekTo(0);
        }
    };
}
