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

  const [currentWeekStart, setCurrentWeekStart] = React.useState(() => {
    const d = new Date();
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust when day is Sunday
    return new Date(d.setDate(diff));
  });

  const [selectedDate, setSelectedDate] = React.useState(new Date().getDate());

  const days = React.useMemo(() => {
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(currentWeekStart);
      d.setDate(d.getDate() + i);
      return d;
    });
  }, [currentWeekStart]);

  const changeWeek = (offset: number) => {
    const newStart = new Date(currentWeekStart);
    newStart.setDate(newStart.getDate() + (offset * 7));
    setCurrentWeekStart(newStart);
  };

  return (
    <motion.div
      initial={{ y: '100%' }} // Start from bottom if it's a drawer
      animate={{ y: isOpen ? 0 : '100%' }} // Animate up
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      onDragEnd={handleDragEnd}
      drag="y"
      dragConstraints={{ top: -800, bottom: 0 }}
      dragElastic={0.1}
      className="absolute top-0 left-0 right-0 h-[95%] bg-white/95 backdrop-blur-3xl rounded-b-[40px] shadow-2xl z-40 border-b border-white/20 flex flex-col pt-12 text-gray-900"
    >
        {/* Handle Bar */}
        <div className="w-full flex justify-center pb-2 cursor-grab active:cursor-grabbing" onClick={onClose}>
            <div className="w-16 h-1.5 bg-gray-300 rounded-full" />
        </div>

        {/* Month Header & Week Nav */}
        <div className="px-6 py-4 flex justify-between items-center">
            <h2 className="text-2xl font-bold">
                {currentWeekStart.getFullYear()}年 {currentWeekStart.getMonth() + 1}月
            </h2>
            <div className="flex gap-2">
                <button onClick={() => changeWeek(-1)} className="p-2 hover:bg-gray-100 rounded-full text-gray-600">
                    ←
                </button>
                <button onClick={() => changeWeek(1)} className="p-2 hover:bg-gray-100 rounded-full text-gray-600">
                    →
                </button>
            </div>
        </div>

        {/* Fixed Week View (Mon-Sun) */}
        <div className="px-4 py-2 flex justify-between items-center border-b border-gray-100 pb-4">
            {days.map((d, i) => {
                const isSelected = d.getDate() === selectedDate;
                return (
                    <div 
                        key={i} 
                        onClick={() => setSelectedDate(d.getDate())}
                        className={clsx(
                            "flex flex-col items-center justify-center w-11 h-16 rounded-2xl shrink-0 transition-all cursor-pointer",
                            isSelected 
                                ? "bg-prism-accent text-white shadow-lg shadow-blue-500/30 scale-105" 
                                : "bg-transparent text-gray-500 hover:bg-gray-50"
                        )}
                    >
                        <span className="text-xs font-medium mb-1">
                            {['一','二','三','四','五','六','日'][i]}
                        </span>
                        <span className={clsx("text-lg font-bold", isSelected ? "text-white" : "text-gray-900")}>
                            {d.getDate()}
                        </span>
                    </div>
                );
            })}
        </div>

        {/* Content Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
            
            {/* Task Card 1 */}
            <div className="flex gap-4">
                <span className="text-xs text-gray-400 font-mono pt-2">09:00</span>
                <div className="flex-1 p-4 bg-white rounded-2xl border border-gray-100 shadow-sm">
                    <div className="flex justify-between items-start mb-1">
                         <h3 className="font-semibold text-gray-900">审查 Q4 预算</h3>
                         <MoreHorizontal size={16} className="text-gray-400" />
                    </div>
                    <p className="text-xs text-gray-500 mb-3 flex items-center gap-1">
                        <Clock size={12} /> 45 分钟
                    </p>
                    <div className="flex gap-2">
                         <span className="px-2 py-1 bg-green-100 text-green-700 text-[10px] rounded-md font-medium">财务</span>
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
                            日程冲突
                         </h3>
                    </div>
                    <p className="text-sm text-gray-600 mb-3">
                        项目同步会与客户电话会议冲突。
                    </p>
                    <button className="bg-white px-3 py-1.5 rounded-lg text-xs font-medium text-gray-900 border border-gray-200 shadow-sm">
                        重新安排
                    </button>
                </div>
            </div>

        </div>

    </motion.div>
  );
};
