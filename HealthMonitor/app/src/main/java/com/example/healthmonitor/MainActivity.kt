package com.example.healthmonitor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import com.google.android.material.appbar.MaterialToolbar
import android.view.Menu
import android.view.MenuItem

class MainActivity : AppCompatActivity() {
    private lateinit var progressSpO2: CircularProgressIndicator
    private lateinit var progressHeartRate: CircularProgressIndicator
    private lateinit var progressTemperature: CircularProgressIndicator

    private lateinit var tvSpO2Value: TextView
    private lateinit var tvHeartRateValue: TextView
    private lateinit var tvTemperatureValue: TextView
    private lateinit var tvPrediction: TextView

    private lateinit var database: DatabaseReference
    private lateinit var btnStartMonitoring: CardView
    private lateinit var tvStartMonitoring: TextView
    
    private var isMonitoring = false
    private var monitoringStartTime: Long = 0
    private lateinit var healthPredictor: HealthPredictor
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        userId = user.uid

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        initializeViews()
        setupFirebaseDatabase()
        setupClickListeners()
        initializeHealthPredictor()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeViews() {
        progressSpO2 = findViewById(R.id.progressSpO2)
        progressHeartRate = findViewById(R.id.progressHeartRate)
        progressTemperature = findViewById(R.id.progressTemperature)

        tvSpO2Value = findViewById(R.id.tvSpO2Value)
        tvHeartRateValue = findViewById(R.id.tvHeartRateValue)
        tvTemperatureValue = findViewById(R.id.tvTemperatureValue)
        tvPrediction = findViewById(R.id.tvPrediction)

        btnStartMonitoring = findViewById(R.id.btnStartMonitoring)
        tvStartMonitoring = findViewById(R.id.tvStartMonitoring)

        progressSpO2.max = 100
        progressHeartRate.max = 150
        progressTemperature.max = 450
    }

    private fun initializeHealthPredictor() {
        healthPredictor = HealthPredictor(this)
    }

    private fun setupClickListeners() {
        btnStartMonitoring.setOnClickListener {
            if (!isMonitoring) {
                startMonitoring()
            } else {
                stopMonitoring()
            }
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnStatistics).setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAiPrediction).setOnClickListener {
            val intent = Intent(this, AiPredictionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startMonitoring() {
        isMonitoring = true
        monitoringStartTime = System.currentTimeMillis()
        btnStartMonitoring.setCardBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
        tvStartMonitoring.text = "Dừng\nkiểm tra"
    }

    private fun stopMonitoring() {
        isMonitoring = false
        btnStartMonitoring.setCardBackgroundColor(resources.getColor(android.R.color.white, null))
        tvStartMonitoring.text = "Bắt đầu\nkiểm tra"
        
        // Reset các thông số về 0
        updateHeartRate(0)
        updateSpO2(0)
        updateTemperature(0f)
        tvPrediction.text = "Chưa có dữ liệu"
    }

    private fun setupFirebaseDatabase() {
        // Lắng nghe node Health chung
        database = FirebaseDatabase.getInstance().getReference("Health")
        Log.d("Firebase", "Đã kết nối đến Firebase, đường dẫn: Health")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("Firebase", "Nhận được dữ liệu từ Firebase, isMonitoring: $isMonitoring")
                if (snapshot.exists() && isMonitoring) {
                    val heartRate = snapshot.child("HR").getValue(Int::class.java) ?: 0
                    val spO2 = snapshot.child("SpO2").getValue(Int::class.java) ?: 0
                    val temp = snapshot.child("Temp").getValue(Double::class.java) ?: 0.0

                    Log.d("FirebaseData", "HR: $heartRate, SpO2: $spO2, Temp: $temp")

                    // Copy sang node của user
                    updateUserHealth(heartRate, spO2, temp)

                    // Cập nhật UI từ luồng chính
                    runOnUiThread {
                        updateHeartRate(heartRate)
                        updateSpO2(spO2)
                        updateTemperature(temp.toFloat())
                        val prediction = healthPredictor.predict(heartRate, spO2, temp.toFloat())
                        updatePrediction(prediction)
                    }

                    // Lưu vào History
                    saveToUserHistory(heartRate, spO2, temp)
                } else {
                    Log.d("Firebase", "Không có dữ liệu hoặc không trong trạng thái monitoring")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to read data", error.toException())
            }
        })
    }

    private fun updatePrediction(prediction: String) {
        tvPrediction.text = prediction
        // Cập nhật màu sắc dựa trên kết quả dự đoán
        tvPrediction.setTextColor(
            when {
                prediction.contains("Bình thường") -> resources.getColor(android.R.color.holo_green_dark, null)
                prediction.contains("Bất thường") -> resources.getColor(android.R.color.holo_orange_dark, null)
                prediction.contains("Nguy hiểm") -> resources.getColor(android.R.color.holo_red_dark, null)
                else -> resources.getColor(android.R.color.darker_gray, null)
            }
        )
    }

    private fun updateUserHealth(heartRate: Int, spO2: Int, temp: Double) {
        val healthRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("Health")
        val healthData = mapOf(
            "HR" to heartRate,
            "SpO2" to spO2,
            "Temp" to temp
        )
        healthRef.setValue(healthData)
    }

    private fun saveToUserHistory(heartRate: Int, spO2: Int, temperature: Double) {
        val historyRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("History")
        val currentTime = System.currentTimeMillis()
        val historyData = mapOf(
            "heartRate" to heartRate,
            "spO2" to spO2,
            "temperature" to temperature,
            "timestamp" to currentTime
        )
        historyRef.push().setValue(historyData)
    }

    fun updateSpO2(value: Int) {
        if (value in 0..100) {
            progressSpO2.progress = value
            tvSpO2Value.text = "$value%"
        }
    }

    fun updateHeartRate(value: Int) {
        if (value in 0..150) {
            progressHeartRate.progress = value
            tvHeartRateValue.text = "$value BPM"
        }
    }

    fun updateTemperature(value: Float) {
        if (value in 0f..45f) {
            val progress = (value * 10).toInt()
            progressTemperature.progress = progress
            tvTemperatureValue.text = String.format("%.1f°C", value)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        healthPredictor.close()
    }
}
