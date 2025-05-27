package com.example.healthmonitor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HealthHistoryAdapter extends RecyclerView.Adapter<HealthHistoryAdapter.ViewHolder> {

    private List<HealthRecord> healthRecords;

    public HealthHistoryAdapter(List<HealthRecord> healthRecords) {
        this.healthRecords = healthRecords;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_health_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HealthRecord record = healthRecords.get(position);
        holder.tvTime.setText("Time: " + record.getTime());
        holder.tvSpO2.setText("SpO2: " + record.getSpO2() + "%");
        holder.tvHeartRate.setText("Heart Rate: " + record.getHeartRate() + " BPM");
        holder.tvTemperature.setText("Temperature: " + record.getTemperature() + "°C");
    }

    @Override
    public int getItemCount() {
        return healthRecords.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvSpO2, tvHeartRate, tvTemperature;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSpO2 = itemView.findViewById(R.id.tvSpO2);
            tvHeartRate = itemView.findViewById(R.id.tvHeartRate);
            tvTemperature = itemView.findViewById(R.id.tvTemperature);
        }
    }
}