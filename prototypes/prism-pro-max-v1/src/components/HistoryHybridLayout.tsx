/**
 * HistoryHybridLayout.tsx - 历史会话 (Variant 4: Hybrid)
 * 
 * Design Concept: "Hybrid Intelligence"
 * - HEADER: Floating Capsule (from Variant 2)
 * - CONTENT: Structured Glass Cards (from Variant 3)
 * - FOOTER: Floating Glass Dock (from Variant 2)
 */

import { useState } from 'react';
import { Battery, Wifi, Pin, Settings, User, Calendar, CalendarDays, Archive, ChevronRight, ChevronDown } from 'lucide-react';

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

const GROUP_CONFIG: Record<string, { icon: React.ReactNode; label: string; color: string }> = {
    'pinned': { icon: <Pin size={14} />, label: '置顶会话', color: 'text-amber-500' },
    'today': { icon: <Calendar size={14} />, label: '今天', color: 'text-blue-500' },
    'recent': { icon: <CalendarDays size={14} />, label: '最近30天', color: 'text-indigo-500' },
    'archive': { icon: <Archive size={14} />, label: '2025-12', color: 'text-gray-500' },
};

export const HistoryHybridLayout = () => {
    // Collapse State (Default: All Expanded)
    const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});

    const toggle = (key: string) => {
        setCollapsed(prev => ({ ...prev, [key]: !prev[key] }));
    };

    return (
        <div className="absolute inset-0 z-[100] flex flex-col">
            {/* Background: Variant 3's Contrast Background for Cards */}
            <div className="absolute inset-0 bg-[#F5F5F7]/95 backdrop-blur-xl z-0" />
            
            <div className="relative z-10 flex flex-col h-full">

                {/* 1. HEADER: Floating Status Capsule (from Variant 2) */}
                <div className="px-6 pt-16 pb-2">
                    <div className="inline-flex items-center gap-4 px-4 py-2 bg-white/80 border border-white/40 rounded-full shadow-sm backdrop-blur-md">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-green-600">
                            <Battery size={14} className="fill-current" /> 85%
                        </div>
                        <div className="w-px h-3 bg-black/10" />
                        <div className="flex items-center gap-1.5 text-xs font-medium text-prism-primary">
                            <Wifi size={14} /> SmartBadge
                        </div>
                        <span className="text-[10px] text-prism-secondary font-medium tracking-wide">
                           • 正常
                        </span>
                    </div>
                </div>

                {/* 2. CONTENT: Structured Cards (from Variant 3) */}
                <div className="flex-1 overflow-y-auto p-4 space-y-4 no-scrollbar">
                    {(Object.entries(SESSIONS) as [string, Session[]][]).map(([key, group]) => (
                        <div 
                            key={key} 
                            className="bg-white rounded-2xl border border-white/40 shadow-sm overflow-hidden transition-all duration-300"
                        >
                            {/* Card Header (Clickable for Collapse) */}
                            <div 
                                onClick={() => toggle(key)}
                                className="px-4 py-3 border-b border-black/5 flex items-center justify-between bg-gray-50/50 cursor-pointer hover:bg-black/5 transition-colors select-none"
                            >
                                <div className="flex items-center gap-2">
                                    <div className={`${GROUP_CONFIG[key].color}`}>
                                        {GROUP_CONFIG[key].icon}
                                    </div>
                                    <span className="text-xs font-bold text-prism-primary uppercase tracking-wide">
                                        {GROUP_CONFIG[key].label}
                                    </span>
                                </div>
                                <ChevronDown 
                                    size={14} 
                                    className={`text-prism-secondary/50 transition-transform duration-300 ${collapsed[key] ? '-rotate-90' : ''}`} 
                                />
                            </div>

                            {/* Card Items (Collapsible with Grid Trick) */}
                            <div className={`grid transition-[grid-template-rows] duration-300 ease-out ${collapsed[key] ? 'grid-rows-[0fr]' : 'grid-rows-[1fr]'}`}>
                                <div className="overflow-hidden">
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
                            </div>
                        </div>
                    ))}
                    
                    {/* Bottom Spacer for Floating Dock */}
                    <div className="h-24" />
                </div>

                {/* 3. FOOTER: Floating Glass Dock (from Variant 2) */}
                <div className="absolute bottom-6 left-4 right-4">
                    <div className="p-1 pr-2 bg-white/80 backdrop-blur-xl border border-white/40 shadow-glass rounded-[20px] flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-gray-200 to-white flex items-center justify-center shadow-inner">
                                <User size={20} className="text-prism-primary/80" />
                            </div>
                            <div className="flex flex-col justify-center">
                                <div className="text-sm font-bold text-prism-primary leading-tight">Frank Chen</div>
                                <div className="text-[10px] font-medium text-prism-secondary/80">Premium Plan</div>
                            </div>
                        </div>
                        
                        <button className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-black/5 transition-colors text-prism-secondary">
                            <Settings size={18} />
                        </button>
                    </div>
                </div>

            </div>
        </div>
    );
};
