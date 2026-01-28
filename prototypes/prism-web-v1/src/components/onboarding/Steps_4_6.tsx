import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Smartphone } from 'lucide-react';

// --- Step 4: Hardware Wake ---
export const WakeStep: React.FC<{ onNext: () => void }> = ({ onNext }) => {
    // Auto advance for demo purposes
    useEffect(() => { setTimeout(onNext, 5000); }, [onNext]);

    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="text-center">
            <div className="w-32 h-32 mx-auto mb-8 relative">
                 <motion.div 
                    className="absolute inset-0 rounded-full bg-blue-500/20"
                    animate={{ scale: [1, 1.2, 1], opacity: [0.5, 0, 0.5] }}
                    transition={{ duration: 2, repeat: Infinity }}
                 />
                 <div className="w-full h-full bg-slate-800 rounded-2xl flex items-center justify-center border border-slate-700 relative z-10">
                    <div className="w-8 h-8 rounded-full bg-black border-2 border-slate-600 animate-pulse" />
                 </div>
                 {/* Finger Press Animation */}
                 <motion.div 
                    className="absolute top-1/2 left-1/2 w-12 h-12 rounded-full bg-white/10 border border-white/30"
                    initial={{ x: -24, y: -24, scale: 1.5, opacity: 0 }}
                    animate={{ scale: [1.5, 1], opacity: [0, 1, 0] }}
                    transition={{ duration: 1.5, repeat: Infinity }}
                 />
            </div>
            <h2 className="text-xl font-bold text-white mb-4">启动您的 SmartBadge</h2>
            <p className="text-slate-400 text-sm">长按中间按钮 3 秒，直到蓝灯闪烁</p>
            <button onClick={onNext} className="mt-8 text-blue-400 text-sm hover:text-blue-300">灯已经在闪了</button>
        </motion.div>
    );
};

// --- Step 5: Scan ---
export const ScanStep: React.FC<{ onNext: () => void }> = ({ onNext }) => {
    useEffect(() => { setTimeout(onNext, 3000); }, [onNext]);
    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="text-center">
            <div className="relative w-48 h-48 mx-auto mb-8 flex items-center justify-center">
                <motion.div className="absolute inset-0 border border-blue-500/30 rounded-full" animate={{ scale: [0.8, 1.5], opacity: [1, 0] }} transition={{ duration: 2, repeat: Infinity }} />
                <motion.div className="absolute inset-0 border border-blue-500/30 rounded-full" animate={{ scale: [0.8, 1.5], opacity: [1, 0] }} transition={{ duration: 2, repeat: Infinity, delay: 0.5 }} />
                <div className="w-4 h-4 bg-blue-500 rounded-full shadow-lg shadow-blue-500/50" />
            </div>
            <p className="text-slate-400">正在搜索设备...</p>
        </motion.div>
    );
};

// --- Step 6: Found (Manual Gate) ---
export const FoundStep: React.FC<{ onNext: () => void }> = ({ onNext }) => (
    <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="w-full max-w-xs">
        <h2 className="text-xl font-bold text-white mb-6 text-center">发现设备</h2>
        <div className="glass-card p-4 rounded-2xl bg-slate-800/50 border border-white/10">
            <div className="flex items-center gap-4">
                <div className="w-10 h-10 bg-slate-700 rounded-full flex items-center justify-center text-slate-300">
                    <Smartphone size={20} />
                </div>
                <div>
                     <h3 className="font-bold text-white text-sm">SmartBadge (Frank's)</h3>
                     <p className="text-xs text-green-400">ID: FF:23:44:A1</p>
                </div>
                <div className="ml-auto text-xs text-slate-500">-42dBm</div>
            </div>
            <button 
                onClick={onNext}
                className="w-full mt-4 py-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-medium transition-colors flex items-center justify-center gap-2"
            >
                连接
            </button>
        </div>
    </motion.div>
);
