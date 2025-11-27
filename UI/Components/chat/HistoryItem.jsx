import React, { useRef } from 'react';
import { MessageSquare, ChevronRight, Pin } from 'lucide-react';
import { motion } from 'framer-motion';

export default function HistoryItem({ item, onClick, onLongPress }) {
  const timerRef = useRef(null);
  const isLongPress = useRef(false);

  const handleStart = () => {
    isLongPress.current = false;
    timerRef.current = setTimeout(() => {
      isLongPress.current = true;
      if (window.navigator.vibrate) window.navigator.vibrate(50); // Haptic feedback
      onLongPress(item);
    }, 500);
  };

  const handleEnd = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
  };

  const handleClick = (e) => {
    if (isLongPress.current) {
      e.preventDefault();
      e.stopPropagation();
      isLongPress.current = false;
      return;
    }
    onClick(item);
  };

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2 }}
      onMouseDown={handleStart}
      onMouseUp={handleEnd}
      onMouseLeave={handleEnd}
      onTouchStart={handleStart}
      onTouchEnd={handleEnd}
      onClick={handleClick}
      className={`bg-white rounded-xl p-4 flex items-center justify-between hover:bg-[#FAFAFA] active:bg-[#E5E5EA] transition-all duration-200 cursor-pointer border shadow-sm select-none ${item.isPinned ? 'border-l-4 border-l-[#007AFF] border-[#E5E5EA]' : 'border-[#E5E5EA]'}`}
    >
      <div className="flex items-center gap-4 overflow-hidden">
        {/* Icon Container */}
        <div className="w-12 h-12 bg-[#F2F2F7] rounded-xl flex items-center justify-center flex-shrink-0 border border-[#E5E5EA] relative shadow-sm">
          <MessageSquare size={20} className="text-[#8E8E93]" />
          {item.isPinned && (
            <div className="absolute -top-1 -right-1 bg-[#007AFF] rounded-full p-1 border-2 border-white shadow-sm">
              <Pin size={8} className="text-white" fill="currentColor" />
            </div>
          )}
        </div>
        
        {/* Text Content */}
        <div className="flex items-baseline gap-2 truncate">
          <span className="text-black font-medium text-[15px] whitespace-nowrap">{item.date}</span>
          <span className="text-[#636366] text-[15px] truncate">{item.title}</span>
        </div>
      </div>

      {/* Arrow */}
      <ChevronRight size={20} className="text-[#C7C7CC] flex-shrink-0 ml-2" />
    </motion.div>
  );
}