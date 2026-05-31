package com.fivenightsatajisland.aticaobeta.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scan_history")
public class ScanHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String result;
    public float confidence;
    public String date;
    public String imagePath;
    public String severity; // Added severity

    public ScanHistory(String result, float confidence, String date, String imagePath, String severity) {
        this.result = result;
        this.confidence = confidence;
        this.date = date;
        this.imagePath = imagePath;
        this.severity = severity;
    }
}