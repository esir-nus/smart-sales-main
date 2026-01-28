import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { RefreshCw, Smartphone, Zap, AlertCircle, X, Download } from 'lucide-react';

// Types
type ConnectivityState = 'connected' | 'disconnected' | 'searching' | 'failed' | 'firmware' | 'wifi_mismatch';

interface ConnectivityModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export const ConnectivityModal: React.FC<ConnectivityModalProps> = ({ isOpen, onClose }) => {
    const [state, setState] = useState<ConnectivityState>('connected');
    
    // Reset to "Connected" or "Searching" when opened
    useEffect(() => {
        if (isOpen) {
            // Mock: 80% chance of being connected
            setState('connected');
        }
    }, [isOpen]);

    // Mock Search Logic
    useEffect(() => {
        if (state === 'searching') {
            const timer = setTimeout(() => {
                 // 80% Success Rate
                 if (Math.random() > 0.2) {
                     // NEW LOGIC: 30% chance of WiFi Mismatch
                     if (Math.random() > 0.7) {
                         setState('wifi_mismatch');
                     } else {
                         setState('connected');
                     }
                 } else {
                     setState('failed');
                 }
            }, 3000);
            return () => clearTimeout(timer);
        }
    }, [state]);

    // Backdrop Click
    const handleBackdropClick = (e: React.MouseEvent) => {
         if (e.target === e.currentTarget) {
            onClose();
        }
    };

    // --- State Components ---

    const ConnectedView = () => (
        <div className="text-center">
            {/* 3D Badge Twin */}
            <div className="w-32 h-32 mx-auto mb-6 relative">
                 <div className="absolute inset-0 rounded-full bg-green-500/10 animate-pulse" />
                 <div className="w-full h-full bg-slate-800 rounded-2xl flex items-center justify-center border border-slate-700 relative z-10 shadow-xl">
                    <div className="w-8 h-8 rounded-full bg-black border-2 border-slate-600 relative overflow-hidden">
                        <div className="absolute inset-0 bg-green-500/20" />
                        <div className="absolute bottom-0 left-0 right-0 bg-green-500 h-[85%]" />
                    </div>
                 </div>
                 {/* Live Status Badge */}
                 <div className="absolute -bottom-2 -right-2 bg-green-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full border-2 border-slate-900 shadow-sm flex items-center gap-1">
                    <Zap size={8} fill="currentColor" /> 85%
                 </div>
            </div>

            <h3 className="text-lg font-bold text-gray-900">SmartBadge Pro</h3>
            <p className="text-sm text-gray-500 mb-6">ID: 8842 • 固件 v1.2.0</p>

            <div className="grid grid-cols-2 gap-3">
                 <button 
                    onClick={() => setState('disconnected')}
                    className="p-3 rounded-xl bg-red-50 hover:bg-red-100 text-red-500 text-sm font-medium transition-colors flex flex-col items-center gap-2 border border-red-100"
                 >
                    <Zap size={18} />
                    <span>断开连接</span>
                 </button>
                 <button 
                    onClick={() => setState('firmware')}
                    className="p-3 rounded-xl bg-blue-50 hover:bg-blue-100 text-blue-600 text-sm font-medium transition-colors flex flex-col items-center gap-2 border border-blue-100"
                 >
                    <RefreshCw size={18} />
                    <span>检查更新</span>
                 </button>
            </div>
        </div>
    );

    const DisconnectedView = () => (
        <div className="text-center">
             <div className="w-32 h-32 mx-auto mb-6 relative grayscale opacity-60">
                 <div className="w-full h-full bg-slate-800 rounded-2xl flex items-center justify-center border border-slate-700 shadow-inner">
                    <div className="w-8 h-8 rounded-full bg-black border-2 border-slate-600" />
                 </div>
            </div>
            
            <h3 className="text-lg font-bold text-gray-900">SmartBadge Pro</h3>
            <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-red-100 text-red-600 text-xs font-bold mb-6">
                <div className="w-1.5 h-1.5 rounded-full bg-red-500" /> 离线
            </div>

            <button 
                onClick={() => setState('searching')}
                className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-medium transition-colors shadow-sm active:scale-95 transform"
            >
                连接设备
            </button>
        </div>
    );

    const SearchingView = () => (
        <div className="text-center py-4">
             <div className="relative w-32 h-32 mx-auto mb-8 flex items-center justify-center">
                <motion.div className="absolute inset-0 border border-blue-500/30 rounded-full" animate={{ scale: [0.8, 1.8], opacity: [1, 0] }} transition={{ duration: 2, repeat: Infinity }} />
                <motion.div className="absolute inset-0 border border-blue-500/30 rounded-full" animate={{ scale: [0.8, 1.8], opacity: [1, 0] }} transition={{ duration: 2, repeat: Infinity, delay: 0.5 }} />
                <div className="w-16 h-16 bg-blue-500/10 rounded-full flex items-center justify-center animate-pulse">
                    <Smartphone size={24} className="text-blue-500" />
                </div>
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-1">正在搜索...</h3>
            <p className="text-sm text-gray-500">尝试连接并验证网络环境</p>
            
            <button onClick={() => setState('disconnected')} className="mt-8 text-sm text-gray-400 hover:text-gray-600">取消</button>
        </div>
    );

    const FailedView = () => (
        <div className="text-center py-4">
            <div className="w-16 h-16 mx-auto mb-4 bg-red-50 rounded-full flex items-center justify-center text-red-500">
                <AlertCircle size={32} />
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-2">未发现设备</h3>
            <p className="text-sm text-gray-500 mb-6 max-w-[200px] mx-auto">
                请确认设备已开机并在范围内。
            </p>
             <button 
                onClick={() => setState('searching')}
                className="w-full py-3 bg-gray-900 hover:bg-gray-800 text-white rounded-xl font-medium transition-colors"
            >
                重试
            </button>
        </div>
    );

    const WifiMismatchView = () => (
        <div className="text-center py-4">
             <div className="w-16 h-16 mx-auto mb-4 bg-amber-50 rounded-full flex items-center justify-center text-amber-500">
                <Zap size={32} />
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-2">网络环境已变更</h3>
            <p className="text-sm text-gray-500 mb-6 px-4">
                检测到徽章 WiFi 配置与当前手机网络 (Office_5G) 不匹配。
            </p>
            
            <div className="bg-gray-50 p-4 rounded-xl mb-6 text-left space-y-3">
                 <div>
                    <label className="text-xs text-gray-500 font-medium block mb-1">WiFi 名称 (SSID)</label>
                    <input 
                        type="text" 
                        defaultValue="Office_5G"
                        className="w-full bg-white border border-gray-200 rounded-lg px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                    />
                 </div>
                 <div>
                    <label className="text-xs text-gray-500 font-medium block mb-1">WiFi 密码</label>
                    <input 
                        type="password" 
                        defaultValue="12345678"
                        className="w-full bg-white border border-gray-200 rounded-lg px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                    />
                 </div>
            </div>

            <div className="flex gap-3">
                 <button 
                    onClick={() => setState('connected')}
                    className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-600 rounded-xl font-medium transition-colors"
                 >
                    忽略
                </button>
                <button 
                    onClick={() => setState('connected')}
                    className="flex-1 py-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-medium transition-colors shadow-sm"
                >
                    更新配置
                </button>
            </div>
        </div>
    );

    const FirmwareView = () => (
        <div className="text-center py-4">
             <div className="w-16 h-16 mx-auto mb-4 bg-purple-50 rounded-full flex items-center justify-center text-purple-600">
                <Download size={32} />
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-1">发现新版本</h3>
            <p className="text-sm text-gray-500 mb-6">版本 1.3 • 包含重要安全修复</p>
            
            <button 
                onClick={() => {
                    // Mock update
                    const btn = document.getElementById('update-btn');
                    if(btn) btn.innerText = "下载中...";
                    setTimeout(() => {
                        if(btn) btn.innerText = "安装中...";
                        setTimeout(() => setState('connected'), 1500);
                    }, 1500);
                }}
                id="update-btn"
                className="w-full py-3 bg-purple-600 hover:bg-purple-500 text-white rounded-xl font-medium transition-colors"
            >
                立即同步
            </button>
        </div>
    );

    return (
        <AnimatePresence>
            {isOpen && (
                <div 
                    className="absolute inset-0 z-50 flex items-end justify-center sm:items-center px-4 pb-4 sm:pb-0"
                    onClick={handleBackdropClick}
                >
                    {/* Backdrop */}
                    <motion.div 
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
                    />

                    {/* Modal Content */}
                    <motion.div
                        initial={{ y: '100%', opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        exit={{ y: '100%', opacity: 0 }}
                        transition={{ type: 'spring', damping: 25, stiffness: 300 }}
                        className="bg-white relative w-full max-w-sm rounded-[32px] overflow-hidden shadow-2xl"
                    >
                         {/* Close Button Header */}
                         <div className="absolute top-4 right-4 z-20">
                            <button onClick={onClose} className="p-2 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-500 transition-colors">
                                <X size={16} />
                            </button>
                         </div>

                         <div className="p-8">
                            <AnimatePresence mode="wait">
                                <motion.div
                                    key={state}
                                    initial={{ opacity: 0, x: 20 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: -20 }}
                                    transition={{ duration: 0.2 }}
                                >
                                    {state === 'connected' && <ConnectedView />}
                                    {state === 'disconnected' && <DisconnectedView />}
                                    {state === 'searching' && <SearchingView />}
                                    {state === 'failed' && <FailedView />}
                                    {state === 'firmware' && <FirmwareView />}
                                    {state === 'wifi_mismatch' && <WifiMismatchView />}
                                </motion.div>
                            </AnimatePresence>
                         </div>
                    </motion.div>
                </div>
            )}
        </AnimatePresence>
    );
};
