import React from 'react';
import { motion } from 'framer-motion';
import { ChevronRight, Mic, Bluetooth } from 'lucide-react';

// --- Step 1: Welcome ---
export const WelcomeStep: React.FC<{ onNext: () => void }> = ({ onNext }) => (
    <motion.div 
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
        className="flex flex-col items-center max-w-xs text-center"
    >
        <motion.div 
            initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ delay: 0.2 }}
            className="w-24 h-24 rounded-3xl bg-gradient-to-br from-blue-500 to-indigo-600 shadow-xl shadow-blue-500/30 mb-8 flex items-center justify-center text-white"
        >
            <div className="text-4xl font-bold">S</div>
        </motion.div>
        <h1 className="text-3xl font-bold text-white mb-3 tracking-tight">SmartSales</h1>
        <p className="text-slate-400 mb-12 text-lg leading-relaxed">您的 AI 销售教练</p>
        <button onClick={onNext} className="w-full py-4 bg-white text-slate-900 rounded-2xl font-bold shadow-lg shadow-white/10 active:scale-95 transition-all flex items-center justify-center gap-2 group">
            开启旅程 <ChevronRight size={18} className="group-hover:translate-x-1 transition-transform" />
        </button>
    </motion.div>
);

// --- Step 2: Permissions ---
export const PermissionStep: React.FC<{ onNext: () => void }> = ({ onNext }) => (
    <motion.div 
        initial={{ opacity: 0, x: 50 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -50 }}
        className="w-full max-w-xs"
    >
        <div className="glass-card p-6 mb-4 border border-white/10 bg-white/5 backdrop-blur-xl rounded-3xl">
            <div className="flex items-center gap-4 mb-4">
                <div className="w-12 h-12 rounded-full bg-blue-500/20 text-blue-400 flex items-center justify-center"><Mic size={24} /></div>
                <div className="text-left">
                    <h3 className="font-bold text-white">麦克风权限</h3>
                    <p className="text-xs text-slate-400">用于实时对话分析</p>
                </div>
                <button onClick={onNext} className="ml-auto px-4 py-2 bg-blue-500 hover:bg-blue-400 text-white text-sm font-medium rounded-full transition-colors">允许</button>
            </div>
        </div>
        <div className="glass-card p-6 border border-white/10 bg-white/5 backdrop-blur-xl rounded-3xl">
             <div className="flex items-center gap-4 mb-4">
                <div className="w-12 h-12 rounded-full bg-purple-500/20 text-purple-400 flex items-center justify-center"><Bluetooth size={24} /></div>
                <div className="text-left">
                    <h3 className="font-bold text-white">蓝牙权限</h3>
                    <p className="text-xs text-slate-400">连接 SmartBadge</p>
                </div>
                <button onClick={onNext} className="ml-auto px-4 py-2 bg-purple-500 hover:bg-purple-400 text-white text-sm font-medium rounded-full transition-colors">允许</button>
            </div>
        </div>
    </motion.div>
);

// --- Step 3: Voice Handshake ---
export const HandshakeStep: React.FC<{ onNext: () => void }> = ({ onNext }) => {
    // Mock auto-advance after "listening"
    React.useEffect(() => {
        const t = setTimeout(onNext, 4000);
        return () => clearTimeout(t);
    }, [onNext]);

    return (
        <motion.div 
             initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
             className="text-center max-w-xs"
        >
            <h2 className="text-xl font-bold text-white mb-8">让我们先认识一下</h2>
            <p className="text-slate-400 mb-12">试着说：“你好，帮我搞定这个客户”</p>
            
            {/* Mock Waveform */}
            <div className="flex justify-center items-center gap-1 h-16 mb-12">
                {[...Array(12)].map((_, i) => (
                    <motion.div 
                        key={i}
                        className="w-1 bg-gradient-to-t from-blue-500 to-purple-500 rounded-full"
                        animate={{ height: [10, 40, 15, 50, 20] }}
                        transition={{ duration: 1.5, repeat: Infinity, delay: i * 0.1, ease: "easeInOut" }}
                    />
                ))}
            </div>
            
            <p className="text-xs text-slate-500 font-mono">正在聆听...</p>
        </motion.div>
    );
};
