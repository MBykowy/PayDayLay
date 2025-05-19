// app/src/main/java/com/example/paydaylay/database/CategoryDao.java
package com.example.paydaylay.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId")
    List<CategoryEntity> getCategoriesByUser(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoryEntity> categories);

    @Query("DELETE FROM categories WHERE userId = :userId")
    void deleteAllByUser(String userId);
}