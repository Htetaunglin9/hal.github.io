package com.hal.carlistmanager

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "car_entries")
data class CarEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carNo: String,
    val date: String,
    val wheels: Int,
    val road: String,
    val fee: Int,
    val listType: String
)