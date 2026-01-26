package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.tools.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeBleConnector : BleConnector {
    
    private val _events = MutableSharedFlow<BleEvent>(replay = 1)
    override val events: Flow<BleEvent> = _events.asSharedFlow()
    
    private var _isConnected = false
    override val isConnected: Boolean get() = _isConnected
    
    override suspend fun connect(): Boolean {
        _isConnected = true
        _events.emit(BleEvent.Connected)
        return true
    }
    
    override suspend fun disconnect() {
        _isConnected = false
        _events.emit(BleEvent.Disconnected)
    }
    
    /**
     * 测试辅助方法：模拟文件就绪事件
     */
    suspend fun simulateFileReady(filename: String, sizeBytes: Long) {
        _events.emit(BleEvent.FileAvailable(BleFileInfo(filename, sizeBytes)))
    }
}
