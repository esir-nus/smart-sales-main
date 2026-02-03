import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { AlertTriangle } from 'lucide-react';
import { clsx } from 'clsx';
import { ChatExpandedView } from './ChatExpandedView';

export const ConflictCard = ({ item }: { item: any }) => {
    const [expanded, setExpanded] = React.useState(false);

    return (
        <motion.div 
            onClick={() => !expanded && setExpanded(true)}
            animate={{ height: expanded ? 'auto' : 56 }}
            className={clsx(
                "bg-prism-surface backdrop-blur-xl rounded-[20px] overflow-hidden shadow-sm border-retina border-orange-200/50 transition-all relative",
                expanded ? "z-50" : "z-10"
            )}
        >
             {/* Collapsed State Header / Expanded Header */}
             <div className="flex items-center gap-3 px-4 h-14 w-full relative z-10">
                <AlertTriangle size={18} className="text-orange-500 shrink-0" />
                <span className="text-sm font-medium text-gray-800 tracking-tight truncate w-full pr-4">
                    {item.title}
                </span>
                
                {/* Watermark (Always visible) */}
                <div className="absolute right-[-10px] bottom-[-10px] opacity-10 rotate-12 pointer-events-none">
                    <AlertTriangle size={64} className="text-orange-600" />
                </div>
             </div>

             {/* Expanded Dialog Content */}
             <AnimatePresence>
                {expanded && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="pb-2 pt-0 bg-white/50 backdrop-blur-sm"
                    >
                        <ChatExpandedView 
                            initialMessage="⚠️ 发现日程冲突。'团队午餐' (12:00) 优先级较低。建议保留 '审查预算' (12:00)。是否自动调整午餐时间？"
                            onSend={(text) => console.log('Conflict resolution:', text)}
                        />
                    </motion.div>
                )}
             </AnimatePresence>
        </motion.div>
    );
};
