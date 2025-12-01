package com.example.calculatrice;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.io.InputStream;

public class ScanActivity extends AppCompatActivity {
    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private Button btnGallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        btnGallery = findViewById(R.id.btnGallery);

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1001);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            decodeUri(imageUri);
        }
    }

    private void decodeUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Handle transparency: Draw on white background
            Bitmap bitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            canvas.drawColor(android.graphics.Color.WHITE);
            canvas.drawBitmap(originalBitmap, 0, 0, null);

            // 2. Try decoding original bitmap first (best quality)
            Result result = tryDecodeWithRotations(bitmap);

            // 3. If failed, try scaling down (if large) to help with focus/noise or different binarization
            if (result == null && (bitmap.getWidth() > 1024 || bitmap.getHeight() > 1024)) {
                int maxSize = 1024;
                float scale = Math.min(((float) maxSize) / bitmap.getWidth(), ((float) maxSize) / bitmap.getHeight());
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), false);
                result = tryDecodeWithRotations(scaledBitmap);
            }

            if (result != null) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("SCAN_RESULT", result.getText());
                setResult(RESULT_OK, returnIntent);
                NotificationHelper.showNotification(this, "Scan Successful", "QR Code detected: " + result.getText());
                finish();
            } else {
                Toast.makeText(this, "Could not detect QR code. Please ensure the image is clear and contains a valid QR code.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error scanning image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Result tryDecodeWithRotations(Bitmap bitmap) {
        int[] rotations = {0, 90, 180, 270};
        for (int rotation : rotations) {
            Bitmap rotatedBitmap = rotateBitmap(bitmap, rotation);
            Result result = attemptDecode(rotatedBitmap);
            if (result != null) return result;
        }
        return null;
    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        if (angle == 0) return source;
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Result attemptDecode(Bitmap bitmap) {
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        
        java.util.Map<com.google.zxing.DecodeHintType, Object> hints = new java.util.EnumMap<>(com.google.zxing.DecodeHintType.class);
        hints.put(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE);
        // Removed POSSIBLE_FORMATS restriction to be more permissive

        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(hints);

        try {
            return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
        } catch (Exception e) {
            try {
                return reader.decodeWithState(new BinaryBitmap(new com.google.zxing.common.GlobalHistogramBinarizer(source)));
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
