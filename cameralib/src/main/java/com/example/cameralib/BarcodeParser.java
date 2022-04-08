package com.example.cameralib;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class BarcodeParser {

    private Context context;
    private ImageAnalysis analysisUseCase = null;
    private BarcodeScanner barcodeScanner;
    private IOnQrScanListener qrScanListener;

    public BarcodeParser(Context context) {
        this.context = context;
        this.qrScanListener = (IOnQrScanListener) context;
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private void processImageProxy(
            BarcodeScanner barcodeScanner,
            ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError") InputImage inputImage =
                InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(@NonNull List<Barcode> barcodes) {
                        BarcodeParser.this.qrScanListener.onQrScanSuccess(barcodes, null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        BarcodeParser.this.qrScanListener.onQrScanFailed(e, null);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                        imageProxy.close();
                    }
                });
    }

    public void BindAnalyseUseCase(ProcessCameraProvider cameraProvider,
                                   int screenAspectRatio, PreviewView previewView,
                                   CameraSelector cameraSelector,
                                   Camera camera) {

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE)
                        .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        if (cameraProvider == null)
            return;

        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        analysisUseCase = new ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();

        // Initialize our background executor
        ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

        analysisUseCase.setAnalyzer(
                cameraExecutor,
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        processImageProxy(barcodeScanner, image);
                    }
                });
        try {
            camera = cameraProvider.bindToLifecycle(
                    (LifecycleOwner) context,
                    cameraSelector,
                    analysisUseCase
            );
        } catch (IllegalStateException | IllegalArgumentException illegalStateException) {
            //Log.e(TAG, illegalStateException.getMessage());
        }
    }

}
