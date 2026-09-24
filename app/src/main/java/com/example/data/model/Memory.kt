package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val text: String,
    val deadline: String? = null,
    val category: String = "College",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
