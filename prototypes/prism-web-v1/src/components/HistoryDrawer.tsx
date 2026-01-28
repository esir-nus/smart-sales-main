import React from 'react';
import { motion } from 'framer-motion';
import { Battery, Wifi, Pin, Trash2, Edit2, Settings, User } from 'lucide-react';
import { clsx } from 'clsx';

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
}

export const HistoryDrawer = ({ isOpen, onClose, onSelectSession, onSettingsClick }: HistoryDrawerProps) => {
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
                className="absolute top-0 bottom-0 left-0 w-[85%] max-w-[320px] bg-white z-50 shadow-2xl flex flex-col"
            >
                {/* 1. Device Header */}
                <div className="p-6 bg-slate-50 border-b border-slate-100 dark:bg-slate-900/50">
                    <div className="flex items-center justify-between text-xs font-mono text-slate-500 mb-2">
                        <span className="flex items-center gap-1 text-green-600">
                            <Battery size={14} /> 85%
                        </span>
                        <span className="flex items-center gap-1">
                            <Wifi size={14} /> SmartBadge
                        </span>
                    </div>
                    <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                        已连接 • 正常
                    </div>
                </div>

                {/* 2. Session List (Scrollable) */}
                <div className="flex-1 overflow-y-auto py-4">
                    {/* Helper to render Session Groups */}
                    {(Object.entries(SESSIONS) as [string, Session[]][]).map(([key, group]) => (
                        <div key={key} className="mb-6">
                            <div className="px-6 mb-3 text-xs font-bold text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                {key === 'pinned' && <Pin size={12} />}
                                {key === 'pinned' ? '置顶' : 
                                 key === 'today' ? '今天' : 
                                 key === 'recent' ? '最近30天' : '2025-12'}
                            </div>
                            <div className="space-y-1">
                                {group.map(session => (
                                    <div 
                                        key={session.id}
                                        onClick={() => onSelectSession(session.id)}
                                        className="px-6 py-3 hover:bg-slate-50 active:bg-slate-100 cursor-pointer group relative"
                                    >
                                        <div className="text-sm truncate pr-4 text-slate-800">
                                            <span className="font-medium">{session.title}</span>
                                            <span className="text-slate-400">_{session.summary}</span>
                                        </div>
                                        
                                        {/* Hover Actions (Desktop Prototype only) */}
                                        <div className="absolute right-4 top-1/2 -translate-y-1/2 hidden group-hover:flex gap-2 text-slate-400">
                                            <button className="hover:text-amber-500"><Pin size={14} /></button>
                                            <button className="hover:text-blue-500"><Edit2 size={14} /></button>
                                            <button className="hover:text-red-500"><Trash2 size={14} /></button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>

                {/* 3. User Footer */}
                <div className="p-4 border-t border-slate-100 bg-white flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
                        <User size={20} />
                    </div>
                    <div className="flex-1 min-w-0">
                        <div className="font-medium text-slate-900 truncate">Frank Chen</div>
                        <div className="flex items-center gap-2 mt-0.5">
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-gradient-to-r from-purple-500 to-blue-500 text-white font-bold tracking-wide">
                                PRO
                            </span>
                        </div>
                    </div>
                    <button 
                        onClick={onSettingsClick}
                        className="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-50"
                    >
                        <Settings size={20} />
                    </button>
                </div>
            </motion.div>
        </>
    );
};
