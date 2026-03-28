import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
    X, ChevronRight, LogOut
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
                        <div className="relative min-h-[228px] bg-prism-surface/80 backdrop-blur-3xl p-6 text-prism-primary overflow-hidden shrink-0 border-b border-prism-border">
                            {/* Aurora Decorative Blobs (Subtle) */}
                            <div className="absolute top-0 right-0 w-64 h-64 bg-prism-accent/8 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
                            <div className="absolute bottom-0 left-0 w-48 h-48 bg-prism-knot/8 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2" />

                            <div className="relative z-10 flex flex-col h-full gap-6">
                                <div className="flex items-center justify-between">
                                    <div className="text-xs font-semibold tracking-[0.24em] text-prism-secondary uppercase">
                                        个人中心
                                    </div>
                                    <button 
                                        onClick={onClose}
                                        className="p-2 bg-prism-surface-muted hover:bg-prism-border rounded-full transition-colors border border-prism-border/70"
                                    >
                                        <X size={18} className="text-prism-primary" />
                                    </button>
                                </div>
                                
                                <div className="flex items-center gap-4">
                                    <div className="w-16 h-16 rounded-full bg-prism-surface-muted backdrop-blur-md border border-prism-border flex items-center justify-center text-2xl font-bold shadow-sm text-prism-primary">
                                        FC
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <h2 className="text-xl font-bold text-prism-primary">Frank Chen</h2>
                                            <span className="px-2 py-0.5 rounded-full bg-prism-accent/10 text-[10px] font-bold border border-prism-accent/20 tracking-wide text-prism-accent">
                                                PRO
                                            </span>
                                        </div>
                                        <p className="text-prism-secondary text-sm">销售总监</p>
                                        <p className="text-prism-secondary/80 text-xs mt-1">科技 · 10 years · WeChat</p>
                                    </div>
                                    <button className="shrink-0 text-sm font-medium bg-prism-surface-muted hover:bg-prism-border px-3 py-1.5 rounded-full transition-colors text-prism-primary border border-prism-border/70">
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
                                        label="主题外观" 
                                        value="跟随系统"
                                    />
                                    <SettingItem 
                                        label="AI 实验室" 
                                        action={<TogglePill checked={true} />}
                                    />
                                    <SettingItem 
                                        label="通知" 
                                        action={<TogglePill checked={true} />}
                                    />
                                </Section>

                                {/* Section: Storage */}
                                <Section title="存储空间">
                                    <SettingItem 
                                        label="本地缓存" 
                                        value="已用 128MB"
                                        action={<button className="text-xs text-prism-accent font-medium">清理</button>}
                                    />
                                </Section>

                                {/* Section: Security */}
                                <Section title="账号安全">
                                    <SettingItem 
                                        label="修改密码" 
                                    />
                                    <SettingItem 
                                        label="生物识别" 
                                        value="面容 ID"
                                    />
                                </Section>

                                {/* Section: About */}
                                <Section title="关于">
                                    <SettingItem 
                                        label="帮助中心" 
                                    />
                                    <SettingItem 
                                        label="版本信息" 
                                        value="Prism v1.2 (Pro Max)"
                                        showChevron={false}
                                    />
                                </Section>

                                {/* Logout Button */}
                                <button className="w-full py-4 bg-prism-danger/5 text-prism-danger font-medium rounded-2xl shadow-sm border border-prism-danger/20 hover:bg-prism-danger/10 transition-colors flex items-center justify-center gap-2 mt-4">
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
        <div className="bg-prism-surface/90 backdrop-blur-xl rounded-2xl border border-prism-border shadow-sm overflow-hidden divide-y divide-prism-border/50">
            {children}
        </div>
    </div>
);

interface SettingItemProps {
    label: string;
    value?: string;
    action?: React.ReactNode;
    onClick?: () => void;
    showChevron?: boolean;
}

const SettingItem: React.FC<SettingItemProps> = ({
    label,
    value,
    action,
    onClick,
    showChevron = true,
}) => (
    <div 
        onClick={onClick}
        className="flex items-center justify-between p-4 active:bg-prism-surface-muted transition-colors cursor-pointer"
    >
        <span className="text-sm font-medium text-prism-primary">{label}</span>
        
        <div className="flex items-center gap-2">
            {value && <span className="text-sm text-prism-secondary">{value}</span>}
            {action ? action : showChevron ? <ChevronRight size={16} className="text-prism-secondary/50" /> : null}
        </div>
    </div>
);

const TogglePill: React.FC<{ checked: boolean }> = ({ checked }) => (
    <div
        className={`relative h-7 w-12 rounded-full transition-colors ${
            checked ? 'bg-prism-accent/90' : 'bg-prism-surface-muted'
        }`}
    >
        <div
            className={`absolute top-1 h-5 w-5 rounded-full bg-white shadow-sm transition-all ${
                checked ? 'left-6' : 'left-1'
            }`}
        />
    </div>
);
