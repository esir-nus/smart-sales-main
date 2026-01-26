import React from 'react';
import { Menu, Smartphone } from 'lucide-react';

interface HeaderProps {
    title?: string;
}

export const Header: React.FC<HeaderProps> = ({ title = "智能销售" }) => {
  return (
    <header className="flex items-center justify-between h-auto min-h-[56px] px-4 pt-12 pb-2 z-20 relative">
        <div className="flex items-center gap-4">
            <button className="p-2 -ml-2 text-gray-900">
                <Menu size={24} />
            </button>
            <button className="p-2 text-gray-400">
                <Smartphone size={24} />
            </button>
        </div>
        
        <div className="absolute left-1/2 -translate-x-1/2 text-center">
            <h1 className="t-title">{title}</h1>
        </div>
        
        <div className="w-10" /> {/* Spacer for balance */}
    </header>
  );
};
