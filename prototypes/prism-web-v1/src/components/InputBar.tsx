import React from 'react';
import { Paperclip, ArrowUp } from 'lucide-react';

interface InputBarProps {
    topAccessory?: React.ReactNode;
}

export const InputBar: React.FC<InputBarProps> = ({ topAccessory }) => {
  return (
    <div className="absolute bottom-8 left-4 right-4 z-30 flex flex-col items-center gap-4">
        {topAccessory}
        
        <div className="w-full h-16 bg-white/85 backdrop-blur-xl rounded-full shadow-floating border border-white/50 flex items-center px-2">
            {/* Upload Button (Left) */}
            <button className="w-12 h-12 rounded-full flex items-center justify-center text-gray-500 hover:bg-gray-100 transition-colors">
                <Paperclip size={22} className="rotate-45" />
            </button>
            
            {/* Input Field with Scan Shine */}
            <div className="flex-1 px-2 relative h-full flex items-center">
                 <input 
                    type="text" 
                    placeholder="输入消息..." // Localized
                    className="w-full h-full bg-transparent border-none outline-none text-gray-900 placeholder-gray-400 font-medium z-10"
                 />
                 
                 {/* Scan Shine Overlay */}
                 <div className="absolute inset-0 flex items-center pointer-events-none opacity-50">
                    <span className="bg-gradient-to-r from-gray-400 via-gray-900 to-gray-400 bg-[length:200%_auto] text-transparent bg-clip-text animate-scan-shine text-sm font-medium ml-2">
                        {/* Shimmer effect target */}
                    </span>
                 </div>
            </div>
            
            {/* Send Button */}
            <button className="w-12 h-12 rounded-full bg-prism-accent text-white flex items-center justify-center shadow-md hover:scale-105 active:scale-95 transition-transform">
                <ArrowUp size={24} strokeWidth={2.5} />
            </button>
        </div>
    </div>
  );
};
