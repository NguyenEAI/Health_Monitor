package com.example.healthmonitor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.healthmonitor.R
import com.example.healthmonitor.model.HealthHistory

class HealthHistoryAdapter(
    private var healthHistories: List<HealthHistory>,
    private val onDeleteClick: (HealthHistory) -> Unit
) : RecyclerView.Adapter<HealthHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvHeartRate: TextView = view.findViewById(R.id.tvHeartRate)
        val tvSpO2: TextView = view.findViewById(R.id.tvSpO2)
        val tvTemperature: TextView = view.findViewById(R.id.tvTemperature)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_health_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = healthHistories[position]
        holder.tvDateTime.text = history.getFormattedDate()
        holder.tvHeartRate.text = "${history.heartRate} BPM"
        holder.tvSpO2.text = "${history.spO2}%"
        holder.tvTemperature.text = String.format("%.1f°C", history.temperature)
        
        holder.btnDelete.setOnClickListener {
            onDeleteClick(history)
        }
    }

    override fun getItemCount() = healthHistories.size

    fun updateData(newHistories: List<HealthHistory>) {
        healthHistories = newHistories
        notifyDataSetChanged()
    }
} 