/**
 * PrototypeDashboard.tsx - 原型控制台
 * 
 * Sidebar navigation for prototype pages.
 * Style matches prism-web-v1 Dashboard.
 */

import React from 'react';
import { Settings, Home, History } from 'lucide-react';

interface DashboardProps {
  onNavigate: (destination: string) => void;
  currentView: string;
}

export const PrototypeDashboard: React.FC<DashboardProps> = ({ onNavigate, currentView }) => {
  return (
    <div className="fixed top-0 left-0 bottom-0 w-64 bg-gray-900 border-r border-white/10 p-6 z-[9999] flex flex-col shadow-2xl">
      <div className="mb-8">
        <h2 className="text-lg font-bold text-white flex items-center gap-2">
          <Settings size={18} className="text-gray-400" />
          原型控制台
        </h2>
        <span className="text-[10px] uppercase tracking-wider text-gray-500 font-mono">导航与状态</span>
      </div>

      <div className="flex-1 space-y-3">
        <MenuButton 
          icon={<Home size={18} />} 
          label="首页" 
          active={currentView === 'home'}
          onClick={() => onNavigate('home')} 
        />
        <div className="h-px bg-white/10 my-2" />
        <MenuButton 
          icon={<History size={18} />} 
          label="历史会话" 
          color="text-cyan-400"
          active={currentView === 'history'}
          onClick={() => onNavigate('history')} 
        />
        <MenuButton 
          icon={<History size={18} />} 
          label="历史会话 2" 
          color="text-purple-400"
          active={currentView === 'history_glass'}
          onClick={() => onNavigate('history_glass')} 
        />
        <MenuButton 
          icon={<History size={18} />} 
          label="历史会话 3" 
          color="text-amber-400"
          active={currentView === 'history_cards'}
          onClick={() => onNavigate('history_cards')} 
        />
        <MenuButton 
          icon={<History size={18} />} 
          label="历史会话 4" 
          color="text-emerald-400"
          active={currentView === 'history_hybrid'}
          onClick={() => onNavigate('history_hybrid')} 
        />
        <div className="h-px bg-white/10 my-2" />
        <MenuButton 
          icon={<Settings size={18} />} // Using Settings as placeholder icon if Calendar not imported
          label="日程表 (Scheduler)" 
          color="text-pink-400"
          active={currentView === 'scheduler'}
          onClick={() => onNavigate('scheduler')} 
        />
      </div>

      <div className="pt-6 border-t border-white/10 text-xs text-gray-600 font-mono">
        Prism v1.0 • Pro Max
      </div>
    </div>
  );
};

const MenuButton: React.FC<{ 
  icon: React.ReactNode; 
  label: string; 
  onClick: () => void;
  color?: string;
  active?: boolean;
}> = ({ icon, label, onClick, color = "text-gray-400", active = false }) => (
  <button
    onClick={onClick}
    className={`w-full flex items-center gap-3 p-3 rounded-lg border transition-all text-left group ${
      active 
        ? 'bg-white/10 border-white/20' 
        : 'bg-white/5 border-transparent hover:border-white/10 hover:bg-white/10'
    }`}
  >
    <div className={`${active ? 'text-white' : color} group-hover:scale-110 transition-transform opacity-80 group-hover:opacity-100`}>
      {icon}
    </div>
    <span className={`text-sm font-medium ${active ? 'text-white' : 'text-gray-400 group-hover:text-white'}`}>
      {label}
    </span>
  </button>
);
