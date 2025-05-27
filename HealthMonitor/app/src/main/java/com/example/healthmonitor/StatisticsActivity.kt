package com.example.healthmonitor

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.healthmonitor.model.HealthHistory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {
    private lateinit var chartHeartRate: LineChart
    private lateinit var chartSpO2: LineChart
    private lateinit var chartTemperature: LineChart
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvNoData: TextView
    private lateinit var btnCalendar: ImageButton
    private lateinit var btnBack: ImageButton
    
    private lateinit var database: DatabaseReference
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        initializeViews()
        setupCharts()
        setupFirebaseDatabase()
        setupClickListeners()
        
        loadDataForDate(selectedDate)
    }

    private fun initializeViews() {
        chartHeartRate = findViewById(R.id.chartHeartRate)
        chartSpO2 = findViewById(R.id.chartSpO2)
        chartTemperature = findViewById(R.id.chartTemperature)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvNoData = findViewById(R.id.tvNoData)
        btnCalendar = findViewById(R.id.btnCalendar)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupCharts() {
        setupChart(chartHeartRate, "Nhịp tim (BPM)")
        setupChart(chartSpO2, "SpO2 (%)")
        setupChart(chartTemperature, "Nhiệt độ (°C)")
    }

    private fun setupChart(chart: LineChart, label: String) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            
            legend.isEnabled = true
            legend.textSize = 12f
            
            animateX(1000)
        }
    }

    private fun setupFirebaseDatabase() {
        database = FirebaseDatabase.getInstance().getReference("History")
    }

    private fun setupClickListeners() {
        btnCalendar.setOnClickListener {
            showDatePicker()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateSelectedDateText()
                loadDataForDate(selectedDate)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateSelectedDateText() {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        
        tvSelectedDate.text = when {
            isSameDay(selectedDate, today) -> "Hôm nay"
            isSameDay(selectedDate, yesterday) -> "Hôm qua"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun loadDataForDate(date: Calendar) {
        val startOfDay = date.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)
        
        val endOfDay = date.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        endOfDay.set(Calendar.SECOND, 59)
        endOfDay.set(Calendar.MILLISECOND, 999)

        Log.d("Statistics", "Đang tải dữ liệu từ ${startOfDay.time} đến ${endOfDay.time}")

        database.orderByChild("timestamp")
            .startAt(startOfDay.timeInMillis.toDouble())
            .endAt(endOfDay.timeInMillis.toDouble())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val histories = mutableListOf<HealthHistory>()
                    for (historySnapshot in snapshot.children) {
                        val history = historySnapshot.getValue(HealthHistory::class.java)
                        history?.let {
                            histories.add(it.copy(id = historySnapshot.key ?: ""))
                        }
                    }
                    
                    if (histories.isEmpty()) {
                        tvNoData.visibility = View.VISIBLE
                        chartHeartRate.visibility = View.GONE
                        chartSpO2.visibility = View.GONE
                        chartTemperature.visibility = View.GONE
                    } else {
                        tvNoData.visibility = View.GONE
                        chartHeartRate.visibility = View.VISIBLE
                        chartSpO2.visibility = View.VISIBLE
                        chartTemperature.visibility = View.VISIBLE
                        
                        updateCharts(histories.sortedBy { it.timestamp })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Statistics", "Lỗi khi tải dữ liệu", error.toException())
                }
            })
    }

    private fun updateCharts(histories: List<HealthHistory>) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val xLabels = histories.map { timeFormat.format(Date(it.timestamp)) }
        
        // Cập nhật biểu đồ nhịp tim
        val heartRateEntries = histories.mapIndexed { index, history ->
            Entry(index.toFloat(), history.heartRate.toFloat())
        }
        updateLineChart(chartHeartRate, heartRateEntries, xLabels, "Nhịp tim", Color.RED)
        
        // Cập nhật biểu đồ SpO2
        val spO2Entries = histories.mapIndexed { index, history ->
            Entry(index.toFloat(), history.spO2.toFloat())
        }
        updateLineChart(chartSpO2, spO2Entries, xLabels, "SpO2", Color.BLUE)
        
        // Cập nhật biểu đồ nhiệt độ
        val temperatureEntries = histories.mapIndexed { index, history ->
            Entry(index.toFloat(), history.temperature.toFloat())
        }
        updateLineChart(chartTemperature, temperatureEntries, xLabels, "Nhiệt độ", Color.GREEN)
    }

    private fun updateLineChart(chart: LineChart, entries: List<Entry>, xLabels: List<String>, label: String, color: Int) {
        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            setCircleColor(color)
            setDrawCircles(true)
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        chart.data = LineData(dataSet)
        chart.invalidate()
    }
} 