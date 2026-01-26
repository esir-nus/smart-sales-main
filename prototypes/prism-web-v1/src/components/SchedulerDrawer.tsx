import React from 'react';
import { motion, type PanInfo, useAnimation } from 'framer-motion';
import { MoreHorizontal, Clock, AlertTriangle } from 'lucide-react';
import { clsx } from 'clsx';

interface SchedulerDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SchedulerDrawer: React.FC<SchedulerDrawerProps> = ({ isOpen, onClose }) => {
  const controls = useAnimation();
  
  // Handle drag end to snap open/close
  const handleDragEnd = (_event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    if (info.offset.y < -100) {
      onClose();
    } else {
      controls.start({ y: 0 });
    }
  };

  // Mock Date Pills
  const days = Array.from({ length: 7 }, (_, i) => 22 + i);

  return (
    <motion.div
      initial={{ y: '-100%' }}
      animate={{ y: isOpen ? 0 : '-100%' }}
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      onDragEnd={handleDragEnd}
      drag="y"
      dragConstraints={{ top: -600, bottom: 0 }}
      dragElastic={0.1}
      className="absolute top-0 left-0 right-0 h-[650px] bg-white/95 backdrop-blur-3xl rounded-b-[40px] shadow-2xl z-40 border-b border-white/20 flex flex-col pt-12 text-gray-900"
    >
        {/* Handle Bar */}
        <div className="w-full flex justify-center pb-2 cursor-grab active:cursor-grabbing" onClick={onClose}>
            <div className="w-16 h-1.5 bg-gray-300 rounded-full" />
        </div>

        {/* Month Carousel */}
        <div className="px-6 py-2 overflow-x-auto flex gap-4 no-scrollbar">
            {['Jan', 'Feb', 'Mar', 'Apr', 'May'].map((m, i) => (
                <span key={m} className={clsx(
                    "font-semibold text-lg px-2 py-1 rounded-full",
                    i === 2 ? "bg-black text-white" : "text-gray-400"
                )}>{m}</span>
            ))}
        </div>

        {/* Day Carousel */}
        <div className="px-6 py-2 overflow-x-auto flex gap-3 no-scrollbar border-b border-gray-100 pb-4">
            {days.map((d, i) => (
                <div key={d} className={clsx(
                    "flex flex-col items-center justify-center w-12 h-16 rounded-2xl shrink-0 transition-colors",
                    i === 2 ? "bg-prism-accent text-white shadow-lg shadow-blue-500/30" : "bg-gray-50 text-gray-500"
                )}>
                    <span className="text-xs">{['M','T','W','T','F','S','S'][i]}</span>
                    <span className="text-xl font-bold">{d}</span>
                </div>
            ))}
        </div>

        {/* Content Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
            
            {/* Task Card 1 */}
            <div className="flex gap-4">
                <span className="text-xs text-gray-400 font-mono pt-2">09:00</span>
                <div className="flex-1 p-4 bg-white rounded-2xl border border-gray-100 shadow-sm">
                    <div className="flex justify-between items-start mb-1">
                         <h3 className="font-semibold text-gray-900">Review Q4 Budget</h3>
                         <MoreHorizontal size={16} className="text-gray-400" />
                    </div>
                    <p className="text-xs text-gray-500 mb-3 flex items-center gap-1">
                        <Clock size={12} /> 45 min
                    </p>
                    <div className="flex gap-2">
                         <span className="px-2 py-1 bg-green-100 text-green-700 text-[10px] rounded-md font-medium">Finance</span>
                    </div>
                </div>
            </div>

            {/* Mock Conflict (Static for now) */}
             <div className="flex gap-4">
                <span className="text-xs text-gray-400 font-mono pt-2">11:00</span>
                <div className="flex-1 p-4 bg-orange-50/50 rounded-2xl border border-orange-100 shadow-sm relative overflow-hidden">
                    <div className="absolute left-0 top-0 bottom-0 w-1 bg-orange-400" />
                    <div className="flex justify-between items-start mb-2">
                         <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                            <AlertTriangle size={16} className="text-orange-500" /> 
                            Conflict Detected
                         </h3>
                    </div>
                    <p className="text-sm text-gray-600 mb-3">
                        Project Sync clashes with Client Call.
                    </p>
                    <button className="bg-white px-3 py-1.5 rounded-lg text-xs font-medium text-gray-900 border border-gray-200 shadow-sm">
                        Reschedule
                    </button>
                </div>
            </div>

        </div>

    </motion.div>
  );
};
