package com.patechltd.salexfypos.scanner;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.patechltd.salexfypos.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerView extends FrameLayout {

    public interface OnScanListener {
        void onDecoded(String text, String format);
    }

    private PreviewView previewView;
    private ImageButton torchButton;
    private TextView statusView;
    private View frameOverlay;
    private Camera camera;
    private BarcodeAnalyzer analyzer;
    private ExecutorService analysisExecutor;
    private boolean bound = false;
    private boolean torchOn = false;
    private boolean hidePreview = false;
    private OnScanListener listener;
    private LifecycleOwner lifecycleOwner;
    private boolean suspend = false;
    private boolean hasFlash;

    public ScannerView(@NonNull Context context) {
        this(context, null);
    }

    public ScannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        buildView();
    }

    private void buildView() {
        setClipToOutline(true);

        previewView = new PreviewView(getContext());
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        addView(previewView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        frameOverlay = new View(getContext());
        frameOverlay.setBackground(new ScanFrameDrawable(getContext()));
        frameOverlay.setClickable(true);
        addView(frameOverlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        statusView = new TextView(getContext());
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(13);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(24), dp(12), dp(24), dp(12));
        statusView.setText("Point the camera at a barcode or QR code");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        bg.setColor(Color.argb(150, 0, 0, 0));
        statusView.setBackground(bg);
        LayoutParams statusParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        statusParams.bottomMargin = dp(16);
        addView(statusView, statusParams);

        torchButton = new ImageButton(getContext());
        torchButton.setImageResource(R.drawable.ic_flash_on);
        torchButton.setBackgroundResource(R.drawable.bg_torch_button);
        torchButton.setContentDescription("Flashlight");
        torchButton.setColorFilter(Color.WHITE);
        torchButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        torchButton.setOnClickListener(v -> toggleTorch());
        LayoutParams torchParams = new LayoutParams(dp(48), dp(48));
        torchParams.gravity = Gravity.BOTTOM | Gravity.END;
        torchParams.bottomMargin = dp(16);
        torchParams.rightMargin = dp(16);
        addView(torchButton, torchParams);
        updateTorchIcon();
    }

    public void setOnScanListener(OnScanListener listener) {
        this.listener = listener;
    }

    public void start(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
        analysisExecutor = Executors.newSingleThreadExecutor();
        analyzer = new BarcodeAnalyzer((text, format) -> {
            if (listener != null && !suspend) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onDecoded(text, format));
            }
        }, 250, ScannerEngines.create(getContext()));
        bindUseCases();
    }

    private void bindUseCases() {
        if (lifecycleOwner == null || analysisExecutor == null || bound) return;
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(getContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                if (!provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    setStatus("No back camera found");
                    return;
                }
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetResolution(new android.util.Size(1280, 720))
                        .build();
                analysis.setAnalyzer(analysisExecutor, analyzer);

                provider.unbindAll();
                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, analysis);
                hasFlash = camera.getCameraInfo().hasFlashUnit();
                torchButton.setVisibility(hasFlash ? VISIBLE : GONE);
                bound = true;
                post(() -> setPreviewVisible(!hidePreview));
            } catch (Exception e) {
                setStatus("Camera error");
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    public void stop() {
        bound = false;
        if (analysisExecutor != null) {
            analysisExecutor.shutdown();
            analysisExecutor = null;
        }
        if (analyzer != null) analyzer.setActive(false);
    }

    public void toggleTorch() {
        if (camera == null || !hasFlash) return;
        try {
            boolean enable = !torchOn;
            camera.getCameraControl().enableTorch(enable);
            torchOn = enable;
            updateTorchIcon();
        } catch (Exception ignored) {
        }
    }

    public boolean isTorchOn() {
        return torchOn;
    }

    public void setTorch(boolean on) {
        if (camera == null || !hasFlash || torchOn == on) return;
        try {
            camera.getCameraControl().enableTorch(on);
            torchOn = on;
            updateTorchIcon();
        } catch (Exception ignored) {
        }
    }

    public boolean hasFlash() {
        return hasFlash;
    }

    public void setSuspend(boolean suspend) {
        this.suspend = suspend;
        if (analyzer != null) analyzer.setActive(!suspend);
        if (suspend) {
            setStatus("Scanning paused");
        } else {
            if (analyzer != null) analyzer.resetLock();
            setStatus("Point the camera at a barcode or QR code");
        }
    }

    public boolean isSuspended() {
        return suspend;
    }

    public void setPreviewVisible(boolean visible) {
        hidePreview = !visible;
        if (previewView != null) previewView.setVisibility(visible ? VISIBLE : INVISIBLE);
        if (visible) {
            frameOverlay.setVisibility(VISIBLE);
            statusView.setVisibility(VISIBLE);
        } else {
            frameOverlay.setVisibility(GONE);
            statusView.setVisibility(GONE);
        }
    }

    public void setStatus(String message) {
        if (statusView != null) statusView.setText(message);
    }

    private void updateTorchIcon() {
        if (torchButton == null) return;
        torchButton.setImageResource(torchOn ? R.drawable.ic_flash_off : R.drawable.ic_flash_on);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
