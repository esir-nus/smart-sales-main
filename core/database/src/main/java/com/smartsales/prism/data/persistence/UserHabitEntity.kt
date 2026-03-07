package com.smartsales.prism.data.persistence

import androidx.room.Entity
import androidx.room.Index


/**
 * Room 实体 — 用户习惯存储
 * 
 * 复合主键: (habitKey, entityId)
 * - 全局习惯: entityId = null
 * - 实体特定习惯: entityId = "c-001", etc.
 * 
 * Wave 1.5 schema: 4-rule weighting model
 */
@Entity(
    tableName = "user_habits",
    primaryKeys = ["habitKey", "entityIdOrEmpty"],
    indices = [Index(value = ["entityIdOrEmpty"])]
)
data class UserHabitEntity(
    val habitKey: String,
    val habitValue: String,
    
    // Room 不支持 nullable 复合主键，用空字符串代表 null
    val entityIdOrEmpty: String,  // "" for global, else entityId
    
    // Rule 1: Frequency
    val inferredCount: Int,
    
    // Rule 2: Explicit positive
    val explicitPositive: Int,
    
    // Rule 3: Explicit negative
    val explicitNegative: Int,
    
    // Rule 4: Recency
    val lastObservedAt: Long,
    val createdAt: Long
)

