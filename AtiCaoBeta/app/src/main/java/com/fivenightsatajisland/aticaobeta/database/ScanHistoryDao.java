package com.fivenightsatajisland.aticaobeta.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY id DESC")
    List<ScanHistory> getAll();

    @Insert
    void insert(ScanHistory scanHistory);

    @Query("DELETE FROM scan_history")
    void deleteAll();
}