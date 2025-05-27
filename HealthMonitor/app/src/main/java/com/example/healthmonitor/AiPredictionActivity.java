package com.example.healthmonitor;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class AiPredictionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_prediction);
        // Xu ly su kien back ve Home
        ImageButton btnBackHome1 = findViewById(R.id.btnBackHome1);
        btnBackHome1.setOnClickListener(view -> finish());
    }

}
