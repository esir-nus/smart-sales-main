import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { User, Bot, Copy, Check } from 'lucide-react';

export default function ChatBubble({ message }) {
  const isUser = message.role === 'user';
  const [isCopied, setIsCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message.content);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };
  
  return (
    <div className={`flex w-full mb-6 ${isUser ? 'justify-end' : 'justify-start'}`}>
      {!isUser && (
        <div className="mr-3 flex-shrink-0">
          <div className="w-8 h-8 rounded-full bg-white border border-[#E5E5EA] flex items-center justify-center text-[#007AFF] shadow-sm">
            <Bot size={18} />
          </div>
        </div>
      )}
      
      <div className={`max-w-[85%] ${isUser ? 'ml-auto' : ''}`}>
        {isUser ? (
          <div className="bg-[#007AFF] p-4 rounded-tl-2xl rounded-tr-2xl rounded-bl-2xl rounded-br-sm text-white shadow-md">
            {message.content}
          </div>
        ) : (
          <div className="bg-white border border-[#E5E5EA] p-4 rounded-tl-2xl rounded-tr-2xl rounded-br-2xl rounded-bl-sm shadow-sm group relative">
            <div className="markdown-body text-sm pb-2">
              <ReactMarkdown>{message.content}</ReactMarkdown>
            </div>
            <div className="flex justify-end pt-2 mt-1 border-t border-[#F2F2F7]">
              <button 
                onClick={handleCopy}
                className="flex items-center gap-1.5 px-2 py-1 rounded-md text-xs font-medium text-[#8E8E93] hover:text-[#007AFF] hover:bg-[#F2F2F7] transition-colors"
              >
                {isCopied ? (
                  <>
                    <Check size={12} />
                    <span>已复制</span>
                  </>
                ) : (
                  <>
                    <Copy size={12} />
                    <span>复制</span>
                  </>
                )}
              </button>
            </div>
          </div>
        )}
        <div className={`text-[10px] text-[#8E8E93] mt-1 ${isUser ? 'text-right' : 'text-left'}`}>
          {new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </div>
      </div>

      {isUser && (
        <div className="ml-3 flex-shrink-0">
          <div className="w-8 h-8 rounded-full bg-[#E5E5EA] flex items-center justify-center text-[#636366]">
            <User size={18} />
          </div>
        </div>
      )}
    </div>
  );
}