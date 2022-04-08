package com.example.cameralib;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.List;

public interface IOnQrScanListener {

    public void onQrScanSuccess(List<Barcode> barcodeList, BarcodeScanner barcodeScanner);
    public void onQrScanFailed(Exception exception, BarcodeScanner barcodeScanner);
}
