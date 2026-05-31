package com.fivenightsatajisland.aticaobeta.monitoring;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fivenightsatajisland.aticaobeta.database.AppDatabase;
import com.fivenightsatajisland.aticaobeta.database.SensorHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MonitoringViewModel extends AndroidViewModel {

    private final MutableLiveData<Esp32Data> _data = new MutableLiveData<>();
    public LiveData<Esp32Data> data = _data;

    private final MutableLiveData<List<Esp32Data>> _history = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Esp32Data>> history = _history;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>();
    public LiveData<Boolean> loading = _loading;

    private final Esp32ApiService apiService;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPolling = false;
    private final String deviceName = "ESP32 ATI-CAO";
    private static final int POLLING_INTERVAL = 800; // Aiming for ~1s including overhead
    private static final int MAX_HISTORY_SIZE = 30;
    
    private long lastLogTime = 0;
    private static final long AUTO_LOG_INTERVAL = 1000; // Log to DB every 1 second

    public MonitoringViewModel(@NonNull Application application) {
        super(application);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
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
            public void onResponse(@NonNull Call<Esp32Data> call, @NonNull Response<Esp32Data> response) {
                _loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    Esp32Data newData = response.body();
                    long currentTimeMillis = System.currentTimeMillis();
                    newData.setTimestamp(currentTimeMillis);
                    _data.setValue(newData);
                    
                    List<Esp32Data> currentHistory = _history.getValue();
                    if (currentHistory == null) currentHistory = new ArrayList<>();
                    currentHistory.add(newData);
                    if (currentHistory.size() > MAX_HISTORY_SIZE) {
                        currentHistory.remove(0);
                    }
                    _history.setValue(new ArrayList<>(currentHistory));
                    
                    // AUTO LOGGING LOGIC
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastLogTime >= AUTO_LOG_INTERVAL) {
                        logCurrentReading(currentTime);
                        lastLogTime = currentTime;
                    }

                    _error.setValue(null);
                    scheduleNextFetch(); 
                } else {
                    _error.setValue("Invalid response from ESP32");
                    isPolling = false; 
                }
            }

            @Override
            public void onFailure(@NonNull Call<Esp32Data> call, @NonNull Throwable t) {
                _loading.setValue(false);
                _error.setValue("ESP32 Device Not Found. Please connect to AtiCao WiFi.");
                isPolling = false; 
            }
        });
    }

    public void logCurrentReading(long timestamp) {
        Esp32Data current = _data.getValue();
        if (current != null) {
            String date = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(new Date(timestamp));
            SensorHistory log = new SensorHistory(
                    current.getTemperature(),
                    current.getHumidity(),
                    current.getSoilMoistureRaw(),
                    current.getSoilStatus(),
                    date,
                    deviceName,
                    timestamp
            );
            AppDatabase.getDatabase(getApplication()).sensorHistoryDao().insert(log);
        }
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
