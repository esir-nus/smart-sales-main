import React, { useState, useEffect, useRef } from 'react';
import { base44 } from '@/api/base44Client';
import ChatBubble from '@/components/chat/ChatBubble';
import { motion, useAnimation } from 'framer-motion';
import AudioDrawer from '@/components/audio/AudioDrawer';
import ChatWelcome from '@/components/chat/ChatWelcome';
import ChatInput from '@/components/chat/ChatInput';

export default function HomePage() {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);

  const [input, setInput] = useState('');
  const [messages, setMessages] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [userName, setUserName] = useState('用户');
  const messagesEndRef = useRef(null);

  useEffect(() => {
    const loadUser = async () => {
      try {
        const user = await base44.auth.me();
        if (user?.full_name) setUserName(user.full_name.split(' ')[0]);
      } catch (e) {
        console.log('User not logged in');
      }
    };
    loadUser();
    
    // Auto sync on enter
    handleSync();
  }, []);

  const handleSync = () => {
    setIsSyncing(true);
    // Simulate sync delay
    setTimeout(() => {
      setIsSyncing(false);
    }, 2000);
  };

  const handlePullDown = (event, info) => {
    if (info.offset.y > 100 && !isDrawerOpen) {
        setIsDrawerOpen(true);
        handleSync();
    }
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() && !isLoading) return;

    const userMsg = {
      role: 'user',
      content: input,
      timestamp: new Date().toISOString(),
    };

    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setIsLoading(true);

    try {
      const prompt = input;
      
      const response = await base44.integrations.Core.InvokeLLM({
        prompt: `You are a smart sales assistant. Be professional, concise, and helpful. Use markdown formatting.
        Respond in Chinese (Simplified).
        User request: ${prompt}
        
        If user asks to analyze a client, provide a structured markdown response like:
        ## 客户画像
        - **性格**: ...
        - **需求**: ...
        ## 销售策略
        - ...
        
        Keep the tone sophisticated.`,
        add_context_from_internet: true
      });

      const aiMsg = {
        role: 'assistant',
        content: response || "抱歉，我暂时无法生成回复。",
        timestamp: new Date().toISOString(),
      };

      setMessages(prev => [...prev, aiMsg]);
    } catch (error) {
      console.error('Error sending message:', error);
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: "抱歉，处理您的请求时出现错误。",
        timestamp: new Date().toISOString(),
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSkillClick = (skill) => {
    setInput(skill);
  };

  return (
    <motion.div 
        className="h-full flex flex-col bg-[#F2F2F7]"
        drag="y"
        dragConstraints={{ top: 0, bottom: 0 }}
        dragElastic={{ top: 0.2, bottom: 0 }}
        onDragEnd={handlePullDown}
    >
      <AudioDrawer 
        isOpen={isDrawerOpen} 
        onClose={() => setIsDrawerOpen(false)} 
        onSync={handleSync}
        isSyncing={isSyncing}
      />
      
      {messages.length === 0 ? (
        <ChatWelcome userName={userName} />
      ) : (
        <div className="flex-1 overflow-y-auto px-2 pb-4">
          {messages.map((msg, idx) => (
            <ChatBubble key={idx} message={msg} />
          ))}
          {isLoading && (
            <div className="flex justify-start mb-6 ml-11">
               <div className="flex gap-1 p-3 bg-white rounded-lg shadow-sm">
                 <div className="w-2 h-2 bg-[#007AFF] rounded-full animate-bounce" />
                 <div className="w-2 h-2 bg-[#007AFF] rounded-full animate-bounce delay-75" />
                 <div className="w-2 h-2 bg-[#007AFF] rounded-full animate-bounce delay-150" />
               </div>
            </div>
          )}
          <div ref={messagesEndRef} />
          </div>
          )}

          <ChatInput 
            input={input}
            setInput={setInput}
            onSend={handleSend}
            isLoading={isLoading}
            onSkillClick={handleSkillClick}
          />
    </motion.div>
  );
}