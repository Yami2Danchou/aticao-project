package com.fivenightsatajisland.aticaobeta.monitoring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fivenightsatajisland.aticaobeta.R;
import com.fivenightsatajisland.aticaobeta.database.AppDatabase;
import com.fivenightsatajisland.aticaobeta.database.SensorHistory;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class SensorHistoryFragment extends Fragment implements SensorHistoryAdapter.OnSensorHistoryClickListener {

    private SensorHistoryAdapter adapter;
    private RecyclerView rvHistory;
    private TextView tvNoHistory;
    private TextView tvConnectionStatus;
    private ImageView ivConnectionStatus;
    private ProgressBar pbPolling;
    private MonitoringViewModel viewModel;
    private LiveData<List<SensorHistory>> currentLiveData;
    private String selectedRange = "10m";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sensor_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHistory = view.findViewById(R.id.rv_sensor_history);
        tvNoHistory = view.findViewById(R.id.tv_no_sensor_history);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        ivConnectionStatus = view.findViewById(R.id.iv_connection_status);
        pbPolling = view.findViewById(R.id.pb_polling);

        viewModel = new ViewModelProvider(this).get(MonitoringViewModel.class);

        adapter = new SensorHistoryAdapter(this);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(adapter);

        setupChips(view);
        setupObservers();
        observeHistory();
    }

    private void setupChips(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_range);
        chipGroup.check(R.id.chip_10m);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_10m) selectedRange = "10m";
            else if (id == R.id.chip_1h) selectedRange = "1h";
            else if (id == R.id.chip_24h) selectedRange = "24h";
            else if (id == R.id.chip_3d) selectedRange = "3d";
            else if (id == R.id.chip_1w) selectedRange = "1w";
            else if (id == R.id.chip_30d) selectedRange = "30d";
            else if (id == R.id.chip_6mo) selectedRange = "6mo";
            
            observeHistory();
        });
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
            pbPolling.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                tvConnectionStatus.setText(R.string.connection_status_disconnected);
                tvConnectionStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pod_danger));
                ivConnectionStatus.setImageResource(android.R.drawable.presence_offline);
                ivConnectionStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pod_danger));
            }
        });

        viewModel.data.observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                tvConnectionStatus.setText(R.string.connection_status_connected);
                tvConnectionStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
                ivConnectionStatus.setImageResource(android.R.drawable.presence_online);
                ivConnectionStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pod_healthy));
            }
        });
    }

    private void observeHistory() {
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }

        long startTime = System.currentTimeMillis();
        switch (selectedRange) {
            case "10m": startTime -= 10L * 60 * 1000; break;
            case "1h": startTime -= 60L * 60 * 1000; break;
            case "24h": startTime -= 24L * 60 * 60 * 1000; break;
            case "3d": startTime -= 3L * 24 * 60 * 60 * 1000; break;
            case "1w": startTime -= 7L * 24 * 60 * 60 * 1000; break;
            case "30d": startTime -= 30L * 24 * 60 * 60 * 1000; break;
            case "6mo": startTime -= 180L * 24 * 60 * 60 * 1000; break;
        }

        currentLiveData = AppDatabase.getDatabase(getContext()).sensorHistoryDao().getRecent(startTime);
        currentLiveData.observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                tvNoHistory.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
            } else {
                tvNoHistory.setVisibility(View.GONE);
                rvHistory.setVisibility(View.VISIBLE);
                adapter.setHistoryList(list);
            }
        });
    }

    @Override
    public void onDeleteDevice(String deviceName) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_device_history)
                .setMessage(getString(R.string.delete_device_message, deviceName))
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.getDatabase(getContext()).sensorHistoryDao().deleteByDevice(deviceName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
