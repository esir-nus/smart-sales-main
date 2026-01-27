import React from 'react';
import { motion, type PanInfo, AnimatePresence } from 'framer-motion';
import { BellRing, Trash2, Sparkles, X } from 'lucide-react';
import { clsx } from 'clsx';
import { InspirationCard } from './InspirationCard';
import { ConflictCard } from './ConflictCard';

interface SchedulerDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

import { ChatExpandedView } from './ChatExpandedView';

const ExpandedTaskCard = ({ item }: { item: any }) => {
    const [expanded, setExpanded] = React.useState(false);
    
    // Mock Alarm Logic
    const hasAlarm = React.useMemo(() => Math.random() > 0.5, []);

    return (
        <motion.div 
            onClick={() => !expanded && setExpanded(true)}
            animate={{ height: expanded ? 'auto' : 'auto' }}
            className="p-4 bg-white rounded-2xl border border-gray-100 shadow-sm transition-all overflow-hidden"
        >
            <div className="flex justify-between items-center mb-2" onClick={() => expanded && setExpanded(false)}>
                <h3 className="font-semibold text-gray-900">{item.title}</h3>
                <div className="flex items-center gap-2">
                    {/* Alarm State Visual */}
                    {hasAlarm && (
                        <div className="flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-blue-50 text-blue-600 border border-blue-100/50">
                             <BellRing size={10} className="animate-pulse" />
                             <span>08:50</span>
                        </div>
                    )}
                </div>
            </div>
            
            {/* Expanded Content (Contextual Agent Input) */}
            <AnimatePresence>
                {expanded && (
                    <motion.div 
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="pt-2 mt-2 border-t border-gray-50"
                    >
                        <ChatExpandedView 
                            initialMessage={`已为您安排 ${item.time}。地点：北京办公室。发现 3 份相关历史文档。需要摘要吗？`}
                            onSend={(text) => console.log('Task update:', text)}
                        />
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
};

export const SchedulerDrawer: React.FC<SchedulerDrawerProps> = ({ isOpen, onClose }) => { 
  const [isExpanded, setIsExpanded] = React.useState(false);
  
  const [currentWeekStart, setCurrentWeekStart] = React.useState(() => {
    const d = new Date();
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust when day is Sunday
    return new Date(d.setDate(diff));
  });

  const [selectedDate, setSelectedDate] = React.useState(new Date().getDate());

  // Task List State (for Swipe-to-Delete)
  const [schedulerItems, setSchedulerItems] = React.useState([
      { id: '1', type: 'task', time: '09:00', title: '审查 Q4 预算', duration: '45 分钟', tag: null }, 
      { id: '2', type: 'conflict', time: '12:00', title: '冲突：审查预算 vs 团队午餐' }, 
      { id: '3', type: 'inspiration', time: '13:00', title: '灵感：竞品定价策略' }, 
      { id: '4', type: 'task', time: '14:30', title: '客户拜访 (张总)', duration: '1 小时', tag: null }
  ]);

  const handleDeleteItem = (id: string) => {
      setSchedulerItems(prev => prev.filter(item => item.id !== id));
      console.log(`Deleted item ${id}`);
  };

  // ---------------------------------------------------------------------------
  // Multi-Select Logic (Lifted State)
  // ---------------------------------------------------------------------------
  const [selectionMode, setSelectionMode] = React.useState(false);
  const [selectedIds, setSelectedIds] = React.useState<Set<string>>(new Set());

  // Enter Mode (triggered by Sparkles on any card)
  const handleEnterSelectionMode = (initialId: string) => {
    setSelectionMode(true);
    setSelectedIds(new Set([initialId]));
  };

  // Toggle Item Selection
  const handleToggleSelection = (id: string) => {
    const newSet = new Set(selectedIds);
    if (newSet.has(id)) {
      newSet.delete(id);
      // If last item deselected, potentially exit mode? For now, keep mode open.
    } else {
      newSet.add(id);
    }
    setSelectedIds(newSet);
  };

  // Exit Mode
  const exitSelectionMode = () => {
    setSelectionMode(false);
    setSelectedIds(new Set());
  };

  // Mock Analysis
  const handleBatchAnalyze = () => {
    console.log(`Analyzing ${selectedIds.size} items:`, Array.from(selectedIds));
    const count = selectedIds.size;
    exitSelectionMode();
    // Simulate Toast
    alert(`💡 正在分析 ${count} 个灵感点...`); 
  };

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


  // Generate Month Grid (Simple 5 weeks)
  const monthGrid = React.useMemo(() => {
    // Always anchor to the Month of the selected currentWeekStart
    // But we need to ensure the grid covers the view
    const year = currentWeekStart.getFullYear();
    const month = currentWeekStart.getMonth();
    const start = new Date(year, month, 1);
    
    // Calculate start offset (Monday = 0)
    // getDay(): Sun=0, Mon=1...Sat=6
    // We want Mon=0...Sun=6. 
    // If Sun(0), offset is 6. If Mon(1), offset is 0.
    const day = start.getDay();
    const offset = day === 0 ? 6 : day - 1; 

    start.setDate(start.getDate() - offset);
    
    return Array.from({ length: 35 }, (_, i) => {
      const d = new Date(start);
      d.setDate(d.getDate() + i);
      return d;
    });
  }, [currentWeekStart]);

  // Calculate Y Offset for Calendar
  const calendarYOffset = React.useMemo(() => {
      if (isExpanded) return 0;
      
      const index = monthGrid.findIndex(d => 
          d.getDate() === selectedDate && 
          d.getMonth() === currentWeekStart.getMonth()
      );
      
      const safeIndex = index === -1 ? 0 : index;
      return -(Math.floor(safeIndex / 7) * 56);
  }, [isExpanded, monthGrid, selectedDate, currentWeekStart]);

  // Handle Swipe (Left/Right) to change week/month
  const handleCalendarDragEnd = (_: any, info: PanInfo) => {
      const SWIPE_THRESHOLD = 50;
      if (Math.abs(info.offset.x) > SWIPE_THRESHOLD) {
          const direction = info.offset.x > 0 ? -1 : 1; // Drag Right -> Prev, Drag Left -> Next
          
          if (!isExpanded) {
             // Week Swipe
             const newDate = new Date(currentWeekStart);
             newDate.setDate(newDate.getDate() + (direction * 7));
             setCurrentWeekStart(newDate);
             // Note: We keep the selectedDate (int) as is, effectively selecting the same weekday in the new week.
          }
      }
  };

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
            <div className="flex justify-between items-center mb-2">
                 <h2 className="text-xl font-bold flex items-center gap-2 tracking-tight">
                    <span className="text-gray-400 font-medium tracking-normal text-sm">{currentWeekStart.getFullYear()}年</span>
                    <span className="text-gray-900 text-2xl">{currentWeekStart.getMonth() + 1}月</span>
                </h2>
            </div>
            
             <div className="overflow-x-auto flex gap-3 no-scrollbar pb-2 mask-linear-fade snap-x snap-mandatory scroll-pl-6">
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
                            "font-medium text-[13px] px-5 py-2.5 rounded-full whitespace-nowrap transition-all duration-300 snap-center border",
                            isActive 
                                ? "bg-black text-white border-black shadow-md scale-105" 
                                : "bg-white text-gray-500 border-gray-100 hover:bg-gray-50"
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
                
                {/* Animated Dates Container - Single Source of Truth */}
                <motion.div
                    animate={{ 
                        height: isExpanded ? 300 : 56, // 5 rows * 56px = 280px + buffer
                    }} 
                    transition={{ type: "spring", stiffness: 300, damping: 30 }}
                    className="overflow-hidden w-full relative col-span-7"
                >
                     {/* The Full Month Grid - Always Rendered */}
                     <motion.div
                        animate={{
                            y: calendarYOffset
                        }}
                        transition={{ type: "spring", stiffness: 300, damping: 30 }}
                        drag={isExpanded ? false : "x"} // Enable drag only when folded
                        dragConstraints={{ left: 0, right: 0 }}
                        dragElastic={0.2}
                        onDragEnd={handleCalendarDragEnd}
                        className="touch-pan-y" // Allow vertical scrolling still?
                     >
                        <div className="grid grid-cols-7 gap-y-2 place-items-center w-full">
                            {monthGrid.map((d) => {
                                const isCurrentMonth = d.getMonth() === currentWeekStart.getMonth();
                                const isSelectedDay = d.getDate() === selectedDate && isCurrentMonth; 
                                const hasTask = d.getDate() % 3 === 0; 
                                
                                return (
                                    <motion.div 
                                        // Removed layout prop to prevent overlap regression during expansion
                                        key={d.toISOString()} 
                                        onClick={() => {
                                            // Update selected date
                                            setSelectedDate(d.getDate());
                                            // Also update month context if clicking a gray date
                                            if (!isCurrentMonth) {
                                                const newDate = new Date(d);
                                                setCurrentWeekStart(newDate);
                                            }
                                        }}
                                        className={clsx(
                                            "flex flex-col items-center justify-center w-full h-12 rounded-2xl transition-all cursor-pointer relative group",
                                            !isSelectedDay && "hover:bg-black/5"
                                        )}
                                    >
                                        <div className={clsx(
                                            "w-9 h-9 flex items-center justify-center rounded-full transition-all duration-300 relative z-10 font-mono",
                                            isSelectedDay ? "bg-blue-600 text-white shadow-lg scale-110" : "text-gray-700", // blue-600 token
                                            !isCurrentMonth && !isSelectedDay && "text-gray-300"
                                        )}>
                                            <span className={clsx("text-sm", isSelectedDay ? "font-bold" : "font-medium")}>
                                                {d.getDate()}
                                            </span>
                                        </div>

                                        {/* Task Indicator */}
                                        {hasTask && (
                                            <div className={clsx(
                                                "absolute bottom-1 w-1 h-1 rounded-full transition-all left-1/2 -translate-x-1/2", 
                                                isSelectedDay ? "bg-white/80" : "bg-blue-400 group-hover:scale-125"
                                            )} />
                                        )}
                                    </motion.div>
                                );
                            })}
                        </div>
                     </motion.div>
                </motion.div>
             </div>
             
             {/* Calendar Expansion Handle (Bottom Pill) */}
             <div 
                className="w-full flex justify-center py-4 mt-auto cursor-pointer active:scale-95 hover:bg-black/5 transition-all z-20"
                onClick={(e) => {
                    e.stopPropagation(); // Prevent parent drag handlers
                    setIsExpanded(!isExpanded);
                }}
             >
                 <div className="w-8 h-1 bg-gray-300 rounded-full" />
             </div>
        </motion.div>

        {/* Content Area */}
        {/* Content Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-6 pb-24 no-scrollbar">
            <AnimatePresence initial={false} mode='popLayout'>
            {schedulerItems.map((item) => (
                <motion.div 
                    layout
                    key={item.id} 
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0, marginBottom: 0 }}
                    transition={{ duration: 0.2 }}
                    className="flex gap-4 relative group items-start"
                >
                     {/* Time Label - Refined Typography */}
                     <span className="text-xs text-gray-400 font-mono pt-3 w-10 text-right shrink-0 tracking-wide">{item.time}</span>
                     
                     {/* Swipe Container */}
                     <div className="flex-1 relative">
                        {/* Timeline Connector */}
                        <div className="absolute left-0 top-3 bottom-0 w-0.5 bg-gray-100 rounded-full" /> 
                        
                        {/* Swipe Wrapper (Delete Background) */}
                        <div className="relative ml-3 bg-red-500 rounded-[20px] overflow-hidden">
                            {/* Delete Icon (Behind) */}
                            <div className="absolute right-4 top-1/2 -translate-y-1/2 text-white flex items-center gap-1 font-medium z-0">
                                <Trash2 size={16} />
                                <span className="text-xs">删除</span>
                            </div>

                            {/* Foreground Card */}
                            <motion.div 
                                drag="x"
                                dragConstraints={{ left: 0, right: 0 }} // Snap back if released early
                                dragElastic={{ left: 0.5, right: 0 }} // Only allow left drag
                                onDragEnd={(_, info) => {
                                    if (info.offset.x < -100) {
                                        handleDeleteItem(item.id);
                                    }
                                }}
                                whileDrag={{ scale: 1.02, left: -20 }} // Subtle feedback
                                className="relative bg-white rounded-[20px] z-10 shadow-sm"
                                style={{ touchAction: "none" }}
                            >
                                {item.type === 'conflict' ? (
                                    <ConflictCard item={item} />
                                ) : item.type === 'inspiration' ? (
                                    <InspirationCard 
                                        item={item} 
                                        selectionMode={selectionMode}
                                        isSelected={selectedIds.has(item.id)}
                                        onEnterMode={() => handleEnterSelectionMode(item.id)}
                                        onToggle={() => handleToggleSelection(item.id)}
                                    />
                                ) : (
                                    // Standard Task Card with Expand Logic
                                    <ExpandedTaskCard item={item} />
                                )}
                            </motion.div>
                        </div>
                     </div>
                </motion.div>
            ))}
            </AnimatePresence>
        </div>

        {/* Drawer Global Handle (Absolute Bottom) */}
        <div className="absolute bottom-3 left-0 right-0 flex justify-center cursor-grab active:cursor-grabbing hover:bg-black/5 py-2 transition-colors">
             <div className="w-16 h-1.5 bg-gray-400 rounded-full shadow-sm" />
        </div>

        {/* Batch Action Bar (Floating) */}
        <AnimatePresence>
            {selectionMode && (
                <motion.div
                    initial={{ y: 100, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    exit={{ y: 100, opacity: 0 }}
                    className="absolute bottom-6 left-6 right-6 z-50 flex items-center justify-between bg-gray-900/90 backdrop-blur-md text-white p-4 rounded-full shadow-2xl"
                >
                    <div className="flex items-center gap-3 pl-2">
                        <button onClick={exitSelectionMode} className="p-1 rounded-full hover:bg-white/10 transition-colors">
                            <X size={20} className="text-gray-400" />
                        </button>
                        <span className="font-medium">{selectedIds.size} 个已选择</span>
                    </div>

                    <button 
                        onClick={handleBatchAnalyze}
                        disabled={selectedIds.size === 0}
                        className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-5 py-2.5 rounded-full font-medium transition-all disabled:opacity-50 disabled:grayscale"
                    >
                        <Sparkles size={16} fill="currentColor" />
                        <span>开始分析</span>
                    </button>
                </motion.div>
            )}
        </AnimatePresence>

    </motion.div>
  );
};


