import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { createPageUrl } from '@/utils';
import ReactMarkdown from 'react-markdown';
import { Bot, Play, Pause, FileText, ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function AudioDrawer({ isOpen, onClose, file }) {
  const navigate = useNavigate();
  const [isPlaying, setIsPlaying] = useState(false);

  // Navigate to Chat with context
  const handleAskAI = () => {
    if (!file) return;
    // Navigate to Home (Chat) with params that ChatViewModel (simulated) would pick up
    navigate(createPageUrl('Home') + `?audioId=${file.id}&context=analyze`);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/40 backdrop-blur-sm"
            style={{ zIndex: 50 }}
          />
          
          {/* Bottom Sheet - Transcript Viewer */}
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: "spring", damping: 30, stiffness: 300 }}
            className="fixed bottom-0 left-0 right-0 h-[85vh] bg-[#F2F2F7] rounded-t-3xl shadow-2xl flex flex-col overflow-hidden"
            style={{ zIndex: 50 }}
          >
            {/* Header */}
            <div className="bg-white/95 backdrop-blur-md border-b border-[#E5E5EA] p-4 rounded-t-3xl flex items-center justify-between sticky top-0 z-10 shadow-sm">
               <div className="flex-1 overflow-hidden">
                 <h2 className="font-bold text-lg text-black flex items-center gap-2">
                   <FileText size={18} className="text-[#007AFF]" />
                   通话转写
                 </h2>
                 <p className="text-xs text-[#8E8E93] truncate">{file?.filename}</p>
               </div>
               <button 
                 onClick={onClose} 
                 className="p-2 bg-[#F2F2F7] rounded-full hover:bg-[#E5E5EA] active:bg-[#E5E5EA]/80 transition-all duration-200 ml-3"
                 aria-label="关闭"
               >
                 <ChevronDown size={20} className="text-[#8E8E93]" />
               </button>
            </div>

            {/* Content Scrollable */}
            <div className="flex-1 overflow-y-auto p-4">
               {/* Audio Player Placeholder */}
               <div className="bg-white rounded-xl p-4 mb-4 border border-[#E5E5EA] shadow-sm flex items-center gap-3">
                  <button 
                    onClick={() => setIsPlaying(!isPlaying)}
                    className="w-10 h-10 bg-[#007AFF] rounded-full flex items-center justify-center text-white shrink-0 shadow-md hover:opacity-90 active:scale-95 transition-all duration-200"
                    aria-label={isPlaying ? '暂停' : '播放'}
                  >
                    {isPlaying ? <Pause size={16} fill="currentColor" /> : <Play size={16} fill="currentColor" className="ml-0.5" />}
                  </button>
                  <div className="flex-1">
                     <div className="h-1 bg-[#E5E5EA] rounded-full overflow-hidden">
                        <div className="h-full bg-[#007AFF] w-1/3" />
                     </div>
                     <div className="flex justify-between mt-1 text-[10px] text-[#8E8E93] font-mono">
                        <span>00:15</span>
                        <span>{file?.duration || '00:45'}</span>
                     </div>
                  </div>
               </div>

               {/* Transcript Text */}
               <div className="bg-white rounded-xl p-4 border border-[#E5E5EA] shadow-sm min-h-[300px]">
                  {file?.transcript ? (
                    <div className="markdown-body text-sm leading-relaxed text-[#333]">
                      <ReactMarkdown>{file.transcript}</ReactMarkdown>
                    </div>
                  ) : (
                    <div className="text-center text-[#8E8E93] py-10">
                      暂无转写内容
                    </div>
                  )}
               </div>
               
               <div className="h-20" /> {/* Spacer */}
            </div>

            {/* Footer Actions */}
            <div className="p-4 bg-white/95 backdrop-blur-md border-t border-[#E5E5EA] pb-8 shadow-lg">
               <Button 
                 onClick={handleAskAI}
                 className="w-full bg-gradient-to-r from-[#007AFF] to-[#5856D6] hover:opacity-90 active:scale-[0.98] text-white shadow-lg h-12 text-base font-semibold rounded-xl flex items-center justify-center gap-2 transition-all duration-200"
               >
                 <Bot size={20} />
                 用 AI 分析本次通话
               </Button>
            </div>

          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}