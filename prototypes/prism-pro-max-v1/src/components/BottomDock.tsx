import React, { useState } from 'react';
import { Plus, Mic, Microscope, MessageSquare } from 'lucide-react';
import { clsx } from 'clsx';
import { motion } from 'framer-motion';

export const BottomDock = () => {
  const [activeMode, setActiveMode] = useState<'coach' | 'analyst'>('analyst');

  return (
    <div className="absolute bottom-0 left-0 right-0 p-6 z-50">
      
      {/* Stacked Layout: Switcher Top, Input Bottom */}
      <div className="flex flex-col gap-3">

        {/* Floating Mode Switcher */}
        <div className="self-center bg-prism-surface/50 backdrop-blur-xl rounded-full p-1 border border-prism-border/50 shadow-sm flex items-center gap-1 mb-2">
            
            <button 
                onClick={() => setActiveMode('coach')}
                className={clsx(
                    "px-4 py-2 rounded-full text-sm font-medium transition-all flex items-center gap-2",
                    activeMode === 'coach' 
                        ? "bg-black text-white shadow-md glow-sm" 
                        : "text-prism-secondary hover:bg-black/5"
                )}
            >
                <MessageSquare size={14} />
                Coach
            </button>
            
            <button 
                onClick={() => setActiveMode('analyst')}
                className={clsx(
                    "px-4 py-2 rounded-full text-sm font-medium transition-all flex items-center gap-2",
                    activeMode === 'analyst' 
                        ? "bg-blue-600 text-white shadow-md glow-blue-sm" 
                        : "text-prism-secondary hover:bg-black/5"
                )}
            >
                <Microscope size={14} />
                Analyst
            </button>
        </div>

        {/* Unified Input Capsule */}
        <div className="w-full h-16 bg-prism-surface/90 backdrop-blur-3xl rounded-[32px] shadow-glass border-retina border-prism-border flex items-center px-2 gap-2 transition-transform hover:scale-[1.005]">
            
            {/* Plus Action */}
            <button className="w-12 h-12 flex items-center justify-center rounded-full hover:bg-black/5 active:scale-95 transition-colors text-prism-secondary">
                 <Plus size={24} strokeWidth={2} />
            </button>

            {/* Input Field */}
            <div className="flex-1">
                 <input 
                    type="text" 
                    placeholder="输入消息..." 
                    className="w-full bg-transparent border-none outline-none text-prism-primary text-lg placeholder:text-prism-secondary/50 font-medium"
                 />
            </div>

            {/* Mic FAB (Right) */}
            <button className="w-12 h-12 bg-black text-white rounded-full flex items-center justify-center shadow-lg active:scale-95 transition-transform">
                 <Mic size={20} />
            </button>

        </div>
      </div>

      {/* Bottom Spacer for Home Indicator */}
      <div className="h-4 w-full" />
    </div>
  );
};
