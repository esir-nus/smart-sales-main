import React, { type ReactNode } from 'react';

interface MobileFrameProps {
  children: ReactNode;
  theme?: string;
}

export const MobileFrame: React.FC<MobileFrameProps> = ({ children, theme = '' }) => {
  return (
    <div className={`relative w-[375px] h-[812px] bg-prism-bg shadow-2xl rounded-[44px] overflow-hidden border-[8px] border-gray-900 box-content ring-4 ring-gray-900/20 ${theme}`}>
      {/* Dynamic Island / Notch Placeholder */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[120px] h-[36px] bg-black rounded-b-[20px] z-50 pointer-events-none" />
      
      {/* Screen Content */}
      <div className="w-full h-full relative flex flex-col font-sans text-gray-900">
        {children}
      </div>
      
      {/* Home Bar Indicator */}
      <div className="absolute bottom-[8px] left-1/2 -translate-x-1/2 w-[130px] h-[5px] bg-black/20 rounded-full z-50 pointer-events-none" />
    </div>
  );
};
