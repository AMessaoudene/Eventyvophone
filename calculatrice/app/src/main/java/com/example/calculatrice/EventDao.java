package com.example.calculatrice;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

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

    @Query("UPDATE events SET name = :name, startDate = :startDate, endDate = :endDate," +
            " location = :location, meetLink = :meetLink, isOnline = :isOnline, isFree = :isFree," +
            " description = :description, hasParticipationForm = :hasParticipationForm, imageUri = :imageUri WHERE id = :id")
    void update(long id, String name, String startDate, String endDate,
                String location, String meetLink, boolean isOnline, boolean isFree,
                String description, boolean hasParticipationForm, String imageUri);
}
