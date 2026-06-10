package com.fivenightsatajisland.aticaobeta;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fivenightsatajisland.aticaobeta.database.AppDatabase;
import com.fivenightsatajisland.aticaobeta.database.ScanHistory;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment implements ScanHistoryAdapter.OnHistoryItemClickListener, TutorialHandler {

    private ScanHistoryAdapter adapter;
    private List<ScanHistory> historyList;
    private RecyclerView rvHistory;
    private TextView tvNoHistory;
    private ImageView ivSortOrder;
    private ImageView ivCompareGraph;
    private boolean isAscending = false;
    private String currentSort = "Time";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHistory = view.findViewById(R.id.rv_history);
        tvNoHistory = view.findViewById(R.id.tv_no_history);
        ivSortOrder = view.findViewById(R.id.iv_sort_order);
        ivCompareGraph = view.findViewById(R.id.iv_compare_graph);

        adapter = new ScanHistoryAdapter(requireContext(), this);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(adapter);

        setupSorting(view);
        loadHistory();
    }

    @Override
    public void showTutorial() {
        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(rvHistory.findViewById(R.id.chip_time), getString(R.string.tut_sort_time_title), getString(R.string.tut_sort_time_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(30),
                        TapTarget.forView(ivSortOrder, getString(R.string.tut_sort_order_title), getString(R.string.tut_sort_order_desc))
                                .outerCircleColor(R.color.cacao_accent)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(30),
                        TapTarget.forView(ivCompareGraph, getString(R.string.tut_compare_title), getString(R.string.tut_compare_desc))
                                .outerCircleColor(R.color.cacao_primary)
                                .targetCircleColor(R.color.white)
                                .transparentTarget(true)
                                .tintTarget(true)
                                .targetRadius(30)
                )
                .start();
    }

    private void setupSorting(View view) {
        Chip chipTime = view.findViewById(R.id.chip_time);
        Chip chipDisease = view.findViewById(R.id.chip_disease);
        Chip chipSeverity = view.findViewById(R.id.chip_severity);

        chipTime.setOnClickListener(v -> {
            currentSort = "Time";
            applySort();
        });
        chipDisease.setOnClickListener(v -> {
            currentSort = "Disease";
            applySort();
        });
        chipSeverity.setOnClickListener(v -> {
            currentSort = "Severity";
            applySort();
        });

        ivSortOrder.setOnClickListener(v -> {
            isAscending = !isAscending;
            ivSortOrder.setImageResource(isAscending ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float);
            applySort();
        });

        ivCompareGraph.setOnClickListener(v -> showComparisonGraph());
    }

    private void showComparisonGraph() {
        if (historyList == null || historyList.isEmpty()) {
            Toast.makeText(getContext(), R.string.compare_data_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_light));
        layout.setPadding(48, 48, 48, 48);
        layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(R.string.compare_models_title);
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
        tvTitle.setGravity(android.view.Gravity.CENTER);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 48);

        BarChart chart = new BarChart(getContext());
        chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 900));

        List<BarEntry> alphaEntries = new ArrayList<>();
        List<BarEntry> betaEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int count = Math.min(10, historyList.size());
        int i = 0;
        while (i < count) {
            ScanHistory h = historyList.get(count - 1 - i); // Chronological
            alphaEntries.add(new BarEntry(i, h.confidenceAlpha));
            betaEntries.add(new BarEntry(i, h.confidenceBeta));
            labels.add(getString(R.string.scan_num_prefix) + (historyList.size() - (count - 1 - i)));
            i++;
        }

        BarDataSet setAlpha = new BarDataSet(alphaEntries, getString(R.string.alpha_model_label));
        setAlpha.setColor(ContextCompat.getColor(requireContext(), R.color.cacao_primary));
        setAlpha.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
        
        BarDataSet setBeta = new BarDataSet(betaEntries, getString(R.string.beta_model_label));
        setBeta.setColor(ContextCompat.getColor(requireContext(), R.color.cacao_accent));
        setBeta.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));

        BarData data = new BarData(setAlpha, setBeta);
        data.setBarWidth(0.35f);

        chart.setData(data);
        chart.groupBars(0f, 0.2f, 0.05f);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setCenterAxisLabels(true);
        chart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setAxisMinimum(0f);
        chart.getXAxis().setAxisMaximum(count);
        chart.getXAxis().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        chart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
        chart.animateY(1000);
        chart.invalidate();

        MaterialCardView btnClose = new MaterialCardView(requireContext());
        btnClose.setRadius(32f);
        btnClose.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cacao_primary));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = 48;
        btnClose.setLayoutParams(btnParams);
        
        TextView tvClose = new TextView(getContext());
        tvClose.setText(R.string.btn_close);
        tvClose.setTextColor(Color.WHITE);
        tvClose.setTypeface(null, android.graphics.Typeface.BOLD);
        tvClose.setPadding(80, 24, 80, 24);
        btnClose.addView(tvClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        layout.addView(tvTitle);
        layout.addView(chart);
        layout.addView(btnClose);
        
        dialog.setContentView(layout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void loadHistory() {
        historyList = AppDatabase.getDatabase(getContext()).scanHistoryDao().getAll();
        applySort();
    }

    private void applySort() {
        if (historyList == null || historyList.isEmpty()) {
            tvNoHistory.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
            return;
        }

        tvNoHistory.setVisibility(View.GONE);
        rvHistory.setVisibility(View.VISIBLE);

        Comparator<ScanHistory> comparator;
        switch (currentSort) {
            case "Disease":
                comparator = (o1, o2) -> o1.result.compareToIgnoreCase(o2.result);
                break;
            case "Severity":
                comparator = (o1, o2) -> {
                    int s1 = getSeverityValue(o1.severity);
                    int s2 = getSeverityValue(o2.severity);
                    return Integer.compare(s1, s2);
                };
                break;
            case "Time":
            default:
                comparator = (o1, o2) -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
                    try {
                        Date d1 = sdf.parse(o1.date);
                        Date d2 = sdf.parse(o2.date);
                        if (d1 != null && d2 != null) return d1.compareTo(d2);
                    } catch (ParseException e) {
                        try {
                            SimpleDateFormat sdfShort = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            Date d1 = sdfShort.parse(o1.date);
                            Date d2 = sdfShort.parse(o2.date);
                            if (d1 != null && d2 != null) return d1.compareTo(d2);
                        } catch (ParseException e2) {
                            return 0;
                        }
                    }
                    return 0;
                };
                break;
        }

        if (isAscending) {
            historyList.sort(comparator);
        } else {
            historyList.sort(Collections.reverseOrder(comparator));
        }

        adapter.setHistoryList(historyList);
    }

    private int getSeverityValue(String severity) {
        if (severity == null) return 0;
        switch (severity) {
            case "Healthy": return 1;
            case "Mild": return 2;
            case "Moderate": return 3;
            case "Severe": return 4;
            default: return 0;
        }
    }

    @Override
    public void onItemClick(ScanHistory history) {
        showFullImage(history.imagePath, history.result);
    }

    @Override
    public void onDeleteClick(ScanHistory history) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Scan")
                .setMessage("Are you sure you want to delete this scan from history?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (history.imagePath != null && history.imagePath.startsWith("file://")) {
                        try {
                            String path = Uri.parse(history.imagePath).getPath();
                            if (path != null) {
                                File file = new File(path);
                                if (file.exists() && !file.delete()) {
                                    android.util.Log.w("HistoryFragment", "Could not delete file: " + path);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    AppDatabase.getDatabase(getContext()).scanHistoryDao().deleteById(history.id);
                    loadHistory();
                    Toast.makeText(getContext(), "Scan deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFullImage(String imagePath, String title) {
        if (getContext() == null || imagePath == null) return;
        
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setPadding(32, 32, 32, 32);
        layout.setGravity(android.view.Gravity.CENTER);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 32);
        tvTitle.setGravity(android.view.Gravity.CENTER);

        MaterialCardView imageCard = new MaterialCardView(getContext());
        imageCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800));
        imageCard.setRadius(24f);
        imageCard.setCardElevation(0f);
        imageCard.setStrokeWidth(0);

        ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        Bitmap bitmap = decodeSampledBitmapFromUri(Uri.parse(imagePath), 800, 800);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            Toast.makeText(getContext(), "Could not load full image.", Toast.LENGTH_SHORT).show();
        }
        
        imageCard.addView(imageView);

        MaterialCardView btnClose = new MaterialCardView(getContext());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 32, 0, 0);
        btnClose.setLayoutParams(btnParams);
        btnClose.setRadius(32f);
        btnClose.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cacao_primary));
        
        TextView tvClose = new TextView(getContext());
        tvClose.setText(R.string.btn_close);
        tvClose.setPadding(64, 24, 64, 24);
        tvClose.setTextColor(Color.WHITE);
        tvClose.setTypeface(null, android.graphics.Typeface.BOLD);
        btnClose.addView(tvClose);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());

        layout.addView(tvTitle);
        layout.addView(imageCard);
        layout.addView(btnClose);

        dialog.setContentView(layout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
        if (getContext() == null) return null;
        try (InputStream input = getContext().getContentResolver().openInputStream(uri)) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            try (InputStream input2 = getContext().getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(input2, null, options);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            do {
                inSampleSize *= 2;
            } while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth);
        }
        return inSampleSize;
    }
}