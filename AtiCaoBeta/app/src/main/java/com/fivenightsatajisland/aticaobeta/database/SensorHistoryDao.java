package com.fivenightsatajisland.aticaobeta.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SensorHistoryDao {
    @Query("SELECT * FROM sensor_history ORDER BY timestamp DESC")
    LiveData<List<SensorHistory>> getAll();

    @Query("SELECT * FROM sensor_history WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    LiveData<List<SensorHistory>> getRecent(long startTime);

    @Insert
    void insert(SensorHistory sensorHistory);

    @Query("DELETE FROM sensor_history")
    void deleteAll();

    @Query("DELETE FROM sensor_history WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM sensor_history WHERE deviceName = :deviceName")
    void deleteByDevice(String deviceName);
}
