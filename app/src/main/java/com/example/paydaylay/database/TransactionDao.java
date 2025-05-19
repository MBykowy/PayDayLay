// app/src/main/java/com/example/paydaylay/database/TransactionDao.java
package com.example.paydaylay.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId")
    List<TransactionEntity> getTransactionsByUser(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TransactionEntity> transactions);

    @Query("DELETE FROM transactions WHERE userId = :userId")
    void deleteAllByUser(String userId);
}