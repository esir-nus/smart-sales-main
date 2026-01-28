import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Play, Plus, RefreshCw, Smartphone, Sparkles, Star, Trash2, ChevronDown, ChevronUp, Cloud } from 'lucide-react';
import { clsx } from 'clsx';

type AudioFile = {
    id: string;
    name: string;
    duration: string;
    date: string;
    fullDate: string; // Added for expanded view
    status: 'transcribing' | 'completed' | 'not_started';
    progress?: number;
    preview?: string;
    fullTranscript?: string; // Added for expanded view
    source: string;
    isStarred?: boolean;
};

// Date Formatter Helper
const getFormattedTime = (fullDateStr?: string, fallbackDate?: string) => {
    // If no fullDate, use fallback (legacy mock data behavior)
    if (!fullDateStr) return fallbackDate || '';

    const date = new Date(fullDateStr);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    // Check if it's today (simple check)
    const isToday = date.getDate() === now.getDate() && 
                    date.getMonth() === now.getMonth() && 
                    date.getFullYear() === now.getFullYear();

    if (isToday) {
        return date.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
        return '1 day';
    } else if (diffDays > 1 && diffDays <= 7) {
        return `${diffDays} days`;
    } else {
        // MM-DD
        return `${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
    }
};

// Mock Data
const AUDIO_FILES: AudioFile[] = [
    {
        id: '1',
        name: 'Q4_年度预算会议.wav',
        duration: '14:20',
        date: 'Yesterday',
        fullDate: '2026-01-26 15:30',
        status: 'completed',
        preview: '财务部关于Q4预算的最终审核意见，重点讨论了SaaS订阅模式的成本结构。李总提出需要在下周一之前...',
        fullTranscript: '财务部关于Q4预算的最终审核意见，重点讨论了SaaS订阅模式的成本结构。李总提出需要在下周一之前完成初步的财务模型构建，并确保所有部门的预算缩减计划都在本月底前提交。张总补充说，市场部的推广费用不能一刀切，需要保留核心渠道的投入。',
        source: 'SmartBadge',
        isStarred: true
    },
    {
        id: '2',
        name: 'meeting_notes.wav',
        duration: '03:12',
        date: '10:15',
        fullDate: '2026-01-27 10:15',
        status: 'transcribing',
        progress: 45,
        source: 'Phone',
        isStarred: false
    },
    {
        id: '3',
        name: '客户拜访_张总_20260124.wav',
        duration: '08:45',
        date: '14:30',
        fullDate: '2026-01-27 14:30',
        status: 'not_started',
        source: 'SmartBadge',
        isStarred: false
    }
];

interface AudioDrawerProps {
    isOpen: boolean;
    onClose: () => void;
}

export const AudioDrawer = ({ isOpen, onClose }: AudioDrawerProps) => {
    // Mock Sync State
    const [isSyncing, setIsSyncing] = useState(false);
    // Track expanded card
    const [expandedId, setExpandedId] = useState<string | null>(null);

    useEffect(() => {
        if (isOpen) {
            setIsSyncing(true);
            const timer = setTimeout(() => setIsSyncing(false), 2000);
            return () => clearTimeout(timer);
        } else {
            setExpandedId(null); // Reset on close
        }
    }, [isOpen]);

    // Reset transcript fold state when expanding a new card
    const [isTranscriptFolded, setIsTranscriptFolded] = useState(true);
    useEffect(() => {
        setIsTranscriptFolded(true);
    }, [expandedId]);

    const toggleExpand = (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        setExpandedId(expandedId === id ? null : id);
    };

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    {/* Scrim */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 0.5 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="absolute inset-0 bg-black z-40"
                    />

                    {/* Drawer Content */}
                    <motion.div
                        initial={{ y: '100%' }}
                        animate={{ y: '0%' }}
                        exit={{ y: '100%' }}
                        transition={{ type: 'spring', damping: 25, stiffness: 200 }}
                        drag="y"
                        dragConstraints={{ top: 0 }}
                        dragElastic={0.2}
                        onDragEnd={(_, info) => {
                            if (info.offset.y > 100) onClose();
                        }}
                        className="absolute top-[5%] left-0 right-0 bottom-0 bg-white rounded-t-3xl z-50 shadow-2xl flex flex-col"
                    >
                        {/* 1. Drag Handle */}
                        <div className="w-full flex justify-center pt-3 pb-1 cursor-grab active:cursor-grabbing" onClick={onClose}>
                            <div className="w-12 h-1.5 bg-slate-200 rounded-full" />
                        </div>

                        {/* 2. Sync Header */}
                        <div className="px-6 py-4 flex items-center justify-between border-b border-slate-100">
                            <h2 className="text-lg font-bold text-slate-800">录音文件</h2>
                            <div className={clsx("flex items-center gap-2 text-xs font-medium px-3 py-1.5 rounded-full transition-colors", 
                                isSyncing ? "bg-blue-50 text-blue-600" : "bg-green-50 text-green-600"
                            )}>
                                {isSyncing ? (
                                    <>
                                        <RefreshCw size={12} className="animate-spin" />
                                        <span>同步中...</span>
                                    </>
                                ) : (
                                    <>
                                        <Smartphone size={12} />
                                        <span>SmartBadge 已同步</span>
                                    </>
                                )}
                            </div>
                        </div>

                        {/* 3. Audio List */}
                        <div className="flex-1 overflow-y-auto p-4 space-y-4 pb-24">
                            {AUDIO_FILES.map(file => {
                                const isExpanded = expandedId === file.id;

                                return (
                                    <div key={file.id} className="relative group">
                                        {/* Swipe Action Background (Delete) */}
                                        <div className="absolute inset-0 bg-red-500 rounded-xl flex items-center justify-end px-6 text-white mb-4">
                                            <Trash2 size={20} />
                                        </div>

                                        <motion.div 
                                            drag={file.status !== 'transcribing' && !isExpanded ? "x" : false}
                                            dragConstraints={{ left: 0, right: 0 }}
                                            dragElastic={{ left: 0.5, right: 0.1 }}
                                            onDragEnd={(_, info) => {
                                                if (info.offset.x < -100) {
                                                    // Trigger Delete (Mock)
                                                    console.log('Delete item', file.id);
                                                    // In real app: setFiles(prev => prev.filter(f => f.id !== file.id))
                                                }
                                            }}
                                            onClick={(e) => file.status === 'completed' && toggleExpand(file.id, e)}
                                            className={clsx(
                                                "bg-white border border-slate-100 rounded-xl shadow-sm relative overflow-hidden transition-all duration-300 z-10",
                                                file.status === 'completed' ? "cursor-pointer hover:border-blue-200" : "",
                                                isExpanded ? "ring-2 ring-blue-100 border-blue-200" : ""
                                            )}
                                            style={{ touchAction: "pan-y" }} // Allow vertical scroll execution
                                        >
                                            <div className="p-4">
                                                {/* Header Row */}
                                                <div className="flex items-start gap-3 mb-2">
                                                    {/* Left Column: Star/Icon (Fixed) */}
                                                    <div className="flex flex-col items-center gap-2 shrink-0 w-10">
                                                        <div className={clsx("w-10 h-10 rounded-full flex items-center justify-center shrink-0 transition-colors cursor-pointer hover:bg-slate-50",
                                                            file.isStarred ? "bg-blue-50 text-blue-500" : "bg-slate-50 text-slate-300"
                                                        )}>
                                                            <Star size={20} className={clsx("transition-all", file.isStarred ? "fill-current scale-110" : "")} />
                                                        </div>
                                                        
                                                        {/* Source Icon (Below Star) */}
                                                        <div className="text-slate-400" title={`Source: ${file.source}`}>
                                                            {file.source === 'SmartBadge' ? <Cloud size={14} /> : <Smartphone size={14} />}
                                                        </div>
                                                    </div>

                                                    {/* Middle: Name & Timestamp */}
                                                    <div className="flex-1 min-w-0">
                                                        <div className="flex justify-between items-start">
                                                            <div className="font-medium text-slate-900 text-sm truncate mr-2">{file.name}</div>
                                                            <div className="text-xs text-slate-400 font-mono whitespace-nowrap pt-0.5">
                                                                {getFormattedTime(file.fullDate, file.date)}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>

                                                {/* EXPANDED CONTENT: Metadata & Ask AI */}
                                                {isExpanded && (
                                                    <motion.div 
                                                        initial={{ opacity: 0, height: 0 }}
                                                        animate={{ opacity: 1, height: 'auto' }}
                                                        className="mb-4 pt-1 pb-3 border-b border-slate-50 flex justify-between items-center"
                                                    >
                                                        <div className="space-y-1 text-xs text-slate-500">
                                                            <div className="flex items-center gap-2">
                                                                <span>📅</span>
                                                                <span>{file.fullDate || '2026-01-26 15:30'}</span>
                                                            </div>
                                                            <div className="flex items-center gap-2">
                                                                <span>📁</span>
                                                                <span>来源：{file.source}</span>
                                                            </div>
                                                        </div>
                                                        <button 
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                console.log('Ask AI about', file.id);
                                                            }}
                                                            className="flex items-center gap-1.5 px-3 py-1.5 bg-indigo-50 text-indigo-600 rounded-lg text-xs font-medium hover:bg-indigo-100 transition-colors"
                                                        >
                                                            <Sparkles size={12} />
                                                            问AI
                                                        </button>
                                                    </motion.div>
                                                )}

                                                {/* Body Content */}
                                                <div className="pl-[3.25rem]">
                                                    {/* State: Not Started */}
                                                    {file.status === 'not_started' && (
                                                        <div className="h-9 bg-slate-50 rounded-lg flex items-center px-4 text-xs text-slate-400 relative overflow-hidden cursor-pointer hover:bg-slate-100 transition-colors border border-slate-100/50">
                                                            <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/50 to-transparent w-1/2 skew-x-12 translate-x-[-150%] animate-[shimmer_2s_infinite]" />
                                                            <span className="font-medium text-slate-500">右滑转写 &gt;&gt;&gt;</span>
                                                        </div>
                                                    )}

                                                    {/* State: Transcribing */}
                                                    {file.status === 'transcribing' && (
                                                        <div className="space-y-2">
                                                            <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                                                                <motion.div 
                                                                    className="h-full bg-amber-400 rounded-full"
                                                                    initial={{ width: '0%' }}
                                                                    animate={{ width: `${file.progress}%` }}
                                                                    transition={{ duration: 2, repeat: Infinity, repeatType: "reverse" }}
                                                                />
                                                            </div>
                                                            <div className="flex justify-between text-[10px] text-slate-400 font-mono">
                                                                <span className="text-amber-500 font-medium">转写中...</span>
                                                                <span>{file.progress}%</span>
                                                            </div>
                                                        </div>
                                                    )}

                                                    {/* State: Completed */}
                                                    {file.status === 'completed' && (
                                                        <div className="space-y-3">
                                                            {/* Transcript Section */}
                                                            {isExpanded ? (
                                                                <div className="bg-slate-50 rounded-lg border border-slate-100/50 overflow-hidden">
                                                                    <div 
                                                                        className="px-3 py-2 flex justify-between items-center cursor-pointer bg-slate-50/50 hover:bg-slate-100/50 transition-colors"
                                                                        onClick={(e) => {
                                                                            e.stopPropagation();
                                                                            setIsTranscriptFolded(!isTranscriptFolded);
                                                                        }}
                                                                    >
                                                                        <span className="text-[10px] font-medium text-slate-400">转写内容</span>
                                                                        {isTranscriptFolded ? <ChevronDown size={14} className="text-slate-400"/> : <ChevronUp size={14} className="text-slate-400"/>}
                                                                    </div>
                                                                    <AnimatePresence initial={false}>
                                                                        <motion.div
                                                                            initial={{ height: 'auto' }}
                                                                            animate={{ height: 'auto' }}
                                                                            exit={{ height: 0 }}
                                                                            className="px-3 pb-3 text-xs text-slate-600 leading-relaxed"
                                                                        >
                                                                            <div className={clsx("transition-all", isTranscriptFolded ? "line-clamp-3" : "")}>
                                                                                {file.fullTranscript || file.preview}
                                                                            </div>
                                                                            {isTranscriptFolded && (
                                                                                <div 
                                                                                    className="mt-1 text-center text-[10px] text-blue-500 font-medium cursor-pointer hover:text-blue-600"
                                                                                    onClick={(e) => {
                                                                                        e.stopPropagation();
                                                                                        setIsTranscriptFolded(false);
                                                                                    }}
                                                                                >
                                                                                    查看全部
                                                                                </div>
                                                                            )}
                                                                        </motion.div>
                                                                    </AnimatePresence>
                                                                </div>
                                                            ) : (
                                                                <div className="text-xs text-slate-600 leading-relaxed truncate bg-slate-50 p-2 rounded-lg border border-slate-100/50">
                                                                    {file.preview}
                                                                </div>
                                                            )}

                                                            {/* Player Bar - ONLY VISIBLE WHEN EXPANDED */}
                                                            {isExpanded && (
                                                                <motion.div 
                                                                    initial={{ opacity: 0, y: -5 }}
                                                                    animate={{ opacity: 1, y: 0 }}
                                                                    className="h-10 bg-white rounded-full flex items-center px-1 gap-2 border border-slate-100 shadow-sm mt-2"
                                                                >
                                                                    <button className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center hover:bg-slate-200">
                                                                        <Play size={12} className="text-slate-600 fill-current ml-0.5" />
                                                                    </button>
                                                                    <div className="h-1.5 flex-1 bg-slate-100 rounded-full relative mx-2">
                                                                        <div className="absolute left-0 top-0 bottom-0 w-1/3 bg-blue-500 rounded-full" />
                                                                        <div className="absolute left-1/3 top-1/2 -translate-y-1/2 w-3 h-3 bg-white border-2 border-blue-500 rounded-full shadow-sm" />
                                                                    </div>
                                                                    <span className="text-[10px] font-mono text-slate-400 pr-3">04:20 / {file.duration}</span>
                                                                </motion.div>
                                                            )}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        </motion.div>
                                    </div>
                                );
                            })}
                        </div>

                        {/* 4. Upload Footer */}
                        <div className="p-4 border-t border-slate-100 bg-white pb-8 absolute bottom-0 left-0 right-0 rounded-t-2xl shadow-[0_-5px_20px_-10px_rgba(0,0,0,0.1)]">
                            <button className="w-full py-3 rounded-xl border-2 border-dashed border-slate-200 text-slate-400 font-medium text-sm flex items-center justify-center gap-2 hover:bg-slate-50 hover:border-slate-300 hover:text-slate-500 transition-colors">
                                <Plus size={18} />
                                上传本地音频
                            </button>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
};
