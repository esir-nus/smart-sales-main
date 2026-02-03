import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { clsx } from 'clsx';

type Chapter = 'flannel' | 'glass' | 'glass-biz' | 'sleek' | 'obsidian';

interface VIGuidebookProps {
  onNavigate: (dest: string) => void;
  onThemeChange: (theme: string) => void;
}

// VI 设计规范手册
export const VIGuidebook: React.FC<VIGuidebookProps> = ({ onNavigate, onThemeChange }) => {
  const [chapter, setChapter] = useState<Chapter>('flannel');

  const handleChapterChange = (ch: Chapter) => {
    setChapter(ch);
    // 映射章节到 index.css 中的 theme class
    const themeMap = {
      'flannel': 'theme-flannel',
      'glass': 'theme-glass',
      'glass-biz': 'theme-glass-biz',
      'sleek': 'theme-glass-sleek',
      'obsidian': 'theme-glass-obsidian'
    };
    onThemeChange(themeMap[ch]);
  };

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="flex flex-col h-full bg-prism-bg transition-colors duration-700"
    >
      {/* 封面头部 */}
      <header className="p-6 pb-4 glass-panel z-10">
        <div className="flex items-center justify-between mb-1">
          <span className="text-[10px] font-semibold text-prism-secondary tracking-[0.2em] uppercase">
            Smart Sales
          </span>
          <button 
            onClick={() => onNavigate('Home')} 
            className="text-sm text-prism-accent font-medium"
          >
            关闭
          </button>
        </div>
        <h1 className="t-hero tracking-tight mb-4">视觉识别规范</h1>
        
        {/* 章节导航 */}
        <div className="flex border-b border-prism-accent/10 overflow-x-auto pb-1" style={{ scrollbarWidth: 'thin' }}>
          {(['flannel', 'glass', 'glass-biz', 'sleek', 'obsidian'] as Chapter[]).map((c, i) => (
             <button
               key={c}
               onClick={() => handleChapterChange(c)}
               className={clsx(
                 "flex-1 py-3 text-sm font-medium transition-all relative min-w-[100px] whitespace-nowrap px-4",
                 chapter === c 
                   ? "text-prism-accent" 
                   : "text-prism-secondary hover:text-prism-accent/70"
               )}
             >
               {i === 0 && '第二章 · 法兰绒'}
               {i === 1 && '第三章 · 毛玻璃'}
               {i === 2 && '第四章 · 商务'}
               {i === 3 && '第五章 · 极简'}
               {i === 4 && '第六章 · 黑曜石'}
               {chapter === c && (
                 <motion.div 
                   layoutId="chapter-indicator"
                   className="absolute bottom-0 left-0 right-0 h-0.5 bg-prism-accent"
                 />
               )}
             </button>
          ))}
        </div>
      </header>

      {/* 章节内容 */}
      <div className="flex-1 overflow-y-auto">
        <AnimatePresence mode="wait">
          {chapter === 'flannel' && <FlannelChapter key="flannel" />}
          {chapter === 'glass' && <GlassChapter key="glass" />}
          {chapter === 'glass-biz' && <GlassBizChapter key="glass-biz" />}
          {chapter === 'sleek' && <SleekChapter key="sleek" />}
          {chapter === 'obsidian' && <ObsidianChapter key="obsidian" />}
        </AnimatePresence>
      </div>
    </motion.div>
  );
};

// 第六章：黑曜石 (Sleek Dark Pro Max)
const ObsidianChapter: React.FC = () => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -20 }}
    transition={{ duration: 0.3 }}
    className="p-6 space-y-8"
  >
    {/* 概述 */}
    <section>
      <h2 className="text-lg font-bold text-prism-accent mb-2">黑曜石 Obsidian</h2>
      <p className="t-body text-prism-secondary leading-relaxed">
        Pro Max 极简黑暗模式。纯黑背景与高透光玻璃的极致对比。像屏幕熄灭后的静谧，深邃而充满张力。
      </p>
    </section>

    {/* 色彩规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">色彩</p>
      <div className="grid grid-cols-2 gap-3">
        <ColorSwatch label="背景" value="#000000" />
        <ColorSwatch label="表面 (60%)" value="rgba(28,28,30,0.6)" />
        <ColorSwatch label="强调色" value="#FFFFFF" />
        <ColorSwatch label="文字" value="#FFFFFF" />
      </div>
    </section>

    {/* 排版规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">排版</p>
      <div className="glass-card p-4 space-y-3">
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字体</span>
          <span className="font-mono text-sm text-prism-accent">SF Pro Text</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字间距</span>
          <span className="font-mono text-sm">-0.02em</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">边框</span>
          <span className="font-mono text-sm">0.5px (White/12)</span>
        </div>
      </div>
    </section>

    {/* 组件示例 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">组件</p>
      <div className="space-y-4">
        {/* Pro Max Dark Demo */}
        <div className="relative h-48 rounded-[32px] overflow-hidden flex items-center justify-center p-8 bg-black">
           {/* Dark Mesh Gradient */}
           <div className="absolute w-[120%] h-[120%] bg-[radial-gradient(circle_at_50%_0%,rgba(50,50,200,0.3),rgba(0,0,0,0))]" />
           <div className="absolute w-60 h-60 bg-purple-900 rounded-full blur-[80px] bottom-[-20%] left-[-20%] opacity-40" />
           
           {/* Premium Dark Glass Card */}
           <div className="relative w-full h-full p-6 flex flex-col justify-between rounded-[24px] border-[0.5px] border-white/10 bg-[#1C1C1E]/60 backdrop-blur-[50px] shadow-[0_20px_40px_-10px_rgba(0,0,0,0.5)]">
              <div className="flex justify-between items-start">
                <div className="space-y-1">
                  <p className="text-[10px] font-semibold text-[#8E8E93] uppercase tracking-wide">Usage</p>
                  <div className="flex items-baseline gap-1">
                     <p className="text-3xl font-semibold text-white tracking-tight">8.2</p>
                     <p className="text-sm text-[#8E8E93]">GB</p>
                  </div>
                </div>
                <div className="w-10 h-10 rounded-full bg-white/10 text-white flex items-center justify-center border border-white/5">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
                  </svg>
                </div>
              </div>
              
              {/* Progress Bar */}
              <div className="space-y-2">
                 <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                    <div className="h-full w-2/3 bg-white shadow-[0_0_10px_rgba(255,255,255,0.5)]" />
                 </div>
              </div>
           </div>
        </div>
      </div>
    </section>
  </motion.div>
);

// 第五章：极简毛玻璃 (Sleek Glass Pro Max)
const SleekChapter: React.FC = () => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -20 }}
    transition={{ duration: 0.3 }}
    className="p-6 space-y-8"
  >
    {/* 概述 */}
    <section>
      <h2 className="text-lg font-bold text-prism-accent mb-2">极简 Sleek Glass</h2>
      <p className="t-body text-prism-secondary leading-relaxed">
        Pro Max 级设计。极致纯净，使用 Apple 风格的高斯模糊和超细边框。摒弃多余装饰，还原内容本质。
      </p>
    </section>

    {/* 色彩规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">色彩</p>
      <div className="grid grid-cols-2 gap-3">
        <ColorSwatch label="背景" value="var(--bg-app)" />
        <ColorSwatch label="表面 (70%)" value="var(--bg-surface)" />
        <ColorSwatch label="强调色" value="var(--color-accent)" />
        <ColorSwatch label="文字" value="var(--color-text-primary)" />
      </div>
    </section>

    {/* 排版规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">排版</p>
      <div className="glass-card p-4 space-y-3">
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字体</span>
          <span className="text-sm text-prism-accent" style={{ fontFamily: 'var(--font-family-base)' }}>SF Pro Text</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字间距</span>
          <span className="font-mono text-sm">-0.02em</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">边框</span>
          <span className="font-mono text-sm">0.5px</span>
        </div>
      </div>
    </section>

    {/* 组件示例 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">组件</p>
      <div className="space-y-4">
        {/* Pro Max Demo */}
        <div 
            className="relative h-48 rounded-[32px] overflow-hidden flex items-center justify-center p-8 transition-colors duration-300"
            style={{ backgroundColor: 'var(--bg-app)' }}
        >
           {/* Sophisticated Mesh Gradient */}
           <div className="absolute w-[120%] h-[120%] bg-[radial-gradient(circle_at_50%_120%,rgba(120,119,198,0.3),rgba(255,255,255,0))]" />
           
           {/* Premium Glass Card */}
           <div 
                className="relative w-full h-full p-6 flex flex-col justify-between backdrop-blur-[50px] transition-all duration-300"
                style={{ 
                    backgroundColor: 'var(--bg-surface)',
                    border: 'var(--border-width) solid var(--border-base)',
                    borderRadius: 'var(--radius-card)',
                    boxShadow: 'var(--shadow-glass)'
                }}
           >
              <div className="flex justify-between items-start">
                <div className="space-y-1">
                  <p className="text-[10px] font-semibold text-prism-secondary uppercase tracking-wide" style={{ letterSpacing: 'var(--font-spacing)' }}>Balance</p>
                  <p className="text-3xl font-semibold text-prism-primary tracking-tight" style={{ fontFamily: 'var(--font-family-base)' }}>$128,400</p>
                </div>
                <div className="w-10 h-10 rounded-full bg-prism-accent text-white flex items-center justify-center">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M7 17L17 7M17 7H7M17 7V17" />
                  </svg>
                </div>
              </div>
           </div>
        </div>
      </div>
    </section>

    {/* 核心交互 */}
    <section>
      <p className="text-[10px] font-semibold text-[#86868B] tracking-[0.1em] uppercase mb-4">核心交互</p>
      <div className="space-y-5">
        
        {/* 1. Cognition Trace (Thinking Box) — Redesigned */}
        <div 
          className="relative rounded-[20px] border-[0.5px] border-black/[0.04] bg-white/70 backdrop-blur-[50px] p-5"
          style={{ fontFamily: '"SF Pro Text", "Inter", system-ui, sans-serif', letterSpacing: '-0.02em' }}
        >
          <div className="flex items-center gap-3 mb-3">
            <div className="w-6 h-6 rounded-full bg-[#F5F5F7] flex items-center justify-center">
              <div className="w-2 h-2 rounded-full bg-black/60" />
            </div>
            <span className="text-[13px] font-medium text-[#1D1D1F]">深度分析中</span>
          </div>
          <div className="pl-9 space-y-2">
            <p className="text-[12px] text-[#86868B] leading-relaxed">识别多模态输入</p>
            <p className="text-[12px] text-[#86868B] leading-relaxed">构建分析上下文</p>
          </div>
        </div>

        {/* 2. Input Capsule — Redesigned */}
        <div 
          className="relative flex items-center gap-3 px-4 py-3 rounded-[16px] border-[0.5px] border-black/[0.04] bg-white/70 backdrop-blur-[50px]"
          style={{ fontFamily: '"SF Pro Text", "Inter", system-ui, sans-serif', letterSpacing: '-0.02em' }}
        >
           <span className="text-[13px] text-[#86868B] flex-1">输入消息</span>
           <div className="w-7 h-7 rounded-full bg-black flex items-center justify-center">
             <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round">
               <path d="M5 12h14M12 5l7 7-7 7" />
             </svg>
           </div>
        </div>

        {/* 3. Session Item — Redesigned */}
        <div 
          className="py-3"
          style={{ fontFamily: '"SF Pro Text", "Inter", system-ui, sans-serif', letterSpacing: '-0.02em' }}
        >
          <div className="flex items-baseline justify-between mb-1">
            <span className="text-[14px] font-semibold text-[#1D1D1F]">张总</span>
            <span className="text-[11px] text-[#86868B]">14:20</span>
          </div>
          <p className="text-[12px] text-[#86868B]">Q4预算审查</p>
        </div>

      </div>
    </section>

    {/* 生产力组件 (Productivity) */}
    <section>
      <p className="text-[10px] font-semibold text-[#86868B] tracking-[0.1em] uppercase mb-4">生产力与状态</p>
      <div className="space-y-6">
        
        {/* 1. Task Card (Standard) */}
        <div>
          <p className="text-[10px] text-[#86868B] mb-2 pl-1">日程卡片 (标准)</p>
          <div 
            className="relative flex items-center justify-between p-4 rounded-[16px] border-[0.5px] border-black/[0.04] bg-white/70 backdrop-blur-[50px]"
            style={{ fontFamily: '"SF Pro Text", "Inter", system-ui, sans-serif', letterSpacing: '-0.02em' }}
          >
            <div className="flex items-center gap-4">
               <span className="text-[14px] font-medium text-[#86868B] w-10">09:30</span>
               <div className="w-[1px] h-4 bg-black/10"></div>
               <span className="text-[14px] font-semibold text-[#1D1D1F]">产品策略周会</span>
            </div>
            <div className="w-5 h-5 rounded-full border border-black/10 flex items-center justify-center">
              <div className="w-2.5 h-2.5 bg-transparent rounded-[2px]" />
            </div>
          </div>
        </div>

        {/* 2. Task Card (Swipe Delete State) */}
        <div>
          <p className="text-[10px] text-[#86868B] mb-2 pl-1">交互状态 (左滑删除)</p>
          <div className="relative h-[56px]">
            {/* Background Layer (Actions) */}
            <div className="absolute inset-y-0 right-0 w-full flex items-center justify-end gap-2 pr-0">
               <div className="h-full bg-[#FF3B30] w-[80px] rounded-[16px] flex items-center justify-center shadow-inner">
                 <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                   <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2M10 11v6M14 11v6" />
                 </svg>
               </div>
            </div>
            {/* Foreground Layer (Card) */}
            <div 
              className="absolute inset-y-0 left-0 w-full flex items-center justify-between p-4 rounded-[16px] border-[0.5px] border-black/[0.04] bg-white/80 backdrop-blur-[50px] shadow-[0_4px_12px_rgba(0,0,0,0.05)] transform -translate-x-[90px]"
              style={{ fontFamily: '"SF Pro Text", "Inter", system-ui, sans-serif', letterSpacing: '-0.02em' }}
            >
              <div className="flex items-center gap-4">
                 <span className="text-[14px] font-medium text-[#86868B] w-10">11:00</span>
                 <div className="w-[1px] h-4 bg-black/10"></div>
                 <span className="text-[14px] font-semibold text-[#1D1D1F] line-through opacity-50">与客户午餐</span>
              </div>
            </div>
          </div>
        </div>

        {/* 3. Conflict Card (Stacked) */}
        <div>
          <p className="text-[10px] text-[#86868B] mb-2 pl-1">冲突检测 (堆叠态)</p>
          <div className="relative h-[68px]">
             {/* Bottom Card */}
             <div className="absolute top-2 left-0 right-0 h-full bg-white/40 border-[0.5px] border-black/[0.02] rounded-[16px] transform scale-[0.96] translate-y-1"></div>
             
             {/* Top Card */}
             <div 
               className="absolute top-0 left-0 right-0 flex items-center justify-between p-4 rounded-[16px] border-[0.5px] border-[#FF9500]/30 bg-white/80 backdrop-blur-[50px] shadow-[0_8px_24px_-8px_rgba(255,149,0,0.15)]"
               style={{ fontFamily: '"SF Pro Text", "Inter", system-ui, sans-serif', letterSpacing: '-0.02em' }}
             >
                <div className="flex items-center gap-3">
                   <div className="w-5 h-5 rounded-full bg-[#FF9500]/10 flex items-center justify-center text-[#FF9500] text-[10px] font-bold">!</div>
                   <span className="text-[14px] font-semibold text-[#1D1D1F]">日程冲突: 2个事项</span>
                </div>
                <span className="text-[12px] font-medium text-[#FF9500]">解决</span>
             </div>
          </div>
        </div>

        {/* 4. Audio Card (Processing State) */}
        <div>
          <p className="text-[10px] text-[#86868B] mb-2 pl-1">音频处理 (转写中)</p>
          <div 
            className="relative p-4 rounded-[16px] border-[0.5px] border-black/[0.04] bg-white/70 backdrop-blur-[50px]"
            style={{ fontFamily: '"SF Pro Text", "Inter", system-ui, sans-serif', letterSpacing: '-0.02em' }}
          >
            <div className="flex justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-black/5 flex items-center justify-center">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#1D1D1F" strokeWidth="2">
                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                  </svg>
                </div>
                <div>
                   <p className="text-[13px] font-semibold text-[#1D1D1F]">会议录音_001.wav</p>
                   <p className="text-[10px] text-[#86868B]">10:24 • 42MB</p>
                </div>
              </div>
              <div className="flex items-center">
                <span className="text-[10px] font-medium text-[#1D1D1F] bg-black/5 px-2 py-1 rounded-full">35%</span>
              </div>
            </div>
            
            {/* Elegant Progress Bar */}
            <div className="h-[2px] w-full bg-black/5 rounded-full overflow-hidden">
               <div className="h-full bg-[#1D1D1F] w-[35%] rounded-full shadow-[0_0_10px_rgba(0,0,0,0.2)]"></div>
            </div>
          </div>
        </div>

      </div>
    </section>
  </motion.div>
);

// 第二章：法兰绒主题
const FlannelChapter: React.FC = () => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -20 }}
    transition={{ duration: 0.3 }}
    className="p-6 space-y-8"
  >
    {/* 概述 */}
    <section>
      <h2 className="text-lg font-bold text-prism-accent mb-2">法兰绒 Flannel</h2>
      <p className="t-body text-prism-secondary leading-relaxed">
        经典商务灰调。传统、稳重、可信赖。适用于正式商务场景和文档阅读。
      </p>
    </section>

    {/* 色彩规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">色彩</p>
      <div className="grid grid-cols-2 gap-3">
        <ColorSwatch label="背景" value="#E5E5E5" />
        <ColorSwatch label="表面" value="#F5F5F5" />
        <ColorSwatch label="强调色" value="#4B5563" />
        <ColorSwatch label="文字" value="#171717" />
      </div>
    </section>

    {/* 排版规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">排版</p>
      <div className="glass-card p-4 space-y-3">
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字体</span>
          <span className="font-mono text-sm text-prism-accent">Helvetica Neue</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字间距</span>
          <span className="font-mono text-sm">0em</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">圆角</span>
          <span className="font-mono text-sm">12px</span>
        </div>
      </div>
    </section>

    {/* 组件示例 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">组件</p>
      <div className="space-y-3">
        <div className="glass-card p-4 rounded-xl">
          <p className="t-body font-semibold mb-1">卡片标题</p>
          <p className="t-caption">卡片描述文本</p>
        </div>
        <button className="w-full bg-prism-accent text-white py-3 rounded-xl font-semibold text-sm">
          主要按钮
        </button>
      </div>
    </section>
  </motion.div>
);

// 第三章：毛玻璃主题
const GlassChapter: React.FC = () => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -20 }}
    transition={{ duration: 0.3 }}
    className="p-6 space-y-8"
  >
    {/* 概述 */}
    <section>
      <h2 className="text-lg font-bold text-prism-accent mb-2">毛玻璃 Glassmorphism</h2>
      <p className="t-body text-prism-secondary leading-relaxed">
        现代、通透、高级感。利用背景模糊和半透明层营造层次深度。适用于强调内容层级和沉浸式体验。
      </p>
    </section>

    {/* 色彩规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">色彩</p>
      <div className="grid grid-cols-2 gap-3">
        <ColorSwatch label="背景" value="#E0E7FF" />
        <ColorSwatch label="表面 (40%)" value="rgba(255,255,255,0.4)" />
        <ColorSwatch label="强调色" value="#6366F1" />
        <ColorSwatch label="文字" value="#1E1B4B" />
      </div>
    </section>

    {/* 排版规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">排版</p>
      <div className="glass-card p-4 space-y-3">
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字体</span>
          <span className="font-mono text-sm text-prism-accent">Inter</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字间距</span>
          <span className="font-mono text-sm">0em</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">圆角</span>
          <span className="font-mono text-sm">24px</span>
        </div>
      </div>
    </section>

    {/* 组件示例 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">组件</p>
      <div className="space-y-4">
        {/* Layered Glass Effect Demo */}
        <div className="relative h-40 rounded-3xl overflow-hidden shadow-sm border border-white/40 flex items-center justify-center p-6 bg-gradient-to-br from-indigo-300 via-purple-300 to-pink-300">
           {/* Back Layer */}
           <div className="absolute w-20 h-20 bg-blue-500 rounded-full blur-[40px] top-4 left-4 opacity-60 animate-pulse" />
           
           {/* Front Glass Card */}
           <div className="glass-card relative w-full h-full p-4 flex flex-col justify-between border-opacity-60 bg-white/30 backdrop-blur-xl">
              <div>
                <p className="text-xs font-bold text-indigo-900 mb-1">透光测试</p>
                <div className="h-2 w-2/3 bg-indigo-900/10 rounded-full" />
              </div>
              <button className="w-full py-2 bg-indigo-600/90 text-white rounded-xl text-xs font-medium shadow-lg backdrop-blur-sm">
                确认
              </button>
           </div>
        </div>
      </div>
    </section>
  </motion.div>
);

// 第四章：商务毛玻璃
const GlassBizChapter: React.FC = () => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -20 }}
    transition={{ duration: 0.3 }}
    className="p-6 space-y-8"
  >
    {/* 概述 */}
    <section>
      <h2 className="text-lg font-bold text-prism-accent mb-2">商务透视 Business Glass</h2>
      <p className="t-body text-prism-secondary leading-relaxed">
        克制、中性、行政级。相比普通毛玻璃，采用更高不透明度和冷灰色调，消除娱乐感，保留通透质感。
      </p>
    </section>

    {/* 色彩规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">色彩</p>
      <div className="grid grid-cols-2 gap-3">
        <ColorSwatch label="背景" value="#F1F5F9" />
        <ColorSwatch label="表面 (65%)" value="rgba(255,255,255,0.65)" />
        <ColorSwatch label="强调色" value="#334155" />
        <ColorSwatch label="文字" value="#0F172A" />
      </div>
    </section>

    {/* 排版规范 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">排版</p>
      <div className="glass-card p-4 space-y-3">
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字体</span>
          <span className="font-mono text-sm text-prism-accent">SF Pro Display</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">字间距</span>
          <span className="font-mono text-sm">-0.01em</span>
        </div>
        <div className="flex justify-between items-baseline">
          <span className="text-[11px] text-prism-secondary">圆角</span>
          <span className="font-mono text-sm">16px</span>
        </div>
      </div>
    </section>

    {/* 组件示例 */}
    <section>
      <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">组件</p>
      <div className="space-y-4">
        {/* Business Glass Demo */}
        <div className="relative h-40 rounded-3xl overflow-hidden flex items-center justify-center p-6 bg-slate-100">
           {/* Subtle corporate blob */}
           <div className="absolute w-32 h-32 bg-slate-300 rounded-full blur-[50px] top-[-20px] right-[-20px] opacity-40" />
           
           {/* Front Corporate Glass Card */}
           <div className="glass-card relative w-full h-full p-5 flex flex-col justify-between border border-white/50 bg-white/60 backdrop-blur-xl shadow-sm">
              <div className="flex justify-between items-start">
                <div>
                  <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-1">Q4 Revenue</p>
                  <p className="text-xl font-bold text-slate-800">$2.4M</p>
                </div>
                <div className="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-600">
                  📈
                </div>
              </div>
              <div className="w-full h-1 bg-slate-200 rounded-full overflow-hidden">
                <div className="h-full w-3/4 bg-slate-700" />
              </div>
           </div>
        </div>
      </div>
    </section>
  </motion.div>
);

// 色块组件
const ColorSwatch: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div className="glass-card p-3 flex items-center gap-3">
    <div 
      className="w-10 h-10 rounded-lg shadow-sm flex-shrink-0"
      style={{ 
        backgroundColor: value,
        border: value.includes('rgba') || value === '#F5F5F5' || value === '#E5E5E5' || value === '#F8FAFC' || value === '#FFFFFF'
          ? '1px solid rgba(0,0,0,0.1)' 
          : 'none'
      }}
    />
    <div className="min-w-0">
      <p className="text-[11px] text-prism-secondary">{label}</p>
      <p className="text-[10px] font-mono text-prism-accent truncate">{value}</p>
    </div>
  </div>
);
