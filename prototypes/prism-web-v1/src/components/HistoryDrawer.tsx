// React unused
import { motion } from 'framer-motion';
import { Battery, Wifi, Pin, Settings, User, Plus, Calendar, CalendarDays, Archive } from 'lucide-react';

type Session = {
    id: string;
    title: string;
    summary: string;
    isPinned?: boolean;
};

// Mock Data
const SESSIONS: Record<string, Session[]> = {
    'pinned': [
        { id: '1', title: 'Q4 预算审查', summary: '关键数据修正', isPinned: true },
    ],
    'today': [
        { id: '2', title: '团队同步', summary: '本周重点回顾' },
    ],
    'recent': [
        { id: '3', title: '产品发布会', summary: '竞品价格分析' },
        { id: '4', title: '面试笔记', summary: '候选人A评估' },
    ],
    'archive': [
        { id: '5', title: 'A3 项目启动', summary: '资源分配确认' },
        { id: '6', title: '销售培训', summary: '话术演练复盘' },
    ]
};

interface HistoryDrawerProps {
    isOpen: boolean;
    onClose: () => void;
    onSelectSession: (id: string) => void;
    onSettingsClick?: () => void;
    onNewSession?: () => void;
}

export const HistoryDrawer = ({ isOpen, onClose, onSelectSession, onSettingsClick, onNewSession }: HistoryDrawerProps) => {
    return (
        <>
            {/* Scrim / Backdrop */}
            {isOpen && (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 0.5 }}
                    exit={{ opacity: 0 }}
                    onClick={onClose}
                    className="absolute inset-0 bg-black z-40"
                />
            )}

            {/* Drawer Content */}
            <motion.div
                initial={{ x: '-100%' }}
                animate={{ x: isOpen ? 0 : '-100%' }}
                transition={{ type: 'spring', damping: 25, stiffness: 200 }}
                className="absolute top-0 bottom-0 left-0 w-[85%] max-w-[320px] bg-prism-surface backdrop-blur-3xl z-50 shadow-2xl flex flex-col border-r border-prism-border"
            >
                {/* 1. Device Header */}
                <div className="p-6 border-b border-prism-border">
                    <div className="flex items-center justify-between mb-4">
                        {/* Device State */}
                        <div>
                            <div className="flex items-center gap-3 text-sm font-medium text-prism-secondary mb-1">
                                <span className="flex items-center gap-1.5 text-green-600">
                                    <Battery size={16} /> 85%
                                </span>
                                <span className="flex items-center gap-1.5">
                                    <Wifi size={16} /> SmartBadge
                                </span>
                            </div>
                            <div className="text-xs font-semibold text-prism-secondary uppercase tracking-wider opacity-70">
                                已连接 • 正常
                            </div>
                        </div>

                        {/* Plus Action */}
                        <button 
                            onClick={onNewSession}
                            className="p-2 -mr-2 text-prism-primary hover:bg-black/5 rounded-full transition-colors"
                        >
                            <Plus size={24} />
                        </button>
                    </div>
                </div>

                {/* 2. Session List (Scrollable) */}
                <div className="flex-1 overflow-y-auto py-4">
                    {/* Helper to render Session Groups */}
                    {(Object.entries(SESSIONS) as [string, Session[]][]).map(([key, group]) => (
                        <div key={key} className="mb-6">
                            <div className="px-6 mb-3 text-[10px] font-bold text-prism-secondary uppercase tracking-widest flex items-center gap-2 opacity-60">
                                {key === 'pinned' && <Pin size={12} />}
                                {key === 'today' && <Calendar size={12} />}
                                {key === 'recent' && <CalendarDays size={12} />}
                                {key === 'archive' && <Archive size={12} />}
                                
                                {key === 'pinned' ? '置顶' : 
                                 key === 'today' ? '今天' : 
                                 key === 'recent' ? '最近30天' : '2025-12'}
                            </div>
                            <div className="space-y-0.5">
                                {group.map(session => (
                                    <div 
                                        key={session.id}
                                        onClick={() => onSelectSession(session.id)}
                                        className="px-6 py-3 cursor-pointer group hover:bg-black/5 transition-colors"
                                    >
                                        <div className="flex items-baseline gap-2 mb-0.5">
                                            <span className="text-sm font-bold text-prism-primary">{session.title}</span>
                                            <span className="text-xs text-prism-secondary font-medium">_{session.summary}</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>

                {/* 3. User Footer */}
                <div className="p-4 border-t border-prism-border flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-black/5 flex items-center justify-center text-prism-secondary">
                        <User size={20} />
                    </div>
                    <div className="flex-1 min-w-0">
                        <div className="font-bold text-prism-primary truncate">Frank Chen</div>
                        <div className="flex items-center gap-2 mt-0.5">
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-black text-white font-bold tracking-wide">
                                PRO
                            </span>
                        </div>
                    </div>
                    <button 
                        onClick={onSettingsClick}
                        className="p-2 text-prism-secondary hover:text-prism-primary rounded-full hover:bg-black/5 transition-colors"
                    >
                        <Settings size={20} />
                    </button>
                </div>
            </motion.div>
        </>
    );
};
