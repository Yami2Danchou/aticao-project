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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment implements ScanHistoryAdapter.OnHistoryItemClickListener {

    private ScanHistoryAdapter adapter;
    private List<ScanHistory> historyList;
    private RecyclerView rvHistory;
    private TextView tvNoHistory;
    private ImageView ivSortOrder;
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

        adapter = new ScanHistoryAdapter(requireContext(), this);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(adapter);

        setupSorting(view);
        loadHistory();
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
                        // Fallback to simpler format if needed
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
            Collections.sort(historyList, comparator);
        } else {
            Collections.sort(historyList, Collections.reverseOrder(comparator));
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
                    // Delete the local file to save space
                    if (history.imagePath != null && history.imagePath.startsWith("file://")) {
                        try {
                            File file = new File(Uri.parse(history.imagePath).getPath());
                            if (file.exists()) file.delete();
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
        
        // Safety for full image loading
        Bitmap bitmap = decodeSampledBitmapFromUri(Uri.parse(imagePath), 800, 800);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            Toast.makeText(getContext(), "Could not load full image. It may be missing or corrupted.", Toast.LENGTH_SHORT).show();
        }
        
        imageCard.addView(imageView);

        MaterialCardView btnClose = new MaterialCardView(getContext());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 32, 0, 0);
        btnClose.setLayoutParams(btnParams);
        btnClose.setRadius(32f);
        btnClose.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cacao_primary));
        
        TextView tvClose = new TextView(getContext());
        tvClose.setText("CLOSE");
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
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}