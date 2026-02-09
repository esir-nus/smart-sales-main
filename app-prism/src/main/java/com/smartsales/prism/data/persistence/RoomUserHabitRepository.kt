package com.smartsales.prism.data.persistence

import android.util.Log
import com.smartsales.prism.domain.habit.UserHabit
import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.rl.ObservationSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 实现 — UserHabitRepository
 * 
 * 原子递增模式: UPDATE → check rows → INSERT if needed
 * 消除 read-modify-write 竞态条件
 */
@Singleton
class RoomUserHabitRepository @Inject constructor(
    private val db: PrismDatabase
) : UserHabitRepository {
    
    private val dao = db.userHabitDao()
    
    override suspend fun getGlobalHabits(): List<UserHabit> {
        return dao.getGlobalHabits().map { it.toDomain() }
    }
    
    override suspend fun getByEntity(entityId: String): List<UserHabit> {
        return dao.getByEntity(entityId).map { it.toDomain() }
    }
    
    override suspend fun getHabit(key: String, entityId: String?): UserHabit? {
        val entityIdOrEmpty = entityId ?: ""
        return dao.getHabit(key, entityIdOrEmpty)?.toDomain()
    }
    
    override suspend fun observe(
        key: String,
        value: String,
        entityId: String?,
        source: ObservationSource
    ) {
        val entityIdOrEmpty = entityId ?: ""
        val now = System.currentTimeMillis()
        
        // 原子递增: 直接 UPDATE，无需先 SELECT
        val rowsAffected = when (source) {
            ObservationSource.INFERRED -> dao.incrementInferred(key, entityIdOrEmpty, now)
            ObservationSource.USER_POSITIVE -> dao.incrementPositive(key, entityIdOrEmpty, now)
            ObservationSource.USER_NEGATIVE -> dao.incrementNegative(key, entityIdOrEmpty, now)
        }
        
        if (rowsAffected == 0) {
            // 行不存在，创建新习惯
            val base = UserHabitEntity(
                habitKey = key, habitValue = value, entityIdOrEmpty = entityIdOrEmpty,
                inferredCount = 0, explicitPositive = 0, explicitNegative = 0,
                lastObservedAt = now, createdAt = now
            )
            val entity = when (source) {
                ObservationSource.INFERRED -> base.copy(inferredCount = 1)
                ObservationSource.USER_POSITIVE -> base.copy(explicitPositive = 1)
                ObservationSource.USER_NEGATIVE -> base.copy(explicitNegative = 1)
            }
            dao.upsert(entity)
            Log.d("RoomHabit", "📝 新习惯: key=$key entity=${entityId ?: "global"} source=$source")
        } else {
            Log.d("RoomHabit", "📈 递增: key=$key entity=${entityId ?: "global"} source=$source")
        }
    }
    
    override suspend fun delete(key: String, entityId: String?) {
        val entityIdOrEmpty = entityId ?: ""
        dao.delete(key, entityIdOrEmpty)
        Log.d("RoomHabit", "🗑️ 删除: key=$key entity=${entityId ?: "global"}")
    }
}
