// React unused
import { motion } from 'framer-motion';
import {
    Battery,
    Wifi,
    Pin,
    Settings,
    User,
    Calendar,
    CalendarDays,
    Archive,
    MoreHorizontal,
    ChevronDown,
    ChevronRight,
} from 'lucide-react';

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
                    className="absolute inset-0 bg-black/35 z-40"
                />
            )}

            {/* Drawer Content */}
            <motion.div
                initial={{ x: '-100%' }}
                animate={{ x: isOpen ? 0 : '-100%' }}
                transition={{ type: 'spring', damping: 25, stiffness: 200 }}
                className="absolute top-0 bottom-0 left-0 w-[85%] max-w-[320px] bg-prism-bg z-50 shadow-2xl flex flex-col overflow-hidden border-r border-prism-border"
            >
                <div className="absolute top-0 right-0 h-72 w-72 rounded-full bg-prism-accent/8 blur-3xl -translate-y-1/3 translate-x-1/3" />
                <div className="absolute bottom-0 left-0 h-64 w-64 rounded-full bg-prism-knot/10 blur-3xl translate-y-1/3 -translate-x-1/3" />

                <div className="relative z-10 flex h-full flex-col">
                    {/* 1. Device Header */}
                    <div className="px-4 pt-12">
                        <div className="w-full rounded-full border border-prism-border bg-prism-surface/90 px-4 py-3 shadow-[0_18px_36px_-20px_rgba(15,23,42,0.28)] backdrop-blur-2xl">
                            <div className="flex items-center gap-3 text-sm">
                                <span className="flex items-center gap-1.5 font-semibold text-green-600">
                                    <Battery size={16} />
                                    85%
                                </span>
                                <span className="h-3 w-px bg-black/10" />
                                <span className="flex items-center gap-1.5 font-semibold text-prism-primary">
                                    <Wifi size={16} />
                                    SmartBadge
                                </span>
                                <span className="ml-auto text-xs font-medium text-prism-secondary">
                                    • 正常
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* 2. Session List (Scrollable) */}
                    <div className="flex-1 overflow-y-auto px-4 pb-28 pt-6">
                        <div className="space-y-4">
                            {(Object.entries(SESSIONS) as [string, Session[]][]).map(([key, group], index) => {
                                const isCollapsed = key === 'today';
                                const meta =
                                    key === 'pinned'
                                        ? { icon: <Pin size={14} />, label: '置顶', tint: 'text-amber-500' }
                                        : key === 'today'
                                          ? { icon: <Calendar size={14} />, label: '今天', tint: 'text-blue-500' }
                                          : key === 'recent'
                                            ? { icon: <CalendarDays size={14} />, label: '最近30天', tint: 'text-indigo-500' }
                                            : { icon: <Archive size={14} />, label: '2025-12', tint: 'text-prism-secondary' };

                                return (
                                    <div
                                        key={key}
                                        className="overflow-hidden rounded-[24px] border border-prism-border bg-prism-surface/90 shadow-[0_20px_40px_-24px_rgba(15,23,42,0.25)] backdrop-blur-2xl"
                                    >
                                        <div className="flex items-center justify-between bg-prism-surface-muted/70 px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                <span className={meta.tint}>{meta.icon}</span>
                                                <span className="text-[11px] font-bold tracking-[0.22em] text-prism-primary">
                                                    {meta.label}
                                                </span>
                                            </div>
                                            {isCollapsed ? (
                                                <ChevronRight size={16} className="text-prism-secondary/70" />
                                            ) : (
                                                <ChevronDown size={16} className="text-prism-secondary/70" />
                                            )}
                                        </div>

                                        {!isCollapsed && (
                                            <div className="divide-y divide-prism-border/70">
                                                {group.map((session, sessionIndex) => (
                                                    <button
                                                        key={session.id}
                                                        onClick={() => onSelectSession(session.id)}
                                                        className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-prism-surface-muted/70"
                                                    >
                                                        <span className={`h-9 w-1 rounded-full ${sessionIndex === 0 && index === 0 ? 'bg-prism-accent/30' : 'bg-prism-border/70'}`} />
                                                        <div className="min-w-0 flex-1">
                                                            <div className="truncate text-sm font-semibold text-prism-primary">
                                                                {session.title}
                                                            </div>
                                                            <div className="truncate text-xs text-prism-secondary">
                                                                {session.summary}
                                                            </div>
                                                        </div>
                                                        <span className="rounded-full border border-prism-border/70 bg-prism-surface-muted/80 p-1.5 text-prism-secondary">
                                                            <MoreHorizontal size={14} />
                                                        </span>
                                                    </button>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    {/* 3. User Footer */}
                    <div className="pointer-events-none absolute bottom-0 left-0 right-0 px-4 pb-8">
                        <div className="pointer-events-auto flex items-center gap-3 rounded-[24px] border border-prism-border bg-prism-surface/92 px-4 py-3 shadow-[0_22px_44px_-26px_rgba(15,23,42,0.28)] backdrop-blur-2xl">
                            <button className="flex min-w-0 flex-1 items-center gap-3 text-left">
                                <div className="flex h-11 w-11 items-center justify-center rounded-full bg-gradient-to-br from-white to-zinc-100 text-prism-secondary ring-1 ring-black/5">
                                    <User size={20} />
                                </div>
                                <div className="min-w-0">
                                    <div className="truncate text-sm font-bold text-prism-primary">Frank Chen</div>
                                    <div className="mt-0.5 flex items-center gap-2">
                                        <span className="rounded-full border border-prism-accent/15 bg-prism-accent/8 px-2 py-0.5 text-[10px] font-bold tracking-[0.18em] text-prism-accent">
                                            PRO
                                        </span>
                                    </div>
                                </div>
                            </button>
                            <button
                                onClick={onSettingsClick}
                                className="rounded-full border border-prism-border/70 bg-prism-surface-muted/80 p-2 text-prism-secondary transition-colors hover:text-prism-primary"
                            >
                                <Settings size={16} />
                            </button>
                        </div>
                    </div>
                </div>
            </motion.div>
        </>
    );
};
