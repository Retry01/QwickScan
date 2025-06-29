package com.example.qwickscan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanQrActivity extends AppCompatActivity {

    private PreviewView cameraPreviewView;
    private TextView resultTextView;
    private TextView resultTitleTextView;
    private ExecutorService cameraExecutor;
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        cameraPreviewView = findViewById(R.id.cameraPreviewView);
        resultTextView = findViewById(R.id.resultTextView);
        resultTitleTextView = findViewById(R.id.resultTitleTextView);

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<androidx.camera.lifecycle.ProcessCameraProvider> cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                androidx.camera.lifecycle.ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] imageData = new byte[buffer.capacity()];
                    buffer.get(imageData);

                    int width = image.getWidth();
                    int height = image.getHeight();

                    Result rawResult = getResult(imageData, width, height);

                    if (rawResult != null) {
                        String result = rawResult.getText();
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            String title = jsonObject.getString("title");
                            String description = jsonObject.getString("description");

                            runOnUiThread(() -> {
                                resultTitleTextView.setText(title);
                                resultTextView.setText(description);
                            });
                        } catch (JSONException e) {
                            Log.e("JSON", "Error parsing JSON", e);
                            runOnUiThread(() -> {
                                resultTitleTextView.setText("Error");
                                resultTextView.setText("Could not parse QR code data.");
                            });
                        }
                    }
                    image.close();
                });

                androidx.camera.core.CameraSelector cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Result getResult(byte[] imageData, int width, int height){
        Result rawResult = null;

        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grey = imageData[y * width + x] & 0xff;
                pixels[y * width + x] = 0xFF000000 | (grey * 0x00010101);
            }
        }
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();
        try {
            rawResult = reader.decode(bitmap);
        } catch (NotFoundException e) {
            Log.e("QRCode", "No QR code found", e);
        } catch (ChecksumException e) {
            Log.e("QRCode", "Checksum error", e);
        } catch (FormatException e) {
            Log.e("QRCode", "Format error", e);
        }
        return rawResult;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
