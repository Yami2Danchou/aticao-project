package com.fivenightsatajisland.aticaobeta.monitoring;

import retrofit2.Call;
import retrofit2.http.GET;

public interface Esp32ApiService {
    @GET("data")
    Call<Esp32Data> getSensorData();
}
