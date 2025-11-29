package com.example.calculatrice;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ParticipationDao {

    @Insert
    long insert(ParticipationEntity participation);

    @Query("SELECT COUNT(*) FROM participations WHERE eventId = :eventId")
    int countForEvent(long eventId);

    @Query("SELECT * FROM participations WHERE eventId = :eventId ORDER BY createdAt DESC")
    List<ParticipationEntity> getForEvent(long eventId);

    @Query("SELECT * FROM participations WHERE id = :id LIMIT 1")
    ParticipationEntity getById(long id);

    @Query("UPDATE participations SET status = :status, qrCodeData = :qrCodeData, decisionAt = :decisionAt WHERE id = :id")
    void updateStatus(long id, String status, String qrCodeData, long decisionAt);
}

