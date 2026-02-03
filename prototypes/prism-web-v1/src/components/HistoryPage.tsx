/**
 * HistoryPage.tsx
 * 
 * Standalone static design page for History Drawer.
 * 100% static - no state, no interactions beyond visual.
 */

import { Battery, Wifi, Pin, Settings, User, Plus, Calendar, CalendarDays, Archive } from 'lucide-react';

type Session = {
    id: string;
    title: string;
    summary: string;
};

// Static Mock Data
const SESSIONS: Record<string, Session[]> = {
    'pinned': [
        { id: '1', title: '张总', summary: 'Q4预算审查' },
    ],
    'today': [
        { id: '2', title: '王经理', summary: 'A3项目跟进' },
    ],
    'recent': [
        { id: '3', title: '李财务', summary: '采购谈判中' },
        { id: '4', title: '陈主任', summary: '竞品价格分析' },
    ],
    'archive': [
        { id: '5', title: '赵总', summary: '合作意向确认' },
        { id: '6', title: '孙经理', summary: '产品演示汇报' },
    ]
};

const GROUP_CONFIG: Record<string, { icon: React.ReactNode; label: string }> = {
    'pinned': { icon: <Pin size={12} />, label: '置顶' },
    'today': { icon: <Calendar size={12} />, label: '今天' },
    'recent': { icon: <CalendarDays size={12} />, label: '最近30天' },
    'archive': { icon: <Archive size={12} />, label: '2025-12' },
};

export const HistoryPage = () => {
    return (
        <div className="w-full h-full bg-prism-surface flex flex-col">
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
                    <button className="p-2 -mr-2 text-prism-primary hover:bg-black/5 rounded-full transition-colors">
                        <Plus size={24} />
                    </button>
                </div>
            </div>

            {/* 2. Session List */}
            <div className="flex-1 overflow-y-auto py-4">
                {(Object.entries(SESSIONS) as [string, Session[]][]).map(([key, group]) => (
                    <div key={key} className="mb-6">
                        <div className="px-6 mb-3 text-[10px] font-bold text-prism-secondary uppercase tracking-widest flex items-center gap-2 opacity-60">
                            {GROUP_CONFIG[key]?.icon}
                            {GROUP_CONFIG[key]?.label}
                        </div>
                        <div className="space-y-0.5">
                            {group.map(session => (
                                <div 
                                    key={session.id}
                                    className="px-6 py-3 cursor-pointer group hover:bg-black/5 transition-colors"
                                >
                                    <div className="flex items-baseline gap-1">
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
                <button className="p-2 text-prism-secondary hover:text-prism-primary rounded-full hover:bg-black/5 transition-colors">
                    <Settings size={20} />
                </button>
            </div>
        </div>
    );
};
