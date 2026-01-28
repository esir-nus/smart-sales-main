import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
    X, ChevronRight, Moon, 
    Smartphone, Bell, HardDrive, Shield, 
    HelpCircle, MessageSquare, Info, LogOut 
} from 'lucide-react';

interface UserCenterProps {
    isOpen: boolean;
    onClose: () => void;
}

export const UserCenter: React.FC<UserCenterProps> = ({ isOpen, onClose }) => {
    return (
        <AnimatePresence>
            {isOpen && (
                <div className="absolute inset-0 z-50 flex justify-end">
                    {/* Backdrop */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="absolute inset-0 bg-black/20 backdrop-blur-sm"
                    />

                    {/* Drawer Content */}
                    <motion.div
                        initial={{ x: '100%' }}
                        animate={{ x: 0 }}
                        exit={{ x: '100%' }}
                        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
                        className="relative w-full h-full bg-white flex flex-col shadow-2xl overflow-hidden"
                    >
                        {/* Header */}
                        <div className="relative h-48 bg-gradient-to-br from-blue-600 to-purple-600 p-6 text-white overflow-hidden shrink-0">
                            {/* Aurora Decorative Blobs */}
                            <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
                            <div className="absolute bottom-0 left-0 w-48 h-48 bg-purple-500/30 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2" />

                            <div className="relative z-10 flex flex-col h-full">
                                <button 
                                    onClick={onClose}
                                    className="self-end p-2 bg-white/20 hover:bg-white/30 rounded-full transition-colors mb-auto backdrop-blur-md"
                                >
                                    <X size={20} className="text-white" />
                                </button>
                                
                                <div className="flex items-center gap-4 mt-4">
                                    <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur-md border-2 border-white/30 flex items-center justify-center text-2xl font-bold shadow-lg">
                                        FC
                                    </div>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <h2 className="text-xl font-bold">Frank Chen</h2>
                                            <span className="px-2 py-0.5 rounded-full bg-white/20 backdrop-blur-md text-[10px] font-bold border border-white/30 tracking-wide shadow-sm">
                                                PRO 专业版
                                            </span>
                                        </div>
                                        <p className="text-blue-100 text-sm">销售总监 • 华东大区</p>
                                    </div>
                                    <button className="text-sm font-medium bg-white/20 hover:bg-white/30 px-3 py-1.5 rounded-full backdrop-blur-md transition-colors">
                                        编辑资料
                                    </button>
                                </div>
                            </div>
                        </div>

                        {/* Scrollable Content */}
                        <div className="flex-1 overflow-y-auto bg-slate-50">
                            <div className="p-4 space-y-6">
                                
                                {/* Section: Preferences */}
                                <Section title="偏好设置">
                                    <SettingItem 
                                        icon={<Moon size={18} />} 
                                        label="主题模式" 
                                        value="跟随系统"
                                        color="bg-purple-100 text-purple-600"
                                    />
                                    <SettingItem 
                                        icon={<Smartphone size={18} />} 
                                        label="AI 实验室" 
                                        value="已开启记忆增强"
                                        color="bg-blue-100 text-blue-600"
                                    />
                                    <SettingItem 
                                        icon={<Bell size={18} />} 
                                        label="通知管理" 
                                        color="bg-orange-100 text-orange-600"
                                    />
                                </Section>

                                {/* Section: Storage */}
                                <Section title="存储空间">
                                    <SettingItem 
                                        icon={<HardDrive size={18} />} 
                                        label="本地缓存" 
                                        value="已用 128MB"
                                        color="bg-cyan-100 text-cyan-600"
                                        action={<button className="text-xs text-blue-600 font-medium">清理</button>}
                                    />
                                </Section>

                                {/* Section: Security */}
                                <Section title="账号安全">
                                    <SettingItem 
                                        icon={<Shield size={18} />} 
                                        label="修改密码" 
                                        color="bg-green-100 text-green-600"
                                    />
                                    <SettingItem 
                                        icon={<Smartphone size={18} />} 
                                        label="生物识别" 
                                        value="面容 ID"
                                        color="bg-slate-200 text-slate-700"
                                    />
                                </Section>

                                {/* Section: Support */}
                                <Section title="帮助与支持">
                                    <SettingItem 
                                        icon={<HelpCircle size={18} />} 
                                        label="帮助中心" 
                                        color="bg-indigo-100 text-indigo-600"
                                    />
                                    <SettingItem 
                                        icon={<MessageSquare size={18} />} 
                                        label="问题反馈" 
                                        color="bg-pink-100 text-pink-600"
                                    />
                                </Section>

                                {/* Section: About */}
                                <Section title="关于">
                                    <SettingItem 
                                        icon={<Info size={18} />} 
                                        label="版本信息" 
                                        value="v1.0.0 (Beta)"
                                        color="bg-gray-200 text-gray-600"
                                    />
                                </Section>

                                {/* Logout Button */}
                                <button className="w-full py-4 bg-white text-red-600 font-medium rounded-2xl shadow-sm border border-red-50 hover:bg-red-50 transition-colors flex items-center justify-center gap-2 mt-4">
                                    <LogOut size={18} />
                                    退出登录
                                </button>

                                <div className="text-center text-xs text-gray-400 py-4 pb-8">
                                    SmartSales Inc. © 2026
                                </div>
                            </div>
                        </div>
                    </motion.div>
                </div>
            )}
        </AnimatePresence>
    );
};

// Sub-components for cleanliness
const Section: React.FC<{ title: string; children: React.ReactNode }> = ({ title, children }) => (
    <div className="space-y-2">
        <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider px-2">{title}</h3>
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden divide-y divide-gray-50">
            {children}
        </div>
    </div>
);

interface SettingItemProps {
    icon: React.ReactNode;
    label: string;
    value?: string;
    action?: React.ReactNode;
    color: string;
    onClick?: () => void;
}

const SettingItem: React.FC<SettingItemProps> = ({ icon, label, value, action, color, onClick }) => (
    <div 
        onClick={onClick}
        className="flex items-center justify-between p-4 active:bg-gray-50 transition-colors cursor-pointer"
    >
        <div className="flex items-center gap-3">
            <div className={`w-8 h-8 rounded-full flex items-center justify-center ${color}`}>
                {icon}
            </div>
            <span className="text-sm font-medium text-gray-900">{label}</span>
        </div>
        
        <div className="flex items-center gap-2">
            {value && <span className="text-sm text-gray-400">{value}</span>}
            {action ? action : <ChevronRight size={16} className="text-gray-300" />}
        </div>
    </div>
);
