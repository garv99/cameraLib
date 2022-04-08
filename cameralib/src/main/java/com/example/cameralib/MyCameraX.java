package com.example.cameralib;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;


public class MyCameraX {
    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double RATIO_16_9_VALUE = 16.0 / 9.0;
    private Context context;
    private androidx.camera.core.Camera camera = null;
    private ProcessCameraProvider cameraProvider = null;
    private CameraSelector cameraSelector = null;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private Preview previewUseCase = null;
    private PreviewView previewView = null;
    private ListenableFuture<ProcessCameraProvider> processCameraProvider;
    private boolean isFlashLightOn = false;
    private IOnFlashToggle iOnFlashToggle;

    public MyCameraX(Context context, PreviewView previewView) {
        this.context = context;
        this.previewView = previewView;
        this.iOnFlashToggle = (IOnFlashToggle) context;
    }

    public void bindCameraUseCases() {
        bindPreviewUseCase();
        setBarcodeAnalysis();
    }

    public void bindPreviewUseCase() {
        if (cameraProvider == null)
            return;

        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        previewUseCase = new Preview.Builder()
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            camera = cameraProvider.bindToLifecycle(
                    /* lifecycleOwner= */(LifecycleOwner) context,
                    cameraSelector,
                    previewUseCase
            );
        } catch (IllegalStateException | IllegalArgumentException illegalStateException) {
            //Log.e(TAG, illegalStateException.getMessage());
        }
    }

    private int screenAspectRatio() {
        // Get screen metrics used to setup camera for full screen resolution
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return aspectRatio(metrics.widthPixels, metrics.heightPixels);
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = Math.max(width, height) / Math.min(width, height);

        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    public void init() {
        previewView.post(new Runnable() {
            @Override
            public void run() {

                cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
                if (cameraProvider == null) {
                    processCameraProvider = ProcessCameraProvider.getInstance(((Activity) context).getApplication());
                    try {
                        cameraProvider = processCameraProvider.get();

                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                bindCameraUseCases();
            }
        });
    }


    public void onFlashlightToggleClickListener() {
        if (cameraProvider == null | camera == null)
            return;

        if (!cameraProvider.getAvailableCameraInfos().get(0).hasFlashUnit())
            return;

        isFlashLightOn = !isFlashLightOn;
        camera.getCameraControl().enableTorch(isFlashLightOn);
        iOnFlashToggle.flashLightToggle(isFlashLightOn);
    }

    public void unbindAll() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    public void setBarcodeAnalysis() {
        BarcodeParser barcodeParser = new BarcodeParser(context);
        barcodeParser.BindAnalyseUseCase(cameraProvider, screenAspectRatio(), previewView, cameraSelector, camera);
    }
}
