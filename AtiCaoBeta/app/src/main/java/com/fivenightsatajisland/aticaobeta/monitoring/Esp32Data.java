package com.fivenightsatajisland.aticaobeta.monitoring;

import com.google.gson.annotations.SerializedName;

public class Esp32Data {
    @SerializedName("temp")
    private float temperature;
    
    @SerializedName("hum")
    private float humidity;
    
    @SerializedName("soil")
    private int soilMoistureRaw;
    
    @SerializedName("status")
    private String soilStatus;

    public float getTemperature() {
        return temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public int getSoilMoistureRaw() {
        return soilMoistureRaw;
    }

    public String getSoilStatus() {
        return soilStatus;
    }
}
