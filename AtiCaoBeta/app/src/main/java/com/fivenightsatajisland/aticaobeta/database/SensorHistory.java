package com.fivenightsatajisland.aticaobeta.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sensor_history")
public class SensorHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public float temperature;
    public float humidity;
    public int soilValue;
    public String soilStatus;
    public String date;
    public String deviceName;
    public long timestamp; // Added for precise time filtering

    public SensorHistory(float temperature, float humidity, int soilValue, String soilStatus, String date, String deviceName, long timestamp) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.soilValue = soilValue;
        this.soilStatus = soilStatus;
        this.date = date;
        this.deviceName = deviceName;
        this.timestamp = timestamp;
    }
}
