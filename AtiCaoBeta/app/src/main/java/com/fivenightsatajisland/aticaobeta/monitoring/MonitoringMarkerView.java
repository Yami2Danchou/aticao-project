package com.fivenightsatajisland.aticaobeta.monitoring;

import android.content.Context;
import android.widget.TextView;
import com.fivenightsatajisland.aticaobeta.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MonitoringMarkerView extends MarkerView {

    private final TextView tvDate;
    private final TextView tvValue;
    private final List<Esp32Data> historyData;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());

    public MonitoringMarkerView(Context context, int layoutResource, List<Esp32Data> historyData) {
        super(context, layoutResource);
        tvDate = findViewById(R.id.tv_marker_date);
        tvValue = findViewById(R.id.tv_marker_value);
        this.historyData = historyData;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        if (index >= 0 && index < historyData.size()) {
            Esp32Data data = historyData.get(index);
            tvDate.setText(sdf.format(new Date(data.getTimestamp())));
            tvValue.setText(String.format(Locale.getDefault(), "Value: %.1f", e.getY()));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
