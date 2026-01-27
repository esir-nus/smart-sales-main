import React from 'react';
import { AlignJustify, Package, Mic } from 'lucide-react';

interface RightToolbarProps {
  onAudioClick?: () => void;
}

export const RightToolbar: React.FC<RightToolbarProps> = ({ onAudioClick }) => {
  return (
    <div className="absolute right-4 top-24 flex flex-col gap-4 z-20">
      <button className="w-10 h-10 rounded-full bg-white/40 backdrop-blur-sm flex items-center justify-center text-gray-700 shadow-sm border border-white/30 hover:bg-white/60 transition-colors">
        <AlignJustify size={20} />
      </button>
      <button className="w-10 h-10 rounded-full bg-white/40 backdrop-blur-sm flex items-center justify-center text-gray-700 shadow-sm border border-white/30 hover:bg-white/60 transition-colors">
        <Package size={20} />
      </button>
      <button 
        onClick={onAudioClick}
        className="w-10 h-10 rounded-full bg-white/40 backdrop-blur-sm flex items-center justify-center text-gray-700 shadow-sm border border-white/30 hover:bg-white/60 transition-colors"
      >
        <Mic size={20} />
      </button>
    </div>
  );
};
