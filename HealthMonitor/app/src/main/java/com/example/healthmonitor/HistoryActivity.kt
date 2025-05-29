package com.example.healthmonitor

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthmonitor.adapter.HealthHistoryAdapter
import com.example.healthmonitor.model.HealthHistory
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvNoData: TextView
    private lateinit var btnCalendar: ImageButton
    private lateinit var btnBack: ImageButton
    
    private lateinit var historyAdapter: HealthHistoryAdapter
    private lateinit var database: DatabaseReference
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val userId = intent.getStringExtra("USER_ID") ?: return

        initializeViews()
        setupRecyclerView()
        setupFirebaseDatabase(userId)
        setupClickListeners()
        
        loadHistoryForDate(selectedDate)
    }

    private fun initializeViews() {
        rvHistory = findViewById(R.id.rvHistory)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvNoData = findViewById(R.id.tvNoData)
        btnCalendar = findViewById(R.id.btnCalendar)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupRecyclerView() {
        historyAdapter = HealthHistoryAdapter(emptyList()) { history ->
            deleteHistoryItem(history)
        }
        rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupFirebaseDatabase(userId: String) {
        database = FirebaseDatabase.getInstance().getReference("users").child(userId).child("History")
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
                loadHistoryForDate(selectedDate)
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

    private fun loadHistoryForDate(date: Calendar) {
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

        Log.d("History", "Đang tải dữ liệu từ ${startOfDay.timeInMillis} đến ${endOfDay.timeInMillis}")

        database.orderByChild("timestamp")
            .startAt(startOfDay.timeInMillis.toDouble())
            .endAt(endOfDay.timeInMillis.toDouble())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("History", "Nhận được dữ liệu từ Firebase, số lượng: ${snapshot.childrenCount}")
                    
                    val histories = mutableListOf<HealthHistory>()
                    for (historySnapshot in snapshot.children) {
                        val history = historySnapshot.getValue(HealthHistory::class.java)
                        history?.let {
                            histories.add(it.copy(id = historySnapshot.key ?: ""))
                        }
                    }
                    
                    Log.d("History", "Đã xử lý ${histories.size} mục dữ liệu")
                    
                    if (histories.isEmpty()) {
                        tvNoData.visibility = View.VISIBLE
                        rvHistory.visibility = View.GONE
                    } else {
                        tvNoData.visibility = View.GONE
                        rvHistory.visibility = View.VISIBLE
                        historyAdapter.updateData(histories.sortedByDescending { it.timestamp })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("History", "Lỗi khi tải dữ liệu", error.toException())
                }
            })
    }

    private fun deleteHistoryItem(history: HealthHistory) {
        if (history.id.isNotEmpty()) {
            Log.d("History", "Đang xóa dữ liệu với ID: ${history.id}")
            
            database.child(history.id).removeValue()
                .addOnSuccessListener {
                    Log.d("History", "Xóa dữ liệu thành công")
                }
                .addOnFailureListener { e ->
                    Log.e("History", "Lỗi khi xóa dữ liệu", e)
                }
        } else {
            Log.e("History", "Không thể xóa dữ liệu: ID trống")
        }
    }
} 