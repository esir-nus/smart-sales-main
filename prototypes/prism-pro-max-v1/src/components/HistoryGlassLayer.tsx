/**
 * HistoryPage.tsx - 历史会话页面
 * 
 * Standalone static design for History Drawer content.
 * 100% static - visual display only.
 */

import { Battery, Wifi, Pin, Settings, User, Calendar, CalendarDays, Archive } from 'lucide-react';

type Session = {
    id: string;
    title: string;
    summary: string;
};

// Static Mock Data - Spec Format: [Client Name]_[Summary]
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

export const HistoryGlassLayer = () => {
    return (
        <div className="absolute inset-0 z-[100] flex flex-col">
            {/* Background Blur Layer */}
            <div className="absolute inset-0 bg-white/40 backdrop-blur-2xl z-0" />
            
            {/* Content Layer */}
            <div className="relative z-10 flex flex-col h-full">

                {/* 1. Floating Status Capsule */}
                <div className="px-6 pt-16 pb-2">
                    <div className="inline-flex items-center gap-4 px-4 py-2 bg-white/50 border border-white/40 rounded-full shadow-sm backdrop-blur-md">
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

                {/* 2. Ambient Session List */}
                <div className="flex-1 overflow-y-auto px-6 py-4 space-y-8 no-scrollbar">
                    {(Object.entries(SESSIONS) as [string, Session[]][]).map(([key, group]) => (
                        <div key={key} className="space-y-3">
                            {/* Ambient Group Header */}
                            <div className="flex items-center gap-2.5 opacity-60">
                                <div className={`w-6 h-6 rounded-full flex items-center justify-center 
                                    ${key === 'pinned' ? 'bg-amber-100/50 text-amber-600' : 
                                      key === 'today' ? 'bg-blue-100/50 text-blue-600' :
                                      'bg-gray-200/50 text-gray-500'}`}>
                                    {GROUP_CONFIG[key]?.icon}
                                </div>
                                <span className="text-[10px] font-bold text-prism-secondary uppercase tracking-[0.15em]">
                                    {GROUP_CONFIG[key]?.label}
                                </span>
                            </div>

                            {/* Session Items */}
                            <div className="pl-9 space-y-3">
                                {group.map(session => (
                                    <div 
                                        key={session.id}
                                        className="py-1 group cursor-pointer"
                                    >
                                        <div className="flex items-baseline gap-2 transition-transform duration-300 group-hover:translate-x-1">
                                            <span className="text-[15px] font-semibold text-prism-primary/90 tracking-tight">
                                                {session.title}
                                            </span>
                                            <span className="text-sm text-prism-secondary/70 font-normal tracking-wide">
                                                _{session.summary}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                    
                    {/* Bottom Spacer for Dock */}
                    <div className="h-24" />
                </div>

                {/* 3. Floating Glass Dock (User) */}
                <div className="absolute bottom-6 left-4 right-4">
                    <div className="p-1 pr-2 bg-white/60 backdrop-blur-xl border border-white/40 shadow-glass rounded-[20px] flex items-center justify-between">
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
