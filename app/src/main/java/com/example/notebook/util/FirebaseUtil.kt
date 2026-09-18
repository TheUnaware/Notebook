package com.example.notebook.util

import com.google.firebase.database.FirebaseDatabase

object FirebaseUtil {
    private const val DB_URL = "https://notebook-bf3d2-default-rtdb.asia-southeast1.firebasedatabase.app"

    val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(DB_URL)
    }
}