import React, { useState, useEffect, useRef } from 'react';
import { base44 } from '@/api/base44Client';
import ChatBubble from '@/components/chat/ChatBubble';
import ChatWelcome from '@/components/chat/ChatWelcome';
import ChatInput from '@/components/chat/ChatInput';

export default function HomeView() {
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

    // Handle URL params
    const params = new URLSearchParams(window.location.search);
    const context = params.get('context');
    const sessionId = params.get('sessionId');

    if (context && context.startsWith('audio_')) {
       setIsLoading(true);
       setTimeout(() => {
          setMessages(prev => [...prev, {
             role: 'system',
             content: `**已加载录音转写上下文 (ID: ${context.split('_')[1]})**\n\n正在分析通话内容...`,
             timestamp: new Date().toISOString()
          }]);
          setIsLoading(false);
          
          setTimeout(() => {
             setMessages(prev => [...prev, {
                role: 'assistant',
                content: "我已经阅读了该段通话的转写内容。客户主要关注价格问题，建议您从“长期价值”和“售后服务”两个角度进行回应。\n\n您想让我为您起草一段回复话术吗？",
                timestamp: new Date().toISOString()
             }]);
          }, 1000);
       }, 500);
    }

    if (sessionId) {
       setMessages([
          { role: 'user', content: "上周的销售数据怎么看？", timestamp: "2025-02-14T10:00:00Z" },
          { role: 'assistant', content: "上周销售额同比增长 15%，主要由 X1 型号贡献。", timestamp: "2025-02-14T10:00:05Z" }
       ]);
    }
  }, []);

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
        add_context_from_internet: true
      `});

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
    <div className="h-full flex flex-col bg-[#F2F2F7] relative">
      {messages.length === 0 ? (
        <ChatWelcome userName={userName} />
      ) : (
        <div className="flex-1 overflow-y-auto px-2 pb-4 pt-8">
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
    </div>
  );
}