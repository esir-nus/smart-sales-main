import React from 'react';
import { Settings, Home, Rocket, BarChart2, Calendar } from 'lucide-react';

interface DashboardProps {
  onNavigate: (destination: string) => void;
}

interface DashboardProps {
  onNavigate: (destination: string) => void;
}

export const PrototypeDashboard: React.FC<DashboardProps> = ({ onNavigate }) => {
  // Persistent Sidebar - No Toggle State needed
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
          label="重置首页" 
          onClick={() => onNavigate('Home')} 
        />
        <MenuButton 
          icon={<Rocket size={18} />} 
          label="引导页" 
          color="text-purple-400"
          onClick={() => onNavigate('Onboarding')} 
        />
        <MenuButton 
          icon={<BarChart2 size={18} />} 
          label="分析模式" 
          color="text-blue-400"
          onClick={() => onNavigate('Analyst')} 
        />
        <MenuButton 
          icon={<Calendar size={18} />} 
          label="日程安排" 
          color="text-green-400"
          onClick={() => onNavigate('Scheduler')} 
        />
      </div>

      <div className="pt-6 border-t border-white/10 text-xs text-gray-600 font-mono">
        Prism v1.0 • Dev Build
      </div>
    </div>
  );
};

const MenuButton: React.FC<{ 
  icon: React.ReactNode; 
  label: string; 
  onClick: () => void;
  color?: string;
}> = ({ icon, label, onClick, color = "text-gray-400" }) => (
  <button
    onClick={onClick}
    className="w-full flex items-center gap-3 p-3 rounded-lg bg-white/5 border border-transparent hover:border-white/10 hover:bg-white/10 transition-all text-left group"
  >
    <div className={`${color} group-hover:scale-110 transition-transform`}>{icon}</div>
    <span className="text-sm font-medium text-gray-300 group-hover:text-white">{label}</span>
  </button>
);
