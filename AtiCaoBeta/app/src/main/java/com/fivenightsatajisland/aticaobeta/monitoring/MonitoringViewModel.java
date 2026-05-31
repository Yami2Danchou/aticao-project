package com.fivenightsatajisland.aticaobeta.monitoring;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MonitoringViewModel extends ViewModel {

    private final MutableLiveData<Esp32Data> _data = new MutableLiveData<>();
    public LiveData<Esp32Data> data = _data;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>();
    public LiveData<Boolean> loading = _loading;

    private final Esp32ApiService apiService;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPolling = false;
    private static final int POLLING_INTERVAL = 1000; // 1 second update

    public MonitoringViewModel() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.4.1/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(Esp32ApiService.class);
    }

    public void startPolling() {
        if (!isPolling) {
            isPolling = true;
            _loading.setValue(true);
            executeFetch();
        }
    }

    public void stopPolling() {
        isPolling = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void executeFetch() {
        if (!isPolling) return;

        apiService.getSensorData().enqueue(new Callback<Esp32Data>() {
            @Override
            public void onResponse(Call<Esp32Data> call, Response<Esp32Data> response) {
                _loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _data.setValue(response.body());
                    _error.setValue(null);
                    scheduleNextFetch(); // Only schedule next if successful
                } else {
                    _error.setValue("Invalid response from ESP32");
                    isPolling = false; // Stop auto-polling on error
                }
            }

            @Override
            public void onFailure(Call<Esp32Data> call, Throwable t) {
                _loading.setValue(false);
                _error.setValue("ESP32 Device Not Found. Please connect to AtiCao WiFi.");
                isPolling = false; // Stop auto-polling on failure
            }
        });
    }

    private void scheduleNextFetch() {
        if (isPolling) {
            handler.postDelayed(this::executeFetch, POLLING_INTERVAL);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling();
    }
}
