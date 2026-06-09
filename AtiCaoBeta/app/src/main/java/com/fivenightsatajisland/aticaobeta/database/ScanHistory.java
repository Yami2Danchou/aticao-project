package com.fivenightsatajisland.aticaobeta.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scan_history")
public class ScanHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String result; // Primary result (usually Alpha or user selected)
    public float confidence;
    public String date;
    public String imagePath;
    public String severity;

    // Comparison fields
    public float confidenceAlpha;
    public float confidenceBeta;
    public String resultAlpha;
    public String resultBeta;

    public ScanHistory(String result, float confidence, String date, String imagePath, String severity, 
                       float confidenceAlpha, float confidenceBeta, String resultAlpha, String resultBeta) {
        this.result = result;
        this.confidence = confidence;
        this.date = date;
        this.imagePath = imagePath;
        this.severity = severity;
        this.confidenceAlpha = confidenceAlpha;
        this.confidenceBeta = confidenceBeta;
        this.resultAlpha = resultAlpha;
        this.resultBeta = resultBeta;
    }
}