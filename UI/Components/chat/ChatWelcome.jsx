import React from 'react';
import { motion } from 'framer-motion';

export default function ChatWelcome({ userName }) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center text-center p-6 pb-32">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8"
      >
        <h1 className="text-4xl font-bold text-[#007AFF] mb-2 tracking-tight">LOGO</h1>
        <div className="text-2xl font-bold text-black mb-1">你好, {userName}</div>
        <div className="text-xl text-[#007AFF] font-medium mb-8">我是您的销售助手</div>

        <div className="text-left max-w-xs mx-auto text-[#8E8E93] space-y-2">
          <p>我可以帮您：</p>
          <ul className="list-disc pl-5 space-y-1">
            <li>分析用户画像、意图、痛点。</li>
            <li>生成 PDF、CSV 文档及思维导图。</li>
          </ul>
        </div>
        
        <div className="mt-12 text-[#3A3A3C] font-medium">
          让我们开始吧
        </div>
      </motion.div>
    </div>
  );
}