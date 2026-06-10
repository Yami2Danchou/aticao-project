package com.fivenightsatajisland.aticaobeta.monitoring;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fivenightsatajisland.aticaobeta.R;
import com.fivenightsatajisland.aticaobeta.TutorialHandler;
import com.fivenightsatajisland.aticaobeta.databinding.FragmentMonitoringBinding;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonitoringFragment extends Fragment implements TutorialHandler {

    private FragmentMonitoringBinding binding;
    private MonitoringViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMonitoringBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(MonitoringViewModel.class);
        
        setupChart();
        setupObservers();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.stopPolling();
    }

    private void setupObservers() {
        viewModel.loading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnRetry.setEnabled(!isLoading);
        });

        viewModel.error.observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                binding.tvConnectionStatus.setText(R.string.connection_status_disconnected);
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pod_danger));
                binding.ivConnectionStatus.setImageResource(android.R.drawable.presence_offline);
                binding.ivConnectionStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pod_danger));
                
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry, v -> viewModel.startPolling())
                        .show();
            }
        });

        viewModel.data.observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                updateUI(data);
            }
        });

        viewModel.history.observe(getViewLifecycleOwner(), this::updateChart);
    }

    private void setupChart() {
        LineChart chart = binding.sensorChart;
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(true);
        chart.setGridBackgroundColor(Color.parseColor("#212121"));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.WHITE);
        
        final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Show date only on the first label to avoid duplicates
                if (value == 0) {
                    return sdf.format(new Date());
                }
                return "";
            }
        });

        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(Color.parseColor("#44FFFFFF"));
        chart.getAxisLeft().setTextColor(Color.WHITE);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
    }

    private void updateChart(java.util.List<Esp32Data> history) {
        if (history == null || history.isEmpty()) return;

        // Add Marker View for hovering
        MonitoringMarkerView marker = new MonitoringMarkerView(requireContext(), R.layout.chart_marker_view, history);
        marker.setChartView(binding.sensorChart);
        binding.sensorChart.setMarker(marker);

        java.util.List<Entry> tempEntries = new java.util.ArrayList<>();
        java.util.List<Entry> humEntries = new java.util.ArrayList<>();
        java.util.List<Entry> soilEntries = new java.util.ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            Esp32Data d = history.get(i);
            tempEntries.add(new Entry(i, d.getTemperature()));
            humEntries.add(new Entry(i, d.getHumidity()));
            soilEntries.add(new Entry(i, d.getSoilMoistureRaw() / 40.95f)); // Scale raw (0-4095) to 0-100% approx
        }

        LineDataSet tempSet = createDataSet(tempEntries, "Temp", R.color.pod_warning);
        LineDataSet humSet = createDataSet(humEntries, "Hum", R.color.cacao_accent);
        LineDataSet soilSet = createDataSet(soilEntries, "Soil", R.color.pod_healthy);

        LineData data = new LineData(tempSet, humSet, soilSet);
        binding.sensorChart.setData(data);
        binding.sensorChart.invalidate();
    }

    private LineDataSet createDataSet(java.util.List<Entry> entries, String label, int colorRes) {
        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(ContextCompat.getColor(requireContext(), colorRes));
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setDrawCircles(true);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCircleColor(ContextCompat.getColor(requireContext(), colorRes));
        
        // Add highlighting for dashboard chart
        set.setHighlightEnabled(true);
        set.setDrawHighlightIndicators(true);
        set.setHighLightColor(Color.WHITE);

        return set;
    }

    private void setupListeners() {
        binding.btnRetry.setOnClickListener(v -> viewModel.startPolling());
        binding.btnScanQr.setOnClickListener(v -> startQrScanner());
        binding.btnLog.setVisibility(View.GONE); // Hidden as requested (auto-logging enabled)
    }

    private void startQrScanner() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(requireContext(), options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null && rawValue.startsWith("WIFI:")) {
                        handleWifiQr(rawValue);
                    } else {
                        Snackbar.make(binding.getRoot(), "Invalid WiFi QR Code", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> 
                    Snackbar.make(binding.getRoot(), "Scanning failed: " + e.getMessage(), Snackbar.LENGTH_SHORT).show()
                );
    }

    private void handleWifiQr(String qrData) {
        if (qrData == null || !qrData.startsWith("WIFI:")) {
            Snackbar.make(binding.getRoot(), "Invalid WiFi QR Code format", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String ssid = extractValue(qrData, "S:");
        String password = extractValue(qrData, "P:");

        if (ssid == null || ssid.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Invalid QR: Missing SSID", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Removed SSID setting logic as requested

        String displayPassword = (password != null && !password.isEmpty()) ? "********" : "None";

        new AlertDialog.Builder(requireContext())
                .setTitle("AtiCao Device Found")
                .setMessage("SSID: " + ssid + "\nPassword: " + displayPassword + 
                        "\n\nCopy the password below and connect to this device in your WiFi settings.")
                .setPositiveButton("Copy & Open Settings", (dialog, which) -> {
                    if (password != null) {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("WiFi Password", password);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), "Password copied!", Toast.LENGTH_SHORT).show();
                    }
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String extractValue(String data, String prefix) {
        int index = data.indexOf(prefix);
        if (index == -1) return null;
        int start = index + prefix.length();
        int end = data.indexOf(";", start);
        if (end == -1) return data.substring(start);
        return data.substring(start, end);
    }

    private void updateUI(Esp32Data data) {
        binding.tvTemperature.setText(String.format(Locale.getDefault(), "%.1f °C", data.getTemperature()));
        binding.tvHumidity.setText(String.format(Locale.getDefault(), "%.1f %%", data.getHumidity()));
        binding.tvSoilRaw.setText(String.format(Locale.getDefault(), "Value: %d", data.getSoilMoistureRaw()));
        binding.tvSoilStatus.setText(data.getSoilStatus());
        
        int statusColor;
        switch (data.getSoilStatus().toUpperCase()) {
            case "DRY": statusColor = R.color.pod_warning; break;
            case "WET": statusColor = R.color.cacao_accent; break;
            case "NORMAL": statusColor = R.color.pod_healthy; break;
            case "NO SOIL": statusColor = R.color.text_secondary; break;
            default: statusColor = R.color.pod_danger; break;
        }
        binding.tvSoilStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor));

        binding.tvConnectionStatus.setText(R.string.connection_status_connected);
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
        binding.ivConnectionStatus.setImageResource(android.R.drawable.presence_online);
        binding.ivConnectionStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pod_healthy));

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        binding.tvLastUpdate.setText(getString(R.string.last_reading, currentTime));
    }

    @Override
    public void showTutorial() {
        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(binding.tvTemperature, getString(R.string.tut_env_temp_title), getString(R.string.tut_env_temp_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(60),
                        TapTarget.forView(binding.tvSoilStatus, getString(R.string.tut_soil_moisture_title), getString(R.string.tut_soil_moisture_desc))
                                .outerCircleColor(R.color.cacao_accent)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(60),
                        TapTarget.forView(binding.btnScanQr, getString(R.string.tut_wifi_config_title), getString(R.string.tut_wifi_config_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(40),
                        TapTarget.forView(binding.sensorChart, getString(R.string.tut_chart_trends_title), getString(R.string.tut_chart_trends_desc))
                                .outerCircleColor(R.color.cacao_accent)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(80)
                )
                .start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
