package com.hal.carlistmanager

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
@Dao
interface CarEntryDao {
    @Insert
    suspend fun insert(entry: CarEntry)

    @Query("SELECT * FROM car_entries")
    suspend fun getAll(): List<CarEntry>
    @Query("SELECT listType, SUM(fee) as totalFee FROM car_entries GROUP BY listType")
    suspend fun getTotalFeesByListType(): List<ListTypeSummary>
    @Query("SELECT COUNT(*) FROM car_entries WHERE LOWER(listType) = LOWER(:type)")
    suspend fun getCountByListType(type: String): Int
    @Query("SELECT * FROM car_entries WHERE LOWER(listType) = LOWER(:type) ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun getEntriesByTypePaged(type: String, limit: Int, offset: Int): List<CarEntry>


    @Insert
    suspend fun insertAll(entries: List<CarEntry>)
    @Query("SELECT carNo FROM car_entries")
    suspend fun getAllCarNos(): List<String>
    @Delete
    suspend fun delete(entry: CarEntry)
    @Query("SELECT * FROM car_entries WHERE listType = :type AND date BETWEEN :fromDate AND :toDate ORDER BY id")
    suspend fun getByTypeAndDate(type: String, fromDate: String, toDate: String): List<CarEntry>


}
