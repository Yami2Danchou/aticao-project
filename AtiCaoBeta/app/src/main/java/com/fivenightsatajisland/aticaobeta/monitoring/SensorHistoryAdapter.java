package com.fivenightsatajisland.aticaobeta.monitoring;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.fivenightsatajisland.aticaobeta.R;
import com.fivenightsatajisland.aticaobeta.database.SensorHistory;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SensorHistoryAdapter extends RecyclerView.Adapter<SensorHistoryAdapter.ViewHolder> {

    private final List<String> deviceNames = new ArrayList<>();
    private final Map<String, List<SensorHistory>> deviceDataMap = new HashMap<>();
    private final OnSensorHistoryClickListener listener;

    public interface OnSensorHistoryClickListener {
        void onDeleteDevice(String deviceName);
    }

    public SensorHistoryAdapter(OnSensorHistoryClickListener listener) {
        this.listener = listener;
    }

    public void setHistoryList(List<SensorHistory> fullList) {
        deviceNames.clear();
        deviceDataMap.clear();
        
        for (SensorHistory item : fullList) {
            String name = item.deviceName;
            if (!deviceDataMap.containsKey(name)) {
                deviceNames.add(name);
                deviceDataMap.put(name, new ArrayList<>());
            }
            if (deviceDataMap.get(name) != null) {
                deviceDataMap.get(name).add(item);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String deviceName = deviceNames.get(position);
        List<SensorHistory> data = deviceDataMap.get(deviceName);
        
        holder.tvDeviceName.setText(deviceName);
        if (data != null && !data.isEmpty()) {
            holder.tvLastSync.setText(holder.itemView.getContext().getString(R.string.last_reading_short, data.get(0).date));
            setupHistoryChart(holder.chart, data);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteDevice(deviceName);
        });
    }

    private void setupHistoryChart(LineChart chart, List<SensorHistory> data) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDrawGridBackground(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        
        // Add Marker View for hovering
        SensorMarkerView marker = new SensorMarkerView(chart.getContext(), R.layout.chart_marker_view, data);
        marker.setChartView(chart);
        chart.setMarker(marker);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(true);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setGranularity(1f);

        // Axis labels show Date only
        final SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < data.size()) {
                    int reverseIndex = data.size() - 1 - index;
                    if (reverseIndex >= 0 && reverseIndex < data.size()) {
                        String currentLabel = sdfDate.format(new Date(data.get(reverseIndex).timestamp));
                        
                        // Show label only if it's the first one OR the date changed from previous index
                        if (index == 0) return currentLabel;
                        
                        int prevReverseIndex = data.size() - 1 - (index - 1);
                        if (prevReverseIndex >= 0 && prevReverseIndex < data.size()) {
                            String prevLabel = sdfDate.format(new Date(data.get(prevReverseIndex).timestamp));
                            if (!currentLabel.equals(prevLabel)) {
                                return currentLabel;
                            }
                        }
                    }
                }
                return "";
            }
        });

        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(Color.parseColor("#33FFFFFF"));

        List<Entry> tempEntries = new ArrayList<>();
        List<Entry> humEntries = new ArrayList<>();
        List<Entry> soilEntries = new ArrayList<>();

        int count = data.size();
        for (int i = 0; i < count; i++) {
            SensorHistory h = data.get(count - 1 - i);
            tempEntries.add(new Entry(i, h.temperature));
            humEntries.add(new Entry(i, h.humidity));
            soilEntries.add(new Entry(i, h.soilValue / 40.95f));
        }

        LineDataSet tempSet = createSet(tempEntries, R.color.pod_warning, chart);
        LineDataSet humSet = createSet(humEntries, R.color.cacao_accent, chart);
        LineDataSet soilSet = createSet(soilEntries, R.color.pod_healthy, chart);

        chart.setData(new LineData(tempSet, humSet, soilSet));
        chart.invalidate();
    }

    private LineDataSet createSet(List<Entry> entries, int colorRes, View v) {
        LineDataSet set = new LineDataSet(entries, "");
        int color = ContextCompat.getColor(v.getContext(), colorRes);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setLineWidth(2f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        // Enable highlighting (vertical line)
        set.setHighlightEnabled(true);
        set.setDrawHighlightIndicators(true);
        set.setHighLightColor(Color.WHITE);
        set.setHighlightLineWidth(1f);

        return set;
    }

    @Override
    public int getItemCount() {
        return deviceNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName, tvLastSync;
        LineChart chart;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvLastSync = itemView.findViewById(R.id.tv_last_sync);
            chart = itemView.findViewById(R.id.history_chart);
            btnDelete = itemView.findViewById(R.id.btn_delete_device);
        }
    }
}
