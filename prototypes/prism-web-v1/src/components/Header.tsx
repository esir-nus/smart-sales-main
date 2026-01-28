import React from 'react';
import { Menu, Signal, Bug, Plus } from 'lucide-react';

interface HeaderProps {
    title?: string;
    showHomeActions?: boolean;
    onReset?: () => void;
    onMenuClick?: () => void;
    onSignalClick?: () => void;
}

export const Header: React.FC<HeaderProps> = ({ title = "智能销售", showHomeActions = false, onReset, onMenuClick, onSignalClick }) => {
  return (
    <header className="flex items-center justify-between h-auto min-h-[56px] px-4 pt-12 pb-2 z-20 relative">
        {/* Left: History & Device */}
        <div className="flex items-center gap-3">
            <button className="p-2 -ml-2 text-gray-900" onClick={onMenuClick}>
                <Menu size={24} />
            </button>
            {showHomeActions && (
                <button 
                    onClick={onSignalClick}
                    className="p-2 text-green-600 bg-green-50 rounded-full hover:bg-green-100 transition-colors"
                >
                    <Signal size={18} />
                </button>
            )}
        </div>
        
        {/* Center: Title */}
        <div className="absolute left-1/2 -translate-x-1/2 text-center">
            <h1 className="t-title text-base font-semibold text-gray-900">{title}</h1>
        </div>
        
        {/* Right: Debug & New Session */}
        <div className="flex items-center gap-2">
            {showHomeActions ? (
                <>
                    <button className="p-2 text-gray-400 hover:text-gray-900">
                        <Bug size={20} />
                    </button>
                    <button 
                        onClick={onReset}
                        className="p-2 text-gray-900 bg-gray-100 rounded-full hover:bg-gray-200"
                    >
                        <Plus size={20} />
                    </button>
                </>
            ) : (
                <div className="w-10" /> 
            )}
        </div>
    </header>
  );
};
