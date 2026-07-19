package com.example.schulplaner

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {
    fun isInitialized(): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAuth(): FirebaseAuth? {
        return if (isInitialized()) FirebaseAuth.getInstance() else null
    }

    fun getDb(): FirebaseFirestore? {
        return if (isInitialized()) FirebaseFirestore.getInstance() else null
    }
}
