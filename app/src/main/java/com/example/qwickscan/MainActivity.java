package com.example.qwickscan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qwickscan.ScanQrActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton generateQrButton = findViewById(R.id.generateQrButton);
        ImageButton scanQrButton = findViewById(R.id.scanQrButton);

        generateQrButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GenerateQrActivity.class);
            startActivity(intent);
        });

        scanQrButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScanQrActivity.class);
            startActivity(intent);
        });
    }
}
