package com.smartsales.data.prismlib.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartsales.domain.prism.core.Mode
import com.smartsales.domain.prism.core.entities.*

class Converters {
    private val gson = Gson()

    // Enums
    @TypeConverter
    fun fromMode(mode: Mode): String = mode.name

    @TypeConverter
    fun toMode(value: String): Mode = Mode.valueOf(value)

    @TypeConverter
    fun fromOutcomeStatus(status: OutcomeStatus?): String? = status?.name

    @TypeConverter
    fun toOutcomeStatus(value: String?): OutcomeStatus? = value?.let { OutcomeStatus.valueOf(it) }

    @TypeConverter
    fun fromEntityType(type: EntityType): String = type.name

    @TypeConverter
    fun toEntityType(value: String): EntityType = EntityType.valueOf(value)

    // Collections
    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }

    // Complex Objects
    @TypeConverter
    fun fromArtifactList(value: List<ArtifactMeta>): String = gson.toJson(value)

    @TypeConverter
    fun toArtifactList(value: String): List<ArtifactMeta> {
        val type = object : TypeToken<List<ArtifactMeta>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromAliasList(value: List<AliasMapping>): String = gson.toJson(value)

    @TypeConverter
    fun toAliasList(value: String): List<AliasMapping> {
        val type = object : TypeToken<List<AliasMapping>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromRelatedEntityList(value: List<RelatedEntity>): String = gson.toJson(value)

    @TypeConverter
    fun toRelatedEntityList(value: String): List<RelatedEntity> {
        val type = object : TypeToken<List<RelatedEntity>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromMetricHistory(value: Map<String, List<MetricPoint>>): String = gson.toJson(value)
    
    @TypeConverter
    fun toMetricHistory(value: String): Map<String, List<MetricPoint>> {
        val type = object : TypeToken<Map<String, List<MetricPoint>>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromDecisionLog(value: List<DecisionRecord>): String = gson.toJson(value)
    
    @TypeConverter
    fun toDecisionLog(value: String): List<DecisionRecord> {
        val type = object : TypeToken<List<DecisionRecord>>() {}.type
        return gson.fromJson(value, type)
    }

    // Task & Inspiration Enums
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = TaskPriority.valueOf(value)

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromInspirationSource(source: InspirationSource): String = source.name

    @TypeConverter
    fun toInspirationSource(value: String): InspirationSource = InspirationSource.valueOf(value)

    @TypeConverter
    fun fromAlarmType(type: AlarmType): String = type.name

    @TypeConverter
    fun toAlarmType(value: String): AlarmType = AlarmType.valueOf(value)

    @TypeConverter
    fun fromExperienceLevel(level: ExperienceLevel): String = level.name

    @TypeConverter
    fun toExperienceLevel(value: String): ExperienceLevel = ExperienceLevel.valueOf(value)
}
