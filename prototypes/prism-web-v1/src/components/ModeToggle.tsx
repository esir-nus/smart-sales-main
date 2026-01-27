import React from 'react';
import { MessageSquare, Microscope } from 'lucide-react';
import { motion } from 'framer-motion';
import { clsx } from 'clsx';

interface ModeToggleProps {
  mode: 'coach' | 'analyst';
  onModeChange: (mode: 'coach' | 'analyst') => void;
}

export const ModeToggle: React.FC<ModeToggleProps> = ({ mode, onModeChange }) => {
  return (
    <div className="bg-white/90 backdrop-blur-md rounded-full p-1 shadow-sm border border-white/50 inline-flex relative">
      {/* Active Pill Background Animation */}
      <motion.div
        layout
        className="absolute inset-y-1 bg-white rounded-full shadow-sm border border-gray-100 z-0"
        initial={false}
        animate={{
          left: mode === 'coach' ? '4px' : '50%',
          width: 'calc(50% - 4px)',
          x: mode === 'coach' ? 0 : 0 
        }}
        transition={{ type: "spring", stiffness: 400, damping: 30 }}
      />

      <button
        onClick={() => onModeChange('coach')}
        className={clsx(
          "relative z-10 flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-colors duration-200",
          mode === 'coach' ? "text-purple-600" : "text-gray-500 hover:text-gray-900"
        )}
      >
        <MessageSquare size={16} />
        <span>Coach</span>
      </button>

      <button
        onClick={() => onModeChange('analyst')}
        className={clsx(
          "relative z-10 flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-colors duration-200",
          mode === 'analyst' ? "text-blue-600" : "text-gray-500 hover:text-gray-900"
        )}
      >
        <Microscope size={16} />
        <span>Analyst</span>
      </button>
    </div>
  );
};
