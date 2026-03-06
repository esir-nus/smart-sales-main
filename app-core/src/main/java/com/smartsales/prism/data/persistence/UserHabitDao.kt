package com.smartsales.prism.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * UserHabit DAO — 用户习惯 CRUD
 */
@Dao
interface UserHabitDao {
    /**
     * 获取全局习惯 (entityIdOrEmpty = "")
     */
    @Query("SELECT * FROM user_habits WHERE entityIdOrEmpty = ''")
    suspend fun getGlobalHabits(): List<UserHabitEntity>
    
    /**
     * 获取特定实体的习惯
     */
    @Query("SELECT * FROM user_habits WHERE entityIdOrEmpty = :entityId")
    suspend fun getByEntity(entityId: String): List<UserHabitEntity>
    
    /**
     * 获取特定习惯 (复合主键查询)
     */
    @Query("SELECT * FROM user_habits WHERE habitKey = :key AND entityIdOrEmpty = :entityIdOrEmpty LIMIT 1")
    suspend fun getHabit(key: String, entityIdOrEmpty: String): UserHabitEntity?
    
    /**
     * 插入/更新习惯 (REPLACE on composite key conflict)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: UserHabitEntity)
    
    /**
     * 原子递增 — 推断观察 (消除 read-modify-write 竞态)
     * @return 受影响行数 (0 = 不存在, 需要 insert)
     */
    @Query("UPDATE user_habits SET inferredCount = inferredCount + 1, lastObservedAt = :now WHERE habitKey = :key AND entityIdOrEmpty = :entityIdOrEmpty")
    suspend fun incrementInferred(key: String, entityIdOrEmpty: String, now: Long): Int

    /**
     * 原子递增 — 用户正向确认
     */
    @Query("UPDATE user_habits SET explicitPositive = explicitPositive + 1, lastObservedAt = :now WHERE habitKey = :key AND entityIdOrEmpty = :entityIdOrEmpty")
    suspend fun incrementPositive(key: String, entityIdOrEmpty: String, now: Long): Int

    /**
     * 原子递增 — 用户负向反馈
     */
    @Query("UPDATE user_habits SET explicitNegative = explicitNegative + 1, lastObservedAt = :now WHERE habitKey = :key AND entityIdOrEmpty = :entityIdOrEmpty")
    suspend fun incrementNegative(key: String, entityIdOrEmpty: String, now: Long): Int

    /**
     * 删除习惯
     */
    @Query("DELETE FROM user_habits WHERE habitKey = :key AND entityIdOrEmpty = :entityIdOrEmpty")
    suspend fun delete(key: String, entityIdOrEmpty: String)
    
    /**
     * Test helper: 清空所有数据
     */
    @Query("DELETE FROM user_habits")
    suspend fun deleteAll()
}
