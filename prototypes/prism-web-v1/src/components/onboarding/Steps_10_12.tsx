import React from 'react';
import { motion } from 'framer-motion';
import { CheckCircle2, Mic } from 'lucide-react';

// --- Step 10: Account Gate ---
export const AccountStep: React.FC<{ onNext: () => void }> = ({ onNext }) => (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-xs">
        <div className="glass-card p-6 bg-slate-900/80 border border-white/10 rounded-3xl backdrop-blur-xl">
            <h2 className="text-xl font-bold text-white mb-2">保存您的设置</h2>
            <p className="text-xs text-slate-400 mb-6">登录以绑定 "Frank's Badge"</p>
            
            <input type="text" placeholder="邮箱/手机号" className="w-full bg-slate-800 border border-slate-700 rounded-xl p-3 text-white mb-3 text-sm" />
            <input type="password" placeholder="密码" className="w-full bg-slate-800 border border-slate-700 rounded-xl p-3 text-white mb-6 text-sm" />
            
            <button onClick={onNext} className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-medium text-sm mb-4">登录并绑定</button>
            <div className="text-center text-xs text-slate-500">没有账号？ <span className="text-blue-400">立即注册</span></div>
        </div>
    </motion.div>
);

// --- Step 11: Profile ---
export const ProfileStep: React.FC<{ onNext: () => void }> = ({ onNext }) => (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="w-full max-w-xs text-center">
        <h2 className="text-xl font-bold text-white mb-2">让我更好地帮助你</h2>
        <p className="text-xs text-slate-400 mb-8">(可跳过，后续可完善)</p>
        
        <button className="w-full py-6 border-2 border-dashed border-white/20 rounded-2xl flex flex-col items-center justify-center text-slate-400 hover:border-blue-500 hover:text-blue-400 transition-colors mb-6 group">
            <Mic size={24} className="mb-2 group-hover:scale-110 transition-transform" />
            <span className="text-sm">按住说话介绍自己</span>
        </button>
        
        <div className="relative mb-8">
            <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-white/10"></div></div>
            <div className="relative flex justify-center text-xs bg-transparent"><span className="px-2 bg-slate-900 text-slate-500">或者</span></div>
        </div>
        
        <div className="bg-white/5 rounded-xl p-4 mb-8 text-left">
             <div className="text-xs text-slate-500 mb-1">我是 [李明]</div>
             <div className="text-sm text-white">负责政企大客户销售...</div>
        </div>
        
        <button onClick={onNext} className="w-full py-3 bg-white text-slate-900 rounded-xl font-bold mb-3">完成</button>
        <button onClick={onNext} className="text-slate-500 text-sm hover:text-white">稍后完善</button>
    </motion.div>
);

// --- Step 12: Complete ---
export const CompleteStep: React.FC<{ onComplete: () => void }> = ({ onComplete }) => {
    // Auto finish
    React.useEffect(() => { setTimeout(onComplete, 2500); }, [onComplete]);
    
    return (
        <motion.div initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="text-center">
             <motion.div 
                initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: "spring", stiffness: 200, damping: 10 }}
                className="w-24 h-24 bg-green-500 rounded-full flex items-center justify-center mx-auto mb-8 shadow-2xl shadow-green-500/50"
            >
                <CheckCircle2 size={48} className="text-white" />
            </motion.div>
            <h2 className="text-2xl font-bold text-white mb-2">一切就绪！</h2>
            <p className="text-slate-400">正在进入工作台...</p>
        </motion.div>
    );
};
