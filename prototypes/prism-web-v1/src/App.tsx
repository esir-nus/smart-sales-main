import React, { useState, useRef } from 'react';
import { MobileFrame } from './components/MobileFrame';
import { AuroraBackground } from './components/AuroraBackground';
import { Header } from './components/Header';
import { KnotFAB } from './components/KnotFAB';
import { InputBar } from './components/InputBar';
import { SchedulerDrawer } from './components/SchedulerDrawer';
import { PrototypeDashboard } from './components/PrototypeDashboard';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle2, Circle, Rocket } from 'lucide-react';
import { clsx } from 'clsx';

// Types
type Mode = 'coach' | 'analyst';
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
                <span className="text-xl">📋</span> Analysis Plan
             </h3>
             <span className="text-xs font-mono text-gray-500">RUNNING</span>
        </div>
        <div className="space-y-2">
            {[
                { label: 'Extract audio key points', status: 'done' },
                { label: 'OCR Analysis (5 Images)', status: 'running' },
                { label: 'Synthesis Decision Model', status: 'pending' },
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

// Onboarding Component (Mock)
const OnboardingScreen = () => (
    <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0 z-50 bg-white flex flex-col items-center justify-center p-8 text-center"
    >
        <div className="w-24 h-24 rounded-full bg-blue-50 mb-8 flex items-center justify-center animate-pulse">
             <Rocket size={48} className="text-blue-500" />
        </div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Welcome to SmartSales</h1>
        <p className="text-gray-500 mb-8">Setting up your AI sales assistant...</p>
        <div className="w-full max-w-xs h-1 disabled bg-gray-100 rounded-full overflow-hidden">
            <div className="h-full bg-blue-500 animate-[width_2s_ease-out_forwards] w-3/4" />
        </div>
    </motion.div>
);

function App() {
  const [mode, setMode] = useState<Mode>('coach');
  const [isOnboarding, setIsOnboarding] = useState(false); // New State
  const [messages, setMessages] = useState<Message[]>([
    { id: '1', role: 'assistant', content: 'Good afternoon, Frank. Ready to prep for the Q4 review?' }
  ]);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [showPlan, setShowPlan] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Dashboard Handler
  const handleDashboardNav = (destination: string) => {
      if (destination === 'Home') {
          setIsOnboarding(false);
          setMode('coach');
          setShowPlan(false);
          setMessages([{ id: '1', role: 'assistant', content: 'Good afternoon, Frank. Ready to prep for the Q4 review?' }]);
      }
      if (destination === 'Onboarding') {
          setIsOnboarding(true);
      }
      if (destination === 'Analyst') {
          setIsOnboarding(false);
          runScenario('plan');
      }
      if (destination === 'Scheduler') {
          setIsOnboarding(false);
          setIsDrawerOpen(true);
      }
  };

  // Mock Scenario Trigger
  const runScenario = (type: 'plan' | 'schedule') => {
      if (type === 'plan') {
          setMessages(prev => [...prev, { id: Date.now().toString(), role: 'user', content: 'Run deep analysis on the A3 project.' }]);
          setTimeout(() => {
              setMode('analyst');
              setShowPlan(true); 
              setMessages(prev => [...prev, { id: 'sys', role: 'assistant', content: 'Constructing analysis context...', isTyping: true }]);
          }, 600);
      }
      if (type === 'schedule') {
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
          <Header />
          
          {/* Onboarding Overlay */}
          <AnimatePresence>
            {isOnboarding && <OnboardingScreen />}
          </AnimatePresence>

          {/* Scheduler Drawer (Overlay) */}
          <SchedulerDrawer isOpen={isDrawerOpen} onClose={() => setIsDrawerOpen(false)} />

          {/* Main Chat Area */}
          <div className="flex-1 overflow-y-auto p-4 pb-32 space-y-4 z-10 no-scrollbar" ref={scrollRef}>
            
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
          </div>

          {/* Controls */}
          <KnotFAB isThinking={mode === 'analyst'} onClick={() => setIsDrawerOpen(!isDrawerOpen)} />
          
          {/* Input - Hackily mocking scenarios via click for prototype */}
          <div className="absolute bottom-8 left-4 right-4 z-30" onClick={() => runScenario(mode === 'coach' ? 'plan' : 'schedule')}>
             <InputBar /> 
             {/* Instruction Overlay */}
             <div className="absolute -top-12 left-0 right-0 text-center text-xs text-gray-400 font-mono pointer-events-none">
                Tap bar to simulate: {mode === 'coach' ? 'ANALYST REQUEST' : 'OPEN SCHEDULER'}
             </div>
          </div>

        </MobileFrame>
    </div>
  );
}

export default App;
