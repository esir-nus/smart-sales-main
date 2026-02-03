/**
 * HistoryCardLayout.tsx - 历史会话 (Variant 3: Structured Cards)
 * 
 * Design Concept: "Structured Intelligence"
 * - Information grouped into distinct glass cards.
 * - High legibility, professional data density.
 * - Strong separation of concerns.
 */

import { Battery, Wifi, Pin, Settings, User, Calendar, CalendarDays, Archive, ChevronRight } from 'lucide-react';

type Session = {
    id: string;
    title: string;
    summary: string;
};

// Static Mock Data - Functional Spec Compliant
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

const GROUP_CONFIG: Record<string, { icon: React.ReactNode; label: string; color: string }> = {
    'pinned': { icon: <Pin size={14} />, label: '置顶会话', color: 'text-amber-500' },
    'today': { icon: <Calendar size={14} />, label: '今天', color: 'text-blue-500' },
    'recent': { icon: <CalendarDays size={14} />, label: '最近30天', color: 'text-indigo-500' },
    'archive': { icon: <Archive size={14} />, label: '2025-12', color: 'text-gray-500' },
};

export const HistoryCardLayout = () => {
    return (
        <div className="absolute inset-0 z-[100] flex flex-col">
            {/* Background: Slightly Darker for Contrast with Cards */}
            <div className="absolute inset-0 bg-[#F5F5F7]/90 backdrop-blur-xl z-0" />
            
            <div className="relative z-10 flex flex-col h-full">

                {/* 1. Integrated Header (Minimal) */}
                <div className="px-5 pt-14 pb-4 bg-white/50 backdrop-blur-md border-b border-white/20">
                    <div className="flex items-center justify-between">
                        <div className="flex flex-col">
                            <span className="text-xs font-bold text-prism-secondary uppercase tracking-widest mb-1">设备状态</span>
                            <div className="flex items-center gap-2">
                                <span className="flex items-center gap-1.5 text-sm font-semibold text-prism-primary">
                                    <Wifi size={16} /> SmartBadge
                                </span>
                                <span className="w-1 h-1 rounded-full bg-prism-border" />
                                <span className="flex items-center gap-1 text-sm font-medium text-green-600">
                                    <Battery size={14} className="fill-current" /> 85%
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 2. Card List */}
                <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar">
                    {(Object.entries(SESSIONS) as [string, Session[]][]).map(([key, group]) => (
                        <div 
                            key={key} 
                            className="bg-white/60 backdrop-blur-sm rounded-2xl border border-white/40 shadow-sm overflow-hidden"
                        >
                            {/* Card Header */}
                            <div className="px-4 py-3 border-b border-black/5 flex items-center gap-2 bg-white/20">
                                <div className={`${GROUP_CONFIG[key].color}`}>
                                    {GROUP_CONFIG[key].icon}
                                </div>
                                <span className="text-xs font-bold text-prism-primary uppercase tracking-wide">
                                    {GROUP_CONFIG[key].label}
                                </span>
                            </div>

                            {/* Card Items */}
                            <div className="divide-y divide-black/5">
                                {group.map(session => (
                                    <div 
                                        key={session.id}
                                        className="px-4 py-3 hover:bg-black/5 transition-colors cursor-pointer group flex items-center justify-between"
                                    >
                                        <div className="flex items-center gap-3">
                                            <div className="w-1 h-8 rounded-full bg-gradient-to-b from-prism-primary/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
                                            <div className="flex flex-col">
                                                <span className="text-sm font-semibold text-prism-primary leading-tight">
                                                    {session.title}
                                                </span>
                                                <span className="text-[11px] font-medium text-prism-secondary mt-0.5">
                                                    {session.summary}
                                                </span>
                                            </div>
                                        </div>
                                        <ChevronRight size={14} className="text-prism-border group-hover:text-prism-secondary transition-colors" />
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                    
                    <div className="h-20" /> {/* Bottom spacer */}
                </div>

                {/* 3. Compact Footer */}
                <div className="p-3 bg-white/80 backdrop-blur-md border-t border-white/20">
                    <div className="flex items-center gap-3 p-2 rounded-xl hover:bg-black/5 transition-colors cursor-pointer">
                        <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-500">
                            <User size={16} />
                        </div>
                        <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                                <span className="text-sm font-bold text-prism-primary">Frank Chen</span>
                                <span className="px-1.5 py-0.5 rounded-md bg-black text-white text-[9px] font-bold tracking-wider">PRO</span>
                            </div>
                        </div>
                        <Settings size={18} className="text-prism-secondary" />
                    </div>
                </div>

            </div>
        </div>
    );
};
