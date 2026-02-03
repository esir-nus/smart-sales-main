import React from 'react';
import { Menu, Plus, BarChart2, Bug } from 'lucide-react';

interface HeaderProps {
  onMenuClick?: () => void;
}

export const Header = ({ onMenuClick }: HeaderProps) => {
  return (
    <header className="absolute top-0 left-0 right-0 z-50 px-4 pt-14 pb-2 flex items-center justify-between">
      {/* Left Group */}
      <div className="flex items-center gap-3">
        {/* Menu */}
        <button 
          onClick={onMenuClick}
          className="w-10 h-10 bg-prism-surface backdrop-blur-lg rounded-full shadow-sm border border-prism-border/50 flex items-center justify-center active:scale-95 transition-transform"
        >
          <Menu size={20} className="text-prism-primary" />
        </button>
        {/* Analytics (Data) */}
        <button className="w-10 h-10 bg-prism-surface backdrop-blur-lg rounded-full shadow-sm border border-prism-border/50 flex items-center justify-center active:scale-95 transition-transform">
          <BarChart2 size={20} className="text-amber-500 fill-amber-500 opacity-90" />
        </button>
      </div>

      {/* Center Title */}
      <div className="absolute left-1/2 -translate-x-1/2 top-16">
        <h1 className="text-lg font-medium text-prism-primary tracking-wide">
          新对话
        </h1>
      </div>

      {/* Right Group */}
      <div className="flex items-center gap-3">
        {/* Bug / Debug */}
        <button className="w-10 h-10 bg-prism-surface backdrop-blur-lg rounded-full shadow-sm border border-prism-border/50 flex items-center justify-center active:scale-95 transition-transform">
          <Bug size={20} className="text-prism-secondary" />
        </button>
        {/* Add New */}
        <button className="w-10 h-10 bg-prism-surface backdrop-blur-lg rounded-full shadow-sm border border-prism-border/50 flex items-center justify-center active:scale-95 transition-transform">
          <Plus size={22} className="text-prism-primary" />
        </button>
      </div>
    </header>
  );
};
