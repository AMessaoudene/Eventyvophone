package com.example.calculatrice;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventDao {

    @Insert
    long insert(EventEntity event);

    @Query("SELECT * FROM events ORDER BY id DESC")
    List<EventEntity> getAll();

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    EventEntity getEventById(long eventId);

    @Query("SELECT * FROM events WHERE organizerId = :userId ORDER BY id DESC")
    List<EventEntity> getEventsForUser(long userId);

    @Query("DELETE FROM events WHERE id = :eventId")
    void delete(long eventId);

    @Update
    void update(EventEntity event);
}
