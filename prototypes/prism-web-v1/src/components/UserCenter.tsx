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
                        className="relative w-full h-full bg-prism-bg flex flex-col shadow-2xl overflow-hidden"
                    >
                        {/* Header */}
                        <div className="relative h-48 bg-prism-surface/80 backdrop-blur-3xl p-6 text-prism-primary overflow-hidden shrink-0 border-b border-prism-border">
                            {/* Aurora Decorative Blobs (Subtle) */}
                            <div className="absolute top-0 right-0 w-64 h-64 bg-prism-accent/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
                            <div className="absolute bottom-0 left-0 w-48 h-48 bg-prism-accent/5 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2" />

                            <div className="relative z-10 flex flex-col h-full">
                                <button 
                                    onClick={onClose}
                                    className="self-end p-2 bg-prism-surface-muted hover:bg-prism-border rounded-full transition-colors mb-auto"
                                >
                                    <X size={20} className="text-prism-primary" />
                                </button>
                                
                                <div className="flex items-center gap-4 mt-4">
                                    <div className="w-16 h-16 rounded-full bg-black/5 backdrop-blur-md border border-prism-border flex items-center justify-center text-2xl font-bold shadow-sm text-prism-primary">
                                        FC
                                    </div>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <h2 className="text-xl font-bold text-prism-primary">Frank Chen</h2>
                                            <span className="px-2 py-0.5 rounded-full bg-prism-surface-muted text-[10px] font-bold border border-prism-border tracking-wide text-prism-accent">
                                                PRO 专业版
                                            </span>
                                        </div>
                                        <p className="text-prism-secondary text-sm">销售总监 • 华东大区</p>
                                    </div>
                                    <button className="text-sm font-medium bg-prism-surface-muted hover:bg-prism-border px-3 py-1.5 rounded-full transition-colors text-prism-primary">
                                        编辑资料
                                    </button>
                                </div>
                            </div>
                        </div>

                        {/* Scrollable Content */}
                        <div className="flex-1 overflow-y-auto bg-prism-bg">
                            <div className="p-4 space-y-6">
                                
                                {/* Section: Preferences */}
                                <Section title="偏好设置">
                                    <SettingItem 
                                        icon={<Moon size={18} />} 
                                        label="主题模式" 
                                        value="跟随系统"
                                        color="bg-prism-surface-muted text-prism-primary"
                                    />
                                    <SettingItem 
                                        icon={<Smartphone size={18} />} 
                                        label="AI 实验室" 
                                        value="已开启记忆增强"
                                        color="bg-prism-surface-muted text-prism-primary"
                                    />
                                    <SettingItem 
                                        icon={<Bell size={18} />} 
                                        label="通知管理" 
                                        color="bg-prism-surface-muted text-prism-primary"
                                    />
                                </Section>

                                {/* Section: Storage */}
                                <Section title="存储空间">
                                    <SettingItem 
                                        icon={<HardDrive size={18} />} 
                                        label="本地缓存" 
                                        value="已用 128MB"
                                        color="bg-prism-surface-muted text-prism-primary"
                                        action={<button className="text-xs text-prism-accent font-medium">清理</button>}
                                    />
                                </Section>

                                {/* Section: Security */}
                                <Section title="账号安全">
                                    <SettingItem 
                                        icon={<Shield size={18} />} 
                                        label="修改密码" 
                                        color="bg-prism-surface-muted text-prism-primary"
                                    />
                                    <SettingItem 
                                        icon={<Smartphone size={18} />} 
                                        label="生物识别" 
                                        value="面容 ID"
                                        color="bg-prism-surface-muted text-prism-primary"
                                    />
                                </Section>

                                {/* Section: Support */}
                                <Section title="帮助与支持">
                                    <SettingItem 
                                        icon={<HelpCircle size={18} />} 
                                        label="帮助中心" 
                                        color="bg-prism-surface-muted text-prism-primary"
                                    />
                                    <SettingItem 
                                        icon={<MessageSquare size={18} />} 
                                        label="问题反馈" 
                                        color="bg-prism-surface-muted text-prism-primary"
                                    />
                                </Section>

                                {/* Section: About */}
                                <Section title="关于">
                                    <SettingItem 
                                        icon={<Info size={18} />} 
                                        label="版本信息" 
                                        value="v1.0.0 (Beta)"
                                        color="bg-prism-surface-muted text-prism-primary"
                                    />
                                </Section>

                                {/* Logout Button */}
                                <button className="w-full py-4 bg-prism-surface text-prism-danger font-medium rounded-2xl shadow-sm border border-prism-danger/20 hover:bg-prism-danger/5 transition-colors flex items-center justify-center gap-2 mt-4">
                                    <LogOut size={18} />
                                    退出登录
                                </button>

                                <div className="text-center text-xs text-prism-secondary/50 py-4 pb-8">
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
        <h3 className="text-xs font-bold text-prism-secondary uppercase tracking-wider px-2 opacity-70">{title}</h3>
        <div className="bg-prism-surface rounded-2xl border border-prism-border shadow-sm overflow-hidden divide-y divide-prism-border/50">
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
        className="flex items-center justify-between p-4 active:bg-prism-surface-muted transition-colors cursor-pointer"
    >
        <div className="flex items-center gap-3">
            <div className={`w-8 h-8 rounded-full flex items-center justify-center ${color}`}>
                {icon}
            </div>
            <span className="text-sm font-medium text-prism-primary">{label}</span>
        </div>
        
        <div className="flex items-center gap-2">
            {value && <span className="text-sm text-prism-secondary">{value}</span>}
            {action ? action : <ChevronRight size={16} className="text-prism-secondary/50" />}
        </div>
    </div>
);
