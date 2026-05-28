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
import android.util.TypedValue;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.fivenightsatajisland.aticaobeta.database.AppDatabase;
import com.fivenightsatajisland.aticaobeta.database.ScanHistory;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        LinearLayout historyContainer = view.findViewById(R.id.history_container);
        TextView tvNoHistory = view.findViewById(R.id.tv_no_history);
        
        List<ScanHistory> historyList = AppDatabase.getDatabase(getContext()).scanHistoryDao().getAll();
        
        if (historyList.isEmpty()) {
            tvNoHistory.setVisibility(View.VISIBLE);
        } else {
            tvNoHistory.setVisibility(View.GONE);
            for (ScanHistory history : historyList) {
                addHistoryItem(historyContainer, history);
            }
        }
        
        return view;
    }

    private void addHistoryItem(LinearLayout container, ScanHistory history) {
        if (getContext() == null) return;
        
        MaterialCardView card = new MaterialCardView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setRadius(32f);
        card.setCardElevation(2f);
        card.setStrokeWidth(1);
        
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValue, true);
        card.setStrokeColor(typedValue.data);
        
        card.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.white)); // Default card bg
        // Wait, MaterialCardView handles its own background if not specified, but let's be sure
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        card.setCardBackgroundColor(typedValue.data);

        card.setContentPadding(32, 32, 32, 32);

        LinearLayout horizontalLayout = new LinearLayout(getContext());
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        horizontalLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ImageView ivThumb = new ImageView(getContext());
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(120, 120);
        imgParams.setMargins(0, 0, 32, 0);
        ivThumb.setLayoutParams(imgParams);
        ivThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (history.imagePath != null) {
            Bitmap thumbnail = decodeSampledBitmapFromUri(Uri.parse(history.imagePath), 120, 120);
            if (thumbnail != null) {
                ivThumb.setImageBitmap(thumbnail);
            } else {
                ivThumb.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

        card.setOnClickListener(v -> showFullImage(history.imagePath, history.result));

        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvResult = new TextView(getContext());
        tvResult.setText(history.result);
        tvResult.setTextSize(18);
        tvResult.setTypeface(null, android.graphics.Typeface.BOLD);
        
        int color;
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        color = typedValue.data;
        
        if (history.result.contains("Black Pod Rot")) color = ContextCompat.getColor(getContext(), R.color.pod_danger);
        else if (history.result.contains("Pod Borer")) color = ContextCompat.getColor(getContext(), R.color.pod_warning);
        else if (history.result.contains("Healthy")) color = ContextCompat.getColor(getContext(), R.color.pod_healthy);
        tvResult.setTextColor(color);

        TextView tvConfidence = new TextView(getContext());
        tvConfidence.setText(String.format(Locale.getDefault(), "Confidence: %.1f%%", history.confidence));
        tvConfidence.setTextSize(12);
        tvConfidence.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));

        TextView tvDate = new TextView(getContext());
        tvDate.setText(history.date);
        tvDate.setTextSize(14);
        tvDate.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));

        textLayout.addView(tvResult);
        textLayout.addView(tvConfidence);
        textLayout.addView(tvDate);
        
        horizontalLayout.addView(ivThumb);
        horizontalLayout.addView(textLayout);
        
        card.addView(horizontalLayout);
        container.addView(card);
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

    private void showFullImage(String imagePath, String title) {
        if (getContext() == null || imagePath == null) return;
        
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Let's just create a dynamic view for the dialog to avoid needing a new XML file
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        layout.setBackgroundColor(typedValue.data);
        
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setPadding(32, 32, 32, 32);
        layout.setGravity(android.view.Gravity.CENTER);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        tvTitle.setTextColor(typedValue.data);
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
        imageView.setImageURI(Uri.parse(imagePath));
        imageCard.addView(imageView);

        MaterialCardView btnClose = new MaterialCardView(getContext());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 32, 0, 0);
        btnClose.setLayoutParams(btnParams);
        btnClose.setRadius(32f);
        getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        btnClose.setCardBackgroundColor(typedValue.data);
        
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
        
        // Ensure dialog doesn't go full screen and hide status/nav bars
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }
}