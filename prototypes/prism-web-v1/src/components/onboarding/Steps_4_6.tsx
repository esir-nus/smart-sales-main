import React, { useEffect } from 'react';
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
                    className="absolute inset-0 rounded-full bg-prism-accent/20"
                    animate={{ scale: [1, 1.2, 1], opacity: [0.5, 0, 0.5] }}
                    transition={{ duration: 2, repeat: Infinity }}
                 />
                 <div className="w-full h-full bg-prism-surface rounded-2xl flex items-center justify-center border border-prism-border relative z-10">
                    <div className="w-8 h-8 rounded-full bg-black border-2 border-prism-secondary/50 animate-pulse" />
                 </div>
                 {/* Finger Press Animation */}
                 <motion.div 
                    className="absolute top-1/2 left-1/2 w-12 h-12 rounded-full bg-white/10 border border-white/30"
                    initial={{ x: -24, y: -24, scale: 1.5, opacity: 0 }}
                    animate={{ scale: [1.5, 1], opacity: [0, 1, 0] }}
                    transition={{ duration: 1.5, repeat: Infinity }}
                 />
            </div>
            <h2 className="text-xl font-bold text-prism-primary mb-4">启动您的 SmartBadge</h2>
            <p className="text-prism-secondary text-sm">长按中间按钮 3 秒，直到蓝灯闪烁</p>
            <button onClick={onNext} className="mt-8 text-prism-accent text-sm hover:text-prism-accent/80">灯已经在闪了</button>
        </motion.div>
    );
};

// --- Step 5: Scan ---
export const ScanStep: React.FC<{ onNext: () => void }> = ({ onNext }) => {
    useEffect(() => { setTimeout(onNext, 3000); }, [onNext]);
    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="text-center">
            <div className="relative w-48 h-48 mx-auto mb-8 flex items-center justify-center">
                <motion.div className="absolute inset-0 border border-prism-accent/30 rounded-full" animate={{ scale: [0.8, 1.5], opacity: [1, 0] }} transition={{ duration: 2, repeat: Infinity }} />
                <motion.div className="absolute inset-0 border border-prism-accent/30 rounded-full" animate={{ scale: [0.8, 1.5], opacity: [1, 0] }} transition={{ duration: 2, repeat: Infinity, delay: 0.5 }} />
                <div className="w-4 h-4 bg-prism-accent rounded-full shadow-lg shadow-prism-accent/50" />
            </div>
            <p className="text-prism-secondary">正在搜索设备...</p>
        </motion.div>
    );
};

// --- Step 6: Found (Manual Gate) ---
export const FoundStep: React.FC<{ onNext: () => void }> = ({ onNext }) => (
    <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="w-full max-w-xs">
        <h2 className="text-xl font-bold text-prism-primary mb-6 text-center">发现设备</h2>
        <div className="glass-card p-4 rounded-2xl bg-prism-surface/50 border border-prism-border">
            <div className="flex items-center gap-4">
                <div className="w-10 h-10 bg-prism-surface-muted rounded-full flex items-center justify-center text-prism-secondary">
                    <Smartphone size={20} />
                </div>
                <div>
                     <h3 className="font-bold text-prism-primary text-sm">SmartBadge (Frank's)</h3>
                     <p className="text-xs text-prism-knot">ID: FF:23:44:A1</p>
                </div>
                <div className="ml-auto text-xs text-prism-secondary">-42dBm</div>
            </div>
            <button 
                onClick={onNext}
                className="w-full mt-4 py-3 bg-prism-accent hover:bg-prism-accent/90 text-white rounded-xl font-medium transition-colors flex items-center justify-center gap-2"
            >
                连接
            </button>
        </div>
    </motion.div>
);
