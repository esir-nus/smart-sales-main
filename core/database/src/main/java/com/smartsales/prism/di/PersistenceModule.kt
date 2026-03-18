package com.smartsales.prism.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsales.prism.data.persistence.PrismDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

/**
 * Room 持久化 DI 模块
 * 
 * 提供 PrismDatabase 单例 + 首次创建时种子真实记忆
 * DAOs 由各 Repository 内部通过 db.xxxDao() 获取
 */
@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {
    
    @Provides
    @Singleton
    fun providePrismDatabase(@ApplicationContext context: Context): PrismDatabase {
        return Room.databaseBuilder(
            context,
            PrismDatabase::class.java,
            "prism_database"
        )
            .fallbackToDestructiveMigrationFrom(1)  // v1→v2: CalendarProvider → Room
            .addMigrations(
                PrismDatabase.MIGRATION_2_3,
                PrismDatabase.MIGRATION_3_4,
                PrismDatabase.MIGRATION_4_5,
                PrismDatabase.MIGRATION_5_6,
                PrismDatabase.MIGRATION_6_7,
                PrismDatabase.MIGRATION_7_8
            )
            .addCallback(SeedMemoryCallback)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideScheduledTaskDao(database: PrismDatabase) = database.scheduledTaskDao()

    @Provides
    @Singleton
    fun provideSessionDao(database: PrismDatabase) = database.sessionDao()

    @Provides
    @Singleton
    fun provideMessageDao(database: PrismDatabase) = database.messageDao()
    
    /**
     * 首次建库时插入真实业务记忆 — 非测试数据
     * 
     * 这些是真实的业务活动记录，用于:
     * 1. 用户首次启动即有记忆可检索
     * 2. L2 测试验证记忆读取路径 (LIKE 子串匹配)
     */
    private object SeedMemoryCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("PrismDB", "🌱 首次建库 — 正在种入真实记忆...")
            
            val now = System.currentTimeMillis()
            val feb8 = 1738944000000L  // 2026-02-08 00:00 UTC+8 近似
            val feb5 = 1738684800000L  // 2026-02-05 00:00 UTC+8 近似
            
            val seeds = listOf(
                // 记忆1: 台州大学ameer教授来访调试
                arrayOf(
                    "seed-memory-001",
                    "seed-session-init",
                    "台州大学的ameer教授和他弟弟2月8日来摩升泰公司调试最新的桌面机械臂代码，蔡瑞江，孙扬浩对接，协助相关调试工作。" +
                    "有以下主要待办事项：1. yolo模型需要优化训练，将书籍封面剔除在外，只识别打开的书籍。" +
                    "2. 新的音响麦克风套件表现稳定。3.阿里多模态目前迭代迅速，不太稳定，会时常出现网络问题导致任务失败。" +
                    "4.阿里作业模式的相关服务不够完善，例如模型单次调用智能识别文字，或者图形。有图形就会优先识别图形而忽略文字。" +
                    "蔡瑞江提议先将作业用户群体限制在小学4年级或以下，加强亲子互动，家庭关系构建可能更加实用。" +
                    "5.阿里的多模态调试页面还不够完善，等待更新。",
                    "USER_MESSAGE",
                    feb8, feb8, 0, null, null
                ),
                // 记忆2: 智能助理产品定位
                arrayOf(
                    "seed-memory-002",
                    "seed-session-init",
                    "承时利和公司孙扬浩孙工当前销售智能助理产品已处于打磨阶段，功能主要定位是全方位智能助手，亮点包涵：" +
                    "1.长期记忆系统，能智能调用记忆库，知识库，文件库；" +
                    "2.成长型智能体，会随着使用加深对于用户和客户的理解；" +
                    "3.智能行程管理，智能安排用户行程，智能提醒，解决事件冲突；" +
                    "4.智能专家，随着和用户的沟通，不断推荐最有用的业务工具，例如年报，日报，pdf报告，思维导图，数据分析等",
                    "USER_MESSAGE",
                    feb5, feb5, 0, null, null
                ),
                // 记忆3: 江西大学Ata教授照片需求
                arrayOf(
                    "seed-memory-003",
                    "seed-session-init",
                    "江西大学Ata教授上周4再次询问模型训练用照片，等待安排；一月28号30号也曾反复询问",
                    "USER_MESSAGE",
                    feb5, feb5, 0, null, null
                )
            )
            
            for (seed in seeds) {
                db.execSQL(
                    """INSERT OR IGNORE INTO memory_entries 
                       (entryId, sessionId, content, entryType, createdAt, updatedAt, isArchived, scheduledAt, structuredJson) 
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    seed
                )
            }
            
            Log.d("PrismDB", "🌱 种入 ${seeds.size} 条真实记忆完成")

            // 种入已完成任务 — L2 紧急度排序测试
            val feb12_9am = 1739322000000L   // 2026-02-12 09:00 UTC+8
            val feb12_12pm = 1739332800000L  // 2026-02-12 12:00 UTC+8
            val feb11_14pm = 1739253600000L  // 2026-02-11 14:00 UTC+8
            val feb11_15pm = 1739257200000L  // 2026-02-11 15:00 UTC+8

            // L1_URGENT 已完成任务
            db.execSQL(
                """INSERT OR IGNORE INTO scheduled_tasks 
                   (taskId, title, startTimeMillis, endTimeMillis, durationMinutes, durationSource, conflictPolicy, location, notes, keyPerson, keyPersonEntityId, highlights, isDone, hasAlarm, isSmartAlarm, alarmCascadeJson, urgencyLevel) 
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                arrayOf(
                    "seed-task-001",
                    "完成SmartSales应用架构搭建",
                    feb12_9am, feb12_12pm, 180,
                    "ESTIMATED", "FLEXIBLE",
                    null, null, null, null, null,
                    1, 0, 0, null, "L1_URGENT"
                )
            )

            // L2_IMPORTANT 已完成任务
            db.execSQL(
                """INSERT OR IGNORE INTO scheduled_tasks 
                   (taskId, title, startTimeMillis, endTimeMillis, durationMinutes, durationSource, conflictPolicy, location, notes, keyPerson, keyPersonEntityId, highlights, isDone, hasAlarm, isSmartAlarm, alarmCascadeJson, urgencyLevel) 
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                arrayOf(
                    "seed-task-002",
                    "完成公司年度会议",
                    feb11_14pm, feb11_15pm, 60,
                    "ESTIMATED", "FLEXIBLE",
                    null, null, null, null, null,
                    1, 0, 0, null, "L2_IMPORTANT"
                )
            )

            Log.d("PrismDB", "🌱 种入 2 条已完成任务 (L1 + L2)")
        }
    }
}
