package com.tradingai.mobile.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.tradingai.mobile.data.model.UserProfile;

public class FirestoreRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void createOrUpdateUser(@NonNull UserProfile profile) {
        db.collection("users").document(profile.userId).set(profile);
    }

    public void setInvestmentToggle(boolean enabled) {
        db.collection("settings").document("investment").update("enabled", enabled);
    }
}
