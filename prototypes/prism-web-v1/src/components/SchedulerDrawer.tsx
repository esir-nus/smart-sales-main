import React from 'react';
import { motion, type PanInfo, AnimatePresence } from 'framer-motion';
import { MoreHorizontal, Clock, AlertTriangle, ChevronDown, ChevronUp, GripHorizontal } from 'lucide-react';
import { clsx } from 'clsx';

interface SchedulerDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

const slideVariants = {
  enter: (direction: number) => ({
    x: direction > 0 ? 300 : -300,
    opacity: 0
  }),
  center: {
    x: 0,
    opacity: 1
  },
  exit: (direction: number) => ({
    x: direction < 0 ? 300 : -300,
    opacity: 0
  })
};

export const SchedulerDrawer: React.FC<SchedulerDrawerProps> = ({ isOpen, onClose }) => { 
  const [isExpanded, setIsExpanded] = React.useState(false);
  const [direction, setDirection] = React.useState(0);
  
  const [currentWeekStart, setCurrentWeekStart] = React.useState(() => {
    const d = new Date();
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust when day is Sunday
    return new Date(d.setDate(diff));
  });

  const [selectedDate, setSelectedDate] = React.useState(new Date().getDate());

  // Toggle Expansion behavior
  const handleDragEnd = (_event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    // Top-down pull to expand
    if (!isExpanded && info.offset.y > 50) {
      setIsExpanded(true);
      return;
    }
    // Bottom-up push to collapse
    if (isExpanded && info.offset.y < -50) {
      setIsExpanded(false);
      return;
    }
    // Close drawer logic (only if not expanded or huge swipe)
    if (!isExpanded && info.offset.y < -100) {
      onClose();
    } 
  };
  
  // Week Swipe Handler (Horizontal)
  const handleWeekSwipe = (_event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
      const swipeThreshold = 50;
      if (info.offset.x < -swipeThreshold) {
          changeWeek(1);
      } else if (info.offset.x > swipeThreshold) {
          changeWeek(-1);
      }
  };

  const changeWeek = (offset: number) => {
    setDirection(offset);
    const newStart = new Date(currentWeekStart);
    newStart.setDate(newStart.getDate() + (offset * 7));
    setCurrentWeekStart(newStart);
  };

  const days = React.useMemo(() => {
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(currentWeekStart);
      d.setDate(d.getDate() + i);
      return d;
    });
  }, [currentWeekStart]);

  // Generate Month Grid (Simple 5 weeks)
  const monthGrid = React.useMemo(() => {
    const start = new Date(currentWeekStart.getFullYear(), currentWeekStart.getMonth(), 1);
    const offset = start.getDay() === 0 ? 6 : start.getDay() - 1; // Start Mon
    start.setDate(start.getDate() - offset);
    
    return Array.from({ length: 35 }, (_, i) => {
      const d = new Date(start);
      d.setDate(d.getDate() + i);
      return d;
    });
  }, [currentWeekStart]);

  return (

    <motion.div
      initial={{ y: '-100%' }}
      animate={{ y: isOpen ? 0 : '-100%' }}
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      onDragEnd={handleDragEnd}
      drag="y"
      dragConstraints={{ top: -800, bottom: 0 }}
      dragElastic={0.1}
      className="absolute top-0 left-0 right-0 h-[95%] bg-white/95 backdrop-blur-2xl rounded-b-[40px] shadow-[0_20px_60px_-15px_rgba(0,0,0,0.3)] z-40 border-b border-white/20 flex flex-col pt-8 text-gray-900 overflow-hidden pb-8"
    >
        {/* Month Header & Carousel */}
        <div className="px-6 py-2 flex flex-col gap-4">
            <div className="flex justify-between items-center">
                 <h2 className="text-2xl font-bold flex items-center gap-2 tracking-tight">
                    <span className="text-gray-400 font-medium tracking-normal text-lg">{currentWeekStart.getFullYear()}年</span>
                    <span className="text-gray-900">{currentWeekStart.getMonth() + 1}月</span>
                </h2>
                {/* Arrow Buttons Removed in favor of Swipe */}
                <div className="flex gap-2 opacity-0 pointer-events-none w-0 h-0 overflow-hidden"> 
                   {/* Kept hidden for a11y structure but visually removed */}
                </div>
            </div>
            
            {/* Restored Month Carousel */}
             <div className="overflow-x-auto flex gap-3 no-scrollbar pb-2 mask-linear-fade">
                {Array.from({length: 12}, (_, i) => i + 1).map((m) => {
                    const isActive = (currentWeekStart.getMonth() + 1) === m;
                    return (
                        <button 
                            key={m} 
                            onClick={() => {
                            const d = new Date(currentWeekStart);
                            d.setMonth(m-1);
                            setCurrentWeekStart(d);
                            }}
                            className={clsx(
                            "font-medium text-[13px] px-4 py-2 rounded-full whitespace-nowrap transition-all duration-300",
                            isActive 
                                ? "bg-gray-900 text-white shadow-lg shadow-gray-900/20 scale-105" 
                                : "bg-gray-50 text-gray-500 hover:bg-gray-100"
                        )}>{m}月</button>
                    )
                })}
            </div>
        </div>

        {/* Expandable Calendar Grid */}
        <motion.div 
            animate={{ height: isExpanded ? 360 : 140 }} // Increased base height for handle
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
            className="overflow-hidden relative flex flex-col w-full"
        >
             {/* Grid */}
             <div className="px-4 grid grid-cols-7 gap-y-2 place-items-center w-full">
                {/* Headers */}
                {['一','二','三','四','五','六','日'].map(d => (
                    <div key={d} className="w-full text-center text-[10px] uppercase tracking-wider text-gray-400 font-semibold py-3 flex justify-center">{d}</div>
                ))}
                
                {/* Animated Dates Container */}
                <AnimatePresence mode="popLayout" custom={direction}>
                    <motion.div
                        key={isExpanded ? 'month-view' : currentWeekStart.toISOString()}
                        variants={slideVariants}
                        initial="enter"
                        animate="center"
                        exit="exit"
                        custom={direction}
                        drag={!isExpanded ? "x" : false} // Swipe only on Week View
                        dragConstraints={{ left: 0, right: 0 }}
                        dragElastic={0.2}
                        onDragEnd={handleWeekSwipe}
                        className="col-span-7 grid grid-cols-7 gap-y-2 w-full place-items-center"
                    >
                        {(isExpanded ? monthGrid : days).map((d, i) => {
                            const isSelected = d.getDate() === selectedDate;
                            const hasTask = d.getDate() % 3 === 0; 
                            const isCurrentMonth = d.getMonth() === currentWeekStart.getMonth();
                            
                            return (
                                <motion.div 
                                    layout
                                    key={`${d.getMonth()}-${d.getDate()}`} 
                                    onClick={() => setSelectedDate(d.getDate())}
                                    className={clsx(
                                        "flex flex-col items-center justify-center w-full h-12 rounded-2xl transition-all cursor-pointer relative group",
                                        !isSelected && "hover:bg-black/5"
                                    )}
                                >
                                    <div className={clsx(
                                        "w-9 h-9 flex items-center justify-center rounded-full transition-all duration-300 relative z-10",
                                        isSelected ? "bg-gray-900 text-white shadow-lg scale-110" : "text-gray-700",
                                        !isCurrentMonth && !isSelected && "text-gray-300"
                                    )}>
                                        <span className={clsx("text-sm font-medium", isSelected && "font-bold")}>
                                            {d.getDate()}
                                        </span>
                                    </div>

                                    {/* Task Indicator */}
                                    {hasTask && (
                                        <div className={clsx(
                                            "w-1 h-1 rounded-full mt-1 transition-all", 
                                            isSelected ? "bg-white/60" : "bg-blue-400 group-hover:scale-125"
                                        )} />
                                    )}
                                </motion.div>
                            );
                        })}
                    </motion.div>
                </AnimatePresence>
             </div>
             
             {/* Calendar Expansion Handle (Bottom Pill) */}
             <div 
                className="w-full flex justify-center py-4 mt-auto cursor-grab active:cursor-grabbing hover:bg-black/5 transition-all"
                onClick={() => setIsExpanded(!isExpanded)}
             >
                 <div className="w-8 h-1 bg-gray-200 rounded-full" />
             </div>
        </motion.div>

        {/* Content Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 pb-12">
            
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

        {/* Drawer Global Handle (Absolute Bottom) */}
        <div className="absolute bottom-3 left-0 right-0 flex justify-center cursor-grab active:cursor-grabbing hover:bg-black/5 py-2 transition-colors">
             <div className="w-16 h-1.5 bg-gray-400 rounded-full shadow-sm" />
        </div>

    </motion.div>
  );
};


