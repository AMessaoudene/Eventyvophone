package com.example.calculatrice;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class FirestoreHelper {

    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void addUser(String uid, String username, String email, OnComplete<Void> callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("uid", uid);
        user.put("password", ""); // Place holder for shadow auth

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void updatePassword(String uid, String newPassword, OnComplete<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("password", newPassword);
        
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getUser(String uid, OnComplete<User> callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");
                        User user = new User(username, "", email);
                        // We might need to store UID in User object if we change ID type
                        // For now, we just return the data
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserByEmail(String email, OnComplete<String> callback) {
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String uid = queryDocumentSnapshots.getDocuments().get(0).getId();
                        callback.onSuccess(uid);
                    } else {
                        callback.onFailure(new Exception("Email not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Event Operations

    public void addEvent(EventEntity event, OnComplete<Void> callback) {
        String id = db.collection("events").document().getId();
        event.id = id;
        db.collection("events").document(id)
                .set(event)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getEvents(OnComplete<List<EventEntity>> callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EventEntity> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        EventEntity event = document.toObject(EventEntity.class);
                        events.add(event);
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getAllUsers(OnComplete<Map<String, String>> callback) {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, String> userEmails = new HashMap<>(); // UID -> Email
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String email = document.getString("email");
                        if (email != null) {
                            userEmails.put(document.getId(), email);
                        }
                    }
                    callback.onSuccess(userEmails);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getEventsByOrganizer(String organizerId, OnComplete<List<EventEntity>> callback) {
        db.collection("events").whereEqualTo("organizerId", organizerId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EventEntity> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        EventEntity event = document.toObject(EventEntity.class);
                        events.add(event);
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateEvent(EventEntity event, OnComplete<Void> callback) {
        db.collection("events").document(event.id)
                .set(event)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteEvent(String eventId, OnComplete<Void> callback) {
        db.collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    // Participation Operations

    public void addParticipation(ParticipationEntity participation, OnComplete<Void> callback) {
        String id = db.collection("participations").document().getId();
        participation.id = id;
        db.collection("participations").document(id)
                .set(participation)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void countParticipations(String eventId, OnComplete<Integer> callback) {
        db.collection("participations").whereEqualTo("eventId", eventId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> callback.onSuccess(queryDocumentSnapshots.size()))
                .addOnFailureListener(callback::onFailure);
    }

    public void getParticipations(String eventId, OnComplete<List<ParticipationEntity>> callback) {
        db.collection("participations").whereEqualTo("eventId", eventId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ParticipationEntity> participations = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ParticipationEntity participation = document.toObject(ParticipationEntity.class);
                        participations.add(participation);
                    }
                    callback.onSuccess(participations);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getParticipation(String participationId, OnComplete<ParticipationEntity> callback) {
        db.collection("participations").document(participationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ParticipationEntity participation = documentSnapshot.toObject(ParticipationEntity.class);
                        callback.onSuccess(participation);
                    } else {
                        callback.onFailure(new Exception("Participation not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateParticipation(ParticipationEntity participation, OnComplete<Void> callback) {
        db.collection("participations").document(participation.id)
                .set(participation)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    // Notification Operations

    public void addNotification(NotificationEntity notification, OnComplete<Void> callback) {
        String id = db.collection("notifications").document().getId();
        notification.id = id;
        db.collection("notifications").document(id)
                .set(notification)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserNotifications(String userId, OnComplete<List<NotificationEntity>> callback) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<NotificationEntity> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NotificationEntity notification = document.toObject(NotificationEntity.class);
                        notifications.add(notification);
                    }
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface OnComplete<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
