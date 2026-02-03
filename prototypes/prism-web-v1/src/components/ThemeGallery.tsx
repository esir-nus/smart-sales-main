import React from 'react';
import { motion } from 'framer-motion';
import { clsx } from 'clsx';

type Theme = 'executive' | 'charcoal' | 'ivory' | 'navy' | 'flannel' | 'obsidian' | 'ocean' | 'paper';

interface ThemeGalleryProps {
  currentTheme: string;
  onThemeChange: (theme: string) => void;
  onNavigate: (dest: string) => void;
}

// 高端主题集合 — 专为管理层设计
const themes: { id: Theme; label: string; color: string }[] = [
  { id: 'executive', label: '铂金经典', color: '#F2F2F7' },
  { id: 'charcoal', label: '董事灰', color: '#1F2937' },
  { id: 'ivory', label: '象牙白', color: '#FFFFF0' },
  { id: 'navy', label: '海军蓝', color: '#0A192F' },
  { id: 'flannel', label: '法兰绒', color: '#E5E5E5' },
  { id: 'obsidian', label: '曜黑', color: '#000000' },
  { id: 'ocean', label: '深海', color: '#0C4A6E' },
  { id: 'paper', label: '纸白', color: '#FFFFFF' },
];

export const ThemeGallery: React.FC<ThemeGalleryProps> = ({ currentTheme, onThemeChange, onNavigate }) => {
  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="flex flex-col h-full bg-prism-bg transition-colors duration-500"
    >
      {/* 头部 — 极简设计 */}
      <header className="p-6 pb-4 glass-panel z-10">
        <div className="flex items-center justify-between mb-6">
          <h2 className="t-hero tracking-tight">外观</h2>
          <button 
            onClick={() => onNavigate('Home')} 
            className="text-sm text-prism-accent font-medium hover:opacity-80 transition-opacity"
          >
            完成
          </button>
        </div>

        {/* 主题选择器 — 横向滚动 */}
        <div 
          className="flex gap-4 overflow-x-auto pb-3 theme-scrollbar" 
          style={{ scrollbarWidth: 'thin', scrollbarColor: 'var(--color-accent) var(--bg-surface-muted)' }}
        >
          {themes.map((t) => (
            <button
              key={t.id}
              onClick={() => onThemeChange(t.id === 'executive' ? '' : `theme-${t.id}`)}
              className={clsx(
                "flex flex-col items-center gap-2 min-w-[64px] p-3 rounded-lg transition-all",
                (currentTheme === `theme-${t.id}` || (t.id === 'executive' && currentTheme === ''))
                  ? "bg-prism-surface shadow-sm ring-1 ring-prism-accent/30"
                  : "opacity-60 hover:opacity-100"
              )}
            >
              <div 
                className="w-8 h-8 rounded-md shadow-sm"
                style={{ 
                  backgroundColor: t.color,
                  border: t.color === '#FFFFFF' || t.color === '#FFFFF0' ? '1px solid rgba(0,0,0,0.08)' : 'none'
                }}
              />
              <span className="text-[11px] font-medium text-prism-secondary whitespace-nowrap">
                {t.label}
              </span>
            </button>
          ))}
        </div>
      </header>

      {/* 预览区域 — 模拟真实界面 */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        
        {/* 排版预览 */}
        <section>
          <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">排版</p>
          <div className="glass-card p-5 space-y-3">
            <h1 className="t-hero">智领未来</h1>
            <h2 className="t-title">AI 驱动的销售助理</h2>
            <p className="t-body text-prism-secondary leading-relaxed">
              根据上下文动态调整界面，无论对话还是分析，始终保持高效。
            </p>
            <p className="t-caption">v1.2.0 · 系统正常</p>
          </div>
        </section>

        {/* 对话预览 */}
        <section>
          <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">对话</p>
          <div className="space-y-3">
            {/* 用户消息 */}
            <div className="bg-prism-accent text-white px-4 py-3 rounded-2xl rounded-tr-sm max-w-[80%] ml-auto text-[14px]">
              帮我生成张总的季度分析。
            </div>
            {/* AI 回复 */}
            <div className="glass-card px-4 py-3 rounded-2xl rounded-tl-sm max-w-[80%]">
              <p className="t-body mb-2">已根据沟通记录生成报告。</p>
              <div className="flex gap-2">
                <button className="px-3 py-1.5 rounded-md bg-prism-accent/10 text-[12px] font-medium text-prism-accent">
                  查看
                </button>
                <button className="px-3 py-1.5 rounded-md text-[12px] font-medium text-prism-secondary">
                  稍后
                </button>
              </div>
            </div>
          </div>
        </section>

        {/* 卡片预览 */}
        <section>
          <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">卡片</p>
          <div className="glass-card p-0 overflow-hidden">
            <div className="h-0.5 bg-prism-accent w-full" />
            <div className="p-4 flex justify-between items-center">
              <div className="flex gap-3 items-center">
                <div className="w-9 h-9 rounded-lg bg-prism-accent/8 flex items-center justify-center">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-prism-accent">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                </div>
                <div>
                  <div className="t-body font-semibold">季度预算复盘</div>
                  <div className="t-caption">14:00 · 会议室 A</div>
                </div>
              </div>
              <div className="w-7 h-7 rounded-full bg-prism-surface-muted flex items-center justify-center text-[11px] font-semibold text-prism-secondary">
                李
              </div>
            </div>
          </div>
        </section>

        {/* 输入预览 */}
        <section>
          <p className="text-[10px] font-semibold text-prism-secondary tracking-widest uppercase mb-3">输入</p>
          <div className="glass-card p-4 space-y-3">
            <input 
              type="text" 
              placeholder="请输入..."
              className="w-full bg-prism-surface-muted/40 border border-prism-accent/10 rounded-lg px-4 py-2.5 outline-none focus:ring-1 focus:ring-prism-accent/20 transition-all text-[14px] placeholder:text-prism-secondary/50"
            />
            <div className="flex gap-2">
              <button className="flex-1 bg-prism-accent text-white py-2.5 rounded-lg font-semibold text-[13px] hover:brightness-105 transition-all">
                确认
              </button>
              <button className="flex-1 bg-prism-surface-muted text-prism-secondary py-2.5 rounded-lg font-medium text-[13px] hover:bg-black/5 transition-all">
                取消
              </button>
            </div>
          </div>
        </section>

      </div>
    </motion.div>
  );
};
