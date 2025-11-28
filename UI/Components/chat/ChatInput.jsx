import React from 'react';
import { ArrowUp, Paperclip } from 'lucide-react';

export default function ChatInput({ input, setInput, onSend, isLoading, onSkillClick }) {
  return (
    <div className="fixed bottom-0 left-0 right-0 p-4 bg-[#F2F2F7]/95 backdrop-blur-md z-30 border-t border-[#E5E5EA] shadow-lg">
      <div className="max-w-3xl mx-auto">
        {/* Skill Buttons */}
        <div className="flex gap-3 overflow-x-auto pb-3 scrollbar-hide mb-2">
          {['内容总结', '异议分析', '话术辅导', '生成日报'].map(skill => (
            <button 
              key={skill}
              onClick={() => onSkillClick(skill)}
              className="flex-shrink-0 px-4 py-2 rounded-full bg-white border border-[#E5E5EA] text-xs font-medium text-black shadow-sm hover:border-[#007AFF] hover:text-[#007AFF] active:bg-[#007AFF]/10 transition-all duration-200"
            >
              {skill}
            </button>
          ))}
        </div>

        {/* Input Field */}
        <div className="relative">
          <div className="absolute left-3 bottom-3 flex gap-2 z-10">
             <button 
               className="p-2 text-[#8E8E93] hover:text-[#007AFF] active:bg-[#F2F2F7] rounded-lg transition-all duration-200"
               aria-label="附加文件"
             >
               <Paperclip className="w-5 h-5" />
             </button>
          </div>

          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                onSend();
              }
            }}
            placeholder="上传文件或输入消息..."
            className="w-full pl-12 pr-14 py-4 bg-white border border-[#E5E5EA] rounded-2xl text-black placeholder-[#8E8E93] focus:outline-none focus:border-[#007AFF] focus:shadow-sm shadow-sm resize-none min-h-[60px] max-h-[120px] transition-all duration-200"
            rows={1}
            aria-label="消息输入框"
          />

          <button 
            onClick={onSend}
            disabled={!input.trim() || isLoading}
            className={`absolute right-2 bottom-2 p-3 rounded-full transition-all duration-200 ${
              input.trim() 
                ? 'bg-[#007AFF] text-white hover:opacity-90 active:scale-95 shadow-md' 
                : 'bg-[#E5E5EA] text-[#8E8E93]'
            }`}
            aria-label="发送消息"
          >
            <ArrowUp className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
}