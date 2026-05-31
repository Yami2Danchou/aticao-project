package com.fivenightsatajisland.aticaobeta.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ScanHistoryDao {
    //Added local scan history storage


    @Query("SELECT * FROM scan_history ORDER BY id DESC")
    List<ScanHistory> getAll();

    @Insert
    void insert(ScanHistory scanHistory);

    @Query("DELETE FROM scan_history WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM scan_history")
    void deleteAll();
}