import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { RefreshCw, Wifi, Type } from 'lucide-react';

// --- Step 7: FW Check ---
export const FwCheckStep: React.FC<{ onNext: () => void }> = ({ onNext }) => {
    const [progress, setProgress] = useState(0);
    useEffect(() => {
        const interval = setInterval(() => {
            setProgress(prev => {
                if (prev >= 100) { 
                    clearInterval(interval); 
                    return 100; 
                }
                return prev + 2;
            });
        }, 50);
        return () => clearInterval(interval);
    }, []); // Removed onNext dependency

    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="w-full max-w-xs text-center">
            <RefreshCw size={48} className={`text-blue-400 mx-auto mb-6 ${progress < 100 ? 'animate-spin' : ''}`} />
            
            {progress < 100 ? (
                <>
                    <h2 className="text-xl font-bold text-white mb-2">正在检查固件版本...</h2>
                    <p className="text-slate-400 text-sm mb-8">v1.0.2 -{'>'} v1.2.0 (必需)</p>
                </>
            ) : (
                <>
                    <h2 className="text-xl font-bold text-white mb-2">固件已更新</h2>
                    <p className="text-slate-400 text-sm mb-8">v1.2.0 就绪</p>
                </>
            )}
            
            <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden mb-2">
                <motion.div className="h-full bg-blue-500" style={{ width: `${progress}%` }} />
            </div>
            
            {progress < 100 ? (
                <p className="text-xs text-slate-500">{progress}% - 请勿关闭设备</p>
            ) : (
                <motion.button
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    onClick={onNext}
                    className="w-full mt-6 py-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-medium transition-colors"
                >
                    下一步
                </motion.button>
            )}
        </motion.div>
    );
};

// --- Step 8: WiFi Setup ---
export const WifiStep: React.FC<{ onNext: () => void }> = ({ onNext }) => (
    <motion.div initial={{ opacity: 0, x: 50 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -50 }} className="w-full max-w-xs text-center">
        <Wifi size={48} className="text-purple-400 mx-auto mb-6" />
        <h2 className="text-xl font-bold text-white mb-2">配置网络</h2>
        <p className="text-slate-400 text-sm mb-8">让徽章独立工作</p>
        
        <input type="text" placeholder="WiFi 名称 (SSID)" className="w-full bg-white/10 border border-white/10 rounded-xl p-4 text-white placeholder-slate-500 mb-3 outline-none focus:border-blue-500 transition-colors" />
        <input type="password" placeholder="密码" className="w-full bg-white/10 border border-white/10 rounded-xl p-4 text-white placeholder-slate-500 mb-6 outline-none focus:border-blue-500 transition-colors" />
        
        <button onClick={onNext} className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-medium mb-3">连接网络</button>
        <button onClick={onNext} className="text-slate-500 text-sm hover:text-white transition-colors">跳过</button>
    </motion.div>
);

// --- Step 9: Naming ---
export const NamingStep: React.FC<{ onNext: () => void }> = ({ onNext }) => (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="w-full max-w-xs text-center">
        <Type size={48} className="text-green-400 mx-auto mb-6" />
        <h2 className="text-xl font-bold text-white mb-8">给它起个名字</h2>
        
        <input type="text" defaultValue="Frank's Badge" className="w-full bg-white/10 border border-white/10 rounded-xl p-4 text-center text-xl font-bold text-white mb-8 outline-none focus:border-green-500 transition-colors" />
        
        <button onClick={onNext} className="w-full py-3 bg-green-600 hover:bg-green-500 text-white rounded-xl font-medium">确定</button>
    </motion.div>
);
