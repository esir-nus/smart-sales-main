import React, { useState, useRef, useEffect } from 'react';
import { MobileFrame } from './components/MobileFrame';
import { AuroraBackground } from './components/AuroraBackground';
import { Header } from './components/Header';
import { KnotFAB } from './components/KnotFAB';
import { InputBar } from './components/InputBar';
import { SchedulerDrawer } from './components/SchedulerDrawer';
import { PrototypeDashboard } from './components/PrototypeDashboard';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle2, Circle } from 'lucide-react';
import { clsx } from 'clsx';

import { HomeScreen } from './components/HomeScreen';
import { HistoryDrawer } from './components/HistoryDrawer';
import { AudioDrawer } from './components/AudioDrawer';
import { RightToolbar } from './components/RightToolbar';
import { ModeToggle } from './components/ModeToggle';
import { OnboardingOverlay } from './components/OnboardingOverlay';
import { ConnectivityModal } from './components/ConnectivityModal';
import { UserCenter } from './components/UserCenter';

// Types
type Mode = 'coach' | 'analyst' | 'home';
type Message = {
    id: string;
    role: 'user' | 'assistant';
    content: React.ReactNode;
    isTyping?: boolean;
};

// Plan Card Component (Internal for clarity)
const PlanCard = () => (
    <motion.div 
        initial={{ opacity: 0, y: -20, height: 0 }}
        animate={{ opacity: 1, y: 0, height: 'auto' }}
        className="w-full glass-card p-4 mb-4 border-l-4 border-l-prism-accent overflow-hidden"
    >
        <div className="flex justify-between items-center mb-3">
             <h3 className="font-semibold text-sm text-gray-900 flex items-center gap-2">
                <span className="text-xl">📋</span> 分析计划
             </h3>
             <span className="text-xs font-mono text-gray-500">运行中</span>
        </div>
        <div className="space-y-2">
            {[
                { label: '提取音频关键点', status: 'done' },
                { label: 'OCR 图表分析 (5张)', status: 'running' },
                { label: '综合决策模型', status: 'pending' },
            ].map((step, i) => (
                <div key={i} className="flex items-center gap-3 text-sm">
                    {step.status === 'done' ? (
                        <CheckCircle2 size={16} className="text-green-500" />
                    ) : step.status === 'running' ? (
                         <div className="w-4 h-4 border-2 border-prism-accent border-t-transparent rounded-full animate-spin" />
                    ) : (
                        <Circle size={16} className="text-gray-300" />
                    )}
                    <span className={clsx(
                        step.status === 'done' && "text-gray-400 line-through",
                        step.status === 'running' && "text-gray-900 font-medium",
                        step.status === 'pending' && "text-gray-500"
                    )}>{step.label}</span>
                </div>
            ))}
        </div>
    </motion.div>
);



function App() {
  const [mode, setMode] = useState<Mode>('home'); // View Mode
  const [inputIntent, setInputIntent] = useState<'coach' | 'analyst'>('coach'); // Toggle State
  const [isOnboarding, setIsOnboarding] = useState(false); // New State
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false); // History Drawer State
  const [isAudioOpen, setIsAudioOpen] = useState(false); // Audio Drawer State
  const [isConnectivityOpen, setIsConnectivityOpen] = useState(false); // Connectivity Modal State
  const [isUserCenterOpen, setIsUserCenterOpen] = useState(false); // User Center State
  
  // Auto-expand Scheduler on first launch (Clean Desk Paradigm)
  useEffect(() => {
    // Small delay to allow initial render to settle
    const timber = setTimeout(() => {
        setIsDrawerOpen(true);
    }, 500);
    return () => clearTimeout(timber);
  }, []);
  const [messages, setMessages] = useState<Message[]>([
    { id: '1', role: 'assistant', content: '下午好，Frank。准备好回顾 Q4 了吗？' }
  ]);
  const [showPlan, setShowPlan] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Dashboard Handler
  const handleDashboardNav = (destination: string) => {
      // 1. Universal Cleanup: Always close drawer & overlays unless specific mode requests them
      if (destination !== 'Scheduler') {
          setIsDrawerOpen(false);
      }

      // 2. State Specific Logic
      if (destination === 'Home') {
          setIsOnboarding(false);
          setMode('home');
          setShowPlan(false);
          // Don't reset messages if just viewing home, but if starting fresh...
          // For prototype, keep messages as context unless explicit reset
      }
      if (destination === 'Onboarding') {
          setIsOnboarding(true);
      }
      if (destination === 'Analyst') {
          setIsOnboarding(false);
          // Force reset messages if switching from another deep state? No, keep context or reset?
          // Brief implies "Analyst Mode" should trigger the plan scenario.
          if (mode !== 'analyst') {
             runScenario('analyst');
          }
      }
      if (destination === 'Scheduler') {
          // Scheduler can overlay on top of anything, but design brief says "Clean State"
          // So we might want to hide onboarding if it's open?
          setIsOnboarding(false);
          setIsDrawerOpen(true);
      }
      if (destination === 'Audio') {
          setIsOnboarding(false);
          setIsAudioOpen(true);
      }
      if (destination === 'UserCenter') {
          setIsOnboarding(false);
          setIsUserCenterOpen(true);
      }
  };

  // Mock Scenario Trigger
  const runScenario = (targetMode?: 'coach' | 'analyst' | 'schedule') => {
      const target = targetMode || inputIntent;
      
      if (target === 'analyst') {
          // Analyst Flow
          setMessages(prev => [...prev, { id: Date.now().toString(), role: 'user', content: '对 A3 项目进行深度分析。' }]);
          setTimeout(() => {
              setMode('analyst');
              setShowPlan(true); 
              setMessages(prev => [...prev, { id: 'sys', role: 'assistant', content: '正在构建分析上下文...', isTyping: true }]);
          }, 600);
      } else if (target === 'coach') {
          // Coach Flow
          setMessages(prev => [...prev, { id: Date.now().toString(), role: 'user', content: '帮我回顾一下今天的重点。' }]);
          setMode('coach');
      } else if (target === 'schedule') {
          setIsDrawerOpen(true);
      }
  };

  return (
    <div className="min-h-screen bg-[#1a1a1a] flex items-center justify-center pl-64 transition-all duration-300">
        
        {/* Persistent Dashboard Sidebar */}
        <PrototypeDashboard onNavigate={handleDashboardNav} />

        {/* Mobile Frame (Centered in remaining space) */}

        <MobileFrame>
          <AuroraBackground />
          <Header 
            title={mode === 'home' ? "Session: CEO Wang..." : "智能销售"} 
            showHomeActions={mode === 'home'}
            onReset={() => setMessages([])}
            onMenuClick={() => setIsHistoryOpen(true)}
            onSignalClick={() => setIsConnectivityOpen(true)}
          />
          
          {/* Connectivity Modal */}
          <ConnectivityModal 
            isOpen={isConnectivityOpen} 
            onClose={() => setIsConnectivityOpen(false)} 
          />
          
          {/* User Center (Z-Index High) */}
          <UserCenter 
             isOpen={isUserCenterOpen} 
             onClose={() => setIsUserCenterOpen(false)} 
          />
          
          {/* Onboarding Overlay */}
          <AnimatePresence>
            {isOnboarding && <OnboardingOverlay onComplete={() => handleDashboardNav('Home')} />}
          </AnimatePresence>

          {/* History Drawer (Left) */}
          <AnimatePresence>
            {isHistoryOpen && (
                <HistoryDrawer 
                    isOpen={isHistoryOpen} 
                    onClose={() => setIsHistoryOpen(false)} 
                    onSelectSession={(id) => {
                        console.log('Selected session:', id);
                        setIsHistoryOpen(false);
                    }}
                    onSettingsClick={() => {
                        setIsHistoryOpen(false); // Close history
                        setIsUserCenterOpen(true); // Open settings
                    }}
                />
            )}
          </AnimatePresence>

          {/* Audio Drawer (Bottom) */}
          <AudioDrawer 
            isOpen={isAudioOpen} 
            onClose={() => setIsAudioOpen(false)} 
          />

          {/* Scheduler Drawer (Overlay) */}
          <SchedulerDrawer isOpen={isDrawerOpen} onClose={() => setIsDrawerOpen(false)} />

          {/* Main Chat Area */}
          <div className="flex-1 overflow-hidden flex flex-col z-10 relative">
             <AnimatePresence mode='wait'>
                {mode === 'home' ? (
                    <motion.div 
                        key="home"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="flex-1 h-full"
                    >
                        <HomeScreen onNavigate={handleDashboardNav} />
                    </motion.div>
                ) : (
                    <motion.div 
                        key="chat" 
                        initial={{ opacity: 0 }} 
                        animate={{ opacity: 1 }} 
                        exit={{ opacity: 0 }}
                        className="flex-1 overflow-y-auto p-4 pb-32 space-y-4 no-scrollbar" 
                        ref={scrollRef}
                    >
                        {/* Analyst Plan Card (Persists at top) */}
                        <AnimatePresence>
                            {showPlan && <PlanCard />}
                        </AnimatePresence>

                        {messages.map((msg) => (
                            <motion.div 
                                key={msg.id}
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                                className={clsx(
                                    "max-w-[85%] rounded-2xl px-4 py-3 text-[15px] leading-relaxed shadow-sm",
                                    msg.role === 'user' 
                                        ? "bg-prism-accent text-white self-end ml-auto rounded-tr-sm" 
                                        : "glass-card text-gray-800 self-start mr-auto rounded-tl-sm"
                                )}
                            >
                                {msg.content}
                            </motion.div>
                        ))}
                    </motion.div>
                )}
             </AnimatePresence>
          </div>

          {/* Controls */}
          {mode === 'home' && <RightToolbar onAudioClick={() => setIsAudioOpen(true)} />}
          <KnotFAB isThinking={mode === 'analyst'} onClick={() => setIsDrawerOpen(!isDrawerOpen)} />
          
          {/* Input - Hackily mocking scenarios via click for prototype */}
          <div className="absolute bottom-8 left-4 right-4 z-30">
               {/* Always show InputBar in Home mode for V2, or if Chatting */}
               <div onClick={() => {
                   if (mode === 'home') runScenario();
               }}>
                   <InputBar 
                       topAccessory={mode === 'home' ? (
                           <div onClick={(e) => e.stopPropagation()}>
                               <ModeToggle 
                                   mode={inputIntent}
                                   onModeChange={setInputIntent}
                               />
                           </div>
                       ) : null}
                   /> 
               </div>
               
               {/* Instruction Overlay */}
               {mode === 'home' && (
                   <div className="absolute -top-24 left-0 right-0 text-center text-xs text-white/50 font-mono pointer-events-none">
                        [Prototype] Click Input to enter {inputIntent === 'coach' ? 'Coach' : 'Analyst'} Mode
                   </div>
               )}
          </div>

        </MobileFrame>
    </div>
  );
}

export default App;
