package com.fivenightsatajisland.aticaobeta;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fivenightsatajisland.aticaobeta.database.ScanHistory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScanHistoryAdapter extends RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {

    private List<ScanHistory> historyList = new ArrayList<>();
    private final Context context;
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onItemClick(ScanHistory history);
        void onDeleteClick(ScanHistory history);
    }

    public ScanHistoryAdapter(Context context, OnHistoryItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setHistoryList(List<ScanHistory> list) {
        this.historyList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanHistory history = historyList.get(position);
        
        holder.tvResult.setText(history.result);
        holder.tvConfidence.setText(String.format(Locale.getDefault(), "Confidence: %.1f%%", history.confidence));
        holder.tvDate.setText(history.date);
        holder.tvSeverity.setText("Severity: " + (history.severity != null ? history.severity : "N/A"));

        // Set color based on result
        int color;
        if (history.result.contains("Black Pod Rot")) color = ContextCompat.getColor(context, R.color.pod_danger);
        else if (history.result.contains("Pod Borer")) color = ContextCompat.getColor(context, R.color.pod_warning);
        else if (history.result.contains("Healthy")) color = ContextCompat.getColor(context, R.color.pod_healthy);
        else color = ContextCompat.getColor(context, R.color.text_main);
        holder.tvResult.setTextColor(color);

        // Load thumbnail with safety
        if (history.imagePath != null) {
            Bitmap thumbnail = decodeSampledBitmapFromUri(Uri.parse(history.imagePath), 100, 100);
            if (thumbnail != null) {
                holder.ivThumb.setImageBitmap(thumbnail);
            } else {
                holder.ivThumb.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            holder.ivThumb.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(history));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(history));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            try (InputStream input2 = context.getContentResolver().openInputStream(uri)) {
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvResult, tvConfidence, tvDate, tvSeverity;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            tvResult = itemView.findViewById(R.id.tv_result);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvSeverity = itemView.findViewById(R.id.tv_severity);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}