import React from 'react';
import { clsx } from 'clsx';

interface MobileFrameProps {
  children: React.ReactNode;
}

export const MobileFrame: React.FC<MobileFrameProps> = ({ children }) => {
  return (
    <div className="min-h-screen w-full bg-[#050505] flex items-center justify-center p-8 font-sans antialiased text-prism-primary">
      {/* Device Bezel */}
      <div className="relative w-[430px] h-[932px] bg-prism-bg rounded-[60px] shadow-2xl overflow-hidden border-[14px] border-[#1a1a1a] ring-1 ring-white/10">
        
        {/* Dynamic Island Area (Chrome) */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[126px] h-[37px] bg-black rounded-b-[24px] z-[60] pointer-events-none transition-all duration-300 ease-out" />
        
        {/* Status Bar Time/Icons simulated (Optional, keeping clean for now) */}

        {/* Home Indicator */}
        <div className="absolute bottom-[8px] left-1/2 -translate-x-1/2 w-[140px] h-[5px] bg-black/80 rounded-full z-[60] pointer-events-none backdrop-blur-md" />

        {/* Content Viewport */}
        <div className="w-full h-full relative flex flex-col items-center">
            {children}
        </div>

      </div>
    </div>
  );
};
