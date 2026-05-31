package com.fivenightsatajisland.aticaobeta.monitoring;

import android.content.Context;
import android.content.Intent;
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
import com.fivenightsatajisland.aticaobeta.databinding.FragmentMonitoringBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonitoringFragment extends Fragment {

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
    }

    private void setupListeners() {
        binding.btnRetry.setOnClickListener(v -> viewModel.startPolling());
        binding.btnScanQr.setOnClickListener(v -> startQrScanner());
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
        binding.tvSoilRaw.setText(String.format(Locale.getDefault(), "Raw Value: %d", data.getSoilMoistureRaw()));
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
