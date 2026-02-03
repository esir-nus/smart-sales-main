// import React from 'react';
import { motion } from 'framer-motion';
import { Sparkles, Check } from 'lucide-react';
import { clsx } from 'clsx';

export const InspirationCard = ({ 
    item, 
    selectionMode, 
    isSelected, 
    onEnterMode, 
    onToggle 
}: { 
    item: any,
    selectionMode: boolean,
    isSelected: boolean,
    onEnterMode: () => void,
    onToggle: () => void
}) => {

    return (
        <div className={clsx(
            "p-4 rounded-2xl border shadow-glass relative overflow-hidden transition-all duration-300",
            selectionMode && isSelected ? "bg-prism-surface border-prism-accent" : "bg-prism-surface border-prism-border",
            selectionMode && !isSelected && "opacity-60 grayscale-[0.5]" // Dim unselected
        )}>
            <div className="flex items-start gap-3 relative z-10">
                {/* Checkbox (Visible only in Multi-Select) */}
                {selectionMode && (
                    <motion.div 
                        initial={{ scale: 0, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        className="mt-1"
                    >
                        <button 
                            onClick={(e) => {
                                e.stopPropagation();
                                onToggle();
                            }}
                            className={clsx(
                                "w-5 h-5 rounded-full border flex items-center justify-center transition-all",
                                isSelected ? "bg-purple-600 border-purple-600" : "border-purple-300 bg-white"
                            )}
                        >
                            {isSelected && <Check size={12} className="text-white" />}
                        </button>
                    </motion.div>
                )}

                <div className="flex-1">
                    <h3 className="font-semibold text-purple-900 flex items-center gap-2 pr-8">
                         {!selectionMode && "💡"} {item.title}
                    </h3>
                </div>

                {/* Intelligent Action - Icon Only (Triggers Multi-Select) */}
                {!selectionMode && (
                    <button 
                        onClick={(e) => {
                            e.stopPropagation();
                            onEnterMode();
                        }}
                        className="absolute right-0 top-0 w-8 h-8 rounded-full bg-white shadow-sm flex items-center justify-center text-purple-600 hover:scale-110 active:scale-95 transition-all"
                    >
                        <Sparkles size={16} fill="currentColor" className="opacity-80" />
                    </button>
                )}
            </div>
            
            {/* Ambient Decor */}
            <div className="absolute -right-4 -bottom-4 w-20 h-20 bg-purple-200/30 rounded-full blur-2xl pointer-events-none" />
            
            {/* Click to Toggle Body (Only in Selection Mode) */}
            {selectionMode && (
                <div 
                    className="absolute inset-0 z-0 cursor-pointer"
                    onClick={onToggle}
                />
            )}
        </div>
    );
};
