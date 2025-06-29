package com.example.qwickscan;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.json.JSONObject;

public class GenerateQrActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText descriptionEditText;
    private ImageView qrCodeImageView;
    private Button generateButton;
    private TextView generationStatusTextView;
    private static final int QrCodeWidth = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        generateButton = findViewById(R.id.generateButton);
        generationStatusTextView = findViewById(R.id.generationStatusTextView);

        generateButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();

            if (!title.isEmpty() && !description.isEmpty()) {
                try {
                    // Create JSON object
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("title", title);
                    jsonObject.put("description", description);
                    String jsonData = jsonObject.toString();

                    Bitmap bitmap = generateQRCode(jsonData);
                    qrCodeImageView.setImageBitmap(bitmap);
                    generationStatusTextView.setText("QR generated successfully!");
                } catch (WriterException e) {
                    Log.e("QR_GEN", "Error generating QR code", e);
                    generationStatusTextView.setText("Error generating QR code.");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("JSON", "Error creating JSON", e);
                    generationStatusTextView.setText("Error creating JSON data.");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter both title and description", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap generateQRCode(String data) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, QrCodeWidth, QrCodeWidth, null);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? ContextCompat.getColor(this, android.R.color.black) : ContextCompat.getColor(this, android.R.color.white));
            }
        }
        return bitmap;
    }
}
