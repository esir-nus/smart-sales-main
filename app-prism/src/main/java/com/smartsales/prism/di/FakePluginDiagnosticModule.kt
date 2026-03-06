package com.smartsales.prism.di

import com.smartsales.prism.data.fakes.plugins.FakeConfigurablePlugin
import com.smartsales.prism.domain.analyst.AnalystTool
import com.smartsales.prism.domain.analyst.PrismPlugin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet

/**
 * Provides fake diagnostic plugins dynamically to the RealToolRegistry via Dagger multibinding.
 * This ensures the LLM has a diverse set of real-world "tools" to choose from during L2 testing
 * without polluting production plugin code.
 * 
 * NOTE: For production, this module should either be moved to the test source set,
 * or guarded by a specific BuildFlavor/DEBUG flag.
 */
@Module
@InstallIn(SingletonComponent::class)
object FakePluginDiagnosticModule {

    @Provides
    @ElementsIntoSet
    fun provideFakeDiagnosticPlugins(): Set<PrismPlugin> {
        return setOf(
            // 01: Executive Report
            FakeConfigurablePlugin(
                metadata = AnalystTool(
                    id = "EXECUTIVE_REPORT_01",
                    icon = "📈",
                    label = "高管战略报告",
                    description = "生成针对C-Level的高价值、大盘战略导向总结",
                    requiredParams = mapOf("timeframe" to "string (e.g. Q1, FY2026)")
                )
            ),
            
            // 02: CRM CSV
            FakeConfigurablePlugin(
                metadata = AnalystTool(
                    id = "CRM_CSV_02",
                    icon = "📁",
                    label = "导出数据CSV",
                    description = "将底层CRM明细数据导出为电子表格供一线员工核查",
                    requiredParams = mapOf("reportType" to "string (e.g., 'sales', 'performance')")
                )
            ),
            
            // 03: Negotiation Simulator
            FakeConfigurablePlugin(
                metadata = AnalystTool(
                    id = "NEGOTIATION_SIMULATOR_03",
                    icon = "🎭",
                    label = "谈判模拟器",
                    description = "开启情景模拟模式，与AI进行沙盘推演和话术特训",
                    requiredParams = mapOf("clientPersona" to "string", "objectionType" to "string")
                )
            ),
            
            // 04: Annual Report
            FakeConfigurablePlugin(
                metadata = AnalystTool(
                    id = "ANNUAL_REPORT_04",
                    icon = "📋",
                    label = "年度总结报告",
                    description = "生成包含全年业绩回顾和财务概览的深度长篇报告",
                    requiredParams = mapOf("year" to "string", "department" to "string")
                )
            ),
            
            // 05: Daily Report
            FakeConfigurablePlugin(
                metadata = AnalystTool(
                    id = "DAILY_REPORT_05",
                    icon = "📝",
                    label = "每日工作简报",
                    description = "生成轻量级的当日待办与简要回顾",
                    requiredParams = mapOf("date" to "string")
                )
            )
        )
    }
}
