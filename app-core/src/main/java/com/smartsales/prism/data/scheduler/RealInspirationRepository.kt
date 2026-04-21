package com.smartsales.prism.data.scheduler

import android.content.Context
import android.content.SharedPreferences
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 灵感仓库实现 — SharedPreferences 后端
 * 
 * 灵感是轻量级全局数据，不需要复杂查询，使用 SharedPreferences 足够
 */
@Singleton
class RealInspirationRepository private constructor(
    private val prefs: SharedPreferences
) : InspirationRepository {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    internal constructor(prefs: SharedPreferences, @Suppress("UNUSED_PARAMETER") testOnly: Boolean = true) :
        this(prefs)

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    
    // 内存缓存 + Flow 发射
    private val _inspirations = MutableStateFlow<List<InspirationData>>(emptyList())
    
    init {
        // 启动时从 SharedPreferences 加载
        loadFromPrefs()
    }
    
    override suspend fun insert(text: String): String {
        val normalizedText = text.trim()
        val id = UUID.randomUUID().toString()
        val now = Instant.now()
        val newItem = InspirationData(
            id = id,
            text = normalizedText,
            createdAt = now.toEpochMilli()
        )
        
        val current = _inspirations.value.toMutableList()
        current.add(0, newItem) // 最新在前
        _inspirations.value = current
        saveToPrefs()
        
        return id
    }
    
    override fun getAll(): Flow<List<SchedulerTimelineItem.Inspiration>> {
        return _inspirations.asStateFlow().map { list ->
            list.map { it.toModel() }
        }
    }
    
    override suspend fun delete(id: String) {
        val current = _inspirations.value.filter { it.id != id }
        _inspirations.value = current
        saveToPrefs()
    }
    
    // ========== Persistence ==========
    
    private fun loadFromPrefs() {
        val json = prefs.getString(KEY_INSPIRATIONS, "[]") ?: "[]"
        try {
            val array = JSONArray(json)
            val items = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                InspirationData(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    createdAt = obj.getLong("createdAt")
                )
            }
            _inspirations.value = items
        } catch (e: Exception) {
            _inspirations.value = emptyList()
        }
    }
    
    private fun saveToPrefs() {
        val array = JSONArray()
        _inspirations.value.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("createdAt", item.createdAt)
            })
        }
        prefs.edit().putString(KEY_INSPIRATIONS, array.toString()).apply()
    }
    
    private fun InspirationData.toModel(): SchedulerTimelineItem.Inspiration {
        return SchedulerTimelineItem.Inspiration(
            id = id,
            timeDisplay = timeFormatter.format(Instant.ofEpochMilli(createdAt)),
            title = text
        )
    }
    
    companion object {
        private const val PREFS_NAME = "prism_inspirations"
        private const val KEY_INSPIRATIONS = "items"
    }
}

/**
 * 内部数据类 — 序列化用
 */
private data class InspirationData(
    val id: String,
    val text: String,
    val createdAt: Long
)
