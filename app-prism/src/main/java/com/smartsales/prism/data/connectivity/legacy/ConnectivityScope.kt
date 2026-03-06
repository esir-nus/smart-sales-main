package com.smartsales.prism.data.connectivity.legacy

import javax.inject.Qualifier

/**
 * 连接模块专属的 CoroutineScope 标记
 * 
 * 用于在依赖注入图中标识连接模块的共享作用域。
 * 该作用域使用 SupervisorJob，确保单个协程失败不会影响其他协程。
 * 
 * 生产环境：由 Hilt 提供 @Singleton 范围的作用域
 * 测试环境：注入 TestScope 以自动取消所有子协程
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConnectivityScope
