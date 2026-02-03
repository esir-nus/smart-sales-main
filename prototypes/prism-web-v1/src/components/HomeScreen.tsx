import React from 'react';
import { motion } from 'framer-motion';
import { Clock, MoreHorizontal, Calendar, Zap, LayoutGrid, ArrowRight, Sparkles } from 'lucide-react';
import { clsx } from 'clsx';

// Types
export interface Session {
  id: string;
  date: string; // YYYY-MM-DD
  time?: string; // HH:MM
  title: string;
  summary: string;
  type: 'coach' | 'analyst';
  isPinned?: boolean;
}

// Grouping Helper
const groupSessions = (sessions: Session[]) => {
  const today = '2026-01-27'; // Mock Current Date
  const yesterday = '2026-01-26';

  const groups = {
    today: sessions.filter(s => s.date === today),
    yesterday: sessions.filter(s => s.date === yesterday),
    older: sessions.filter(s => s.date < yesterday)
  };
  return groups;
};

// --- Components ---

const SessionCard = ({ session }: { session: Session; onClick?: () => void }) => (
  <motion.div
    className="glass-card p-4 hover:shadow-md transition-all cursor-pointer relative group overflow-hidden"
  >
    {/* Floating Gradient Accent */}
    <div className={clsx(
        "absolute top-0 left-0 w-1 h-full opacity-0 group-hover:opacity-100 transition-opacity duration-300",
        session.type === 'analyst' ? "bg-gradient-to-b from-blue-400 to-cyan-300" : "bg-gradient-to-b from-purple-400 to-pink-300"
    )} />

    <div className="flex justify-between items-start mb-2">
       <div className="flex items-center gap-2 text-xs font-medium text-prism-secondary uppercase tracking-wider">
          {session.type === 'analyst' ? <LayoutGrid size={12} className="text-prism-accent" /> : <Zap size={12} className="text-prism-knot" />}
          <span>{session.type === 'analyst' ? '深度分析' : '日常会话'}</span>
          {session.time && <span className="text-prism-secondary/60">• {session.time}</span>}
       </div>
       {session.isPinned && <div className="w-1.5 h-1.5 rounded-full bg-prism-danger" />}
    </div>
    
    <h3 className="text-prism-primary font-semibold mb-1 leading-tight group-hover:text-prism-accent transition-colors">
        {session.title}
    </h3>
    <p className="text-sm text-prism-secondary line-clamp-2 leading-relaxed">
        {session.summary}
    </p>

    {/* Hover Arrow */}
    <div className="absolute bottom-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity transform translate-x-2 group-hover:translate-x-0">
        <ArrowRight size={16} className="text-prism-secondary" />
    </div>
  </motion.div>
);

const SectionHeader = ({ title, icon }: { title: string, icon?: React.ReactNode }) => (
  <div className="flex items-center gap-2 px-2 pb-2 mt-6 mb-1 opacity-60">
      {icon}
      <h4 className="text-xs font-bold text-prism-secondary uppercase tracking-widest">{title}</h4>
      <div className="h-px bg-prism-border flex-1 ml-2" />
  </div>
);

// --- Home Hero (Clean Desk V2) ---
const HomeHero = () => (
    <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="flex-1 flex flex-col items-center justify-center text-center p-8 pb-32"
    >
        {/* Breathing Aura (via Style Guide V16 - 3 Blobs handled by global Aurora, here just the focus) */}
        <div className="relative w-48 h-48 mb-8">
             {/* Local Concentric Pulse to emphasize 'Living' */}
            <motion.div 
                animate={{ scale: [1, 1.1, 1], opacity: [0.1, 0.3, 0.1] }}
                transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
                className="absolute inset-0 bg-prism-accent/30 rounded-full blur-3xl"
            />
            
            {/* Center Icon */}
            <div className="absolute inset-0 flex items-center justify-center">
                 <div className="w-20 h-20 bg-prism-surface backdrop-blur-md rounded-2xl shadow-glass border border-prism-border flex items-center justify-center">
                    <Sparkles size={32} className="text-prism-primary/80" />
                 </div>
            </div>
        </div>

        {/* Localized Greeting V2 */}
        <h2 className="text-3xl font-bold mb-2">
            <span className="bg-gradient-to-r from-prism-primary to-prism-secondary bg-clip-text text-transparent">
                下午好, Frank
            </span>
        </h2>
        <p className="text-prism-secondary mb-10 font-medium tracking-wide">
            我是您的销售助手
        </p>

        {/* Quick Actions (Optional, brief didn't strictly ban them but focused on Input) */}
        {/* Keeping them for prototype usability if list empty */}
    </motion.div>
);

interface HomeScreenProps {
    onNavigate: (page: string) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({ onNavigate }) => {
  // Mock Data
  // Mock Data (Empty for Clean Desk Paradigm)
  const sessions: Session[] = [
      // { id: '1', date: '2026-01-27', time: '14:30', title: '客户 Q4 预算会谈摘要', summary: '财务部需要我们在下周一前提供新的报价方案。重点关注 A3 型号的售后条款。', type: 'coach', isPinned: true },
      // { id: '2', date: '2026-01-26', title: '灵感：竞品分析策略', summary: '需要重点关注 A 公司在华东市场的价格变动。他们的促销力度很大。', type: 'analyst' },
      // { id: '3', date: '2026-01-20', title: '张总初次拜访记录', summary: '客户对 A3 打印机的兴趣度较高，但担心售后服务响应速度。建议安排一次技术演示。', type: 'coach' },
  ];

  const groups = groupSessions(sessions);
  const isEmpty = sessions.length === 0; // Toggle this to test empty state

  return (
    <div className="flex flex-col h-full relative z-10">
        
        {/* Header (Mimic contract) */}
        {/* Note: App.tsx has a unified header, but we might want a specific one or rely on App's. 
            For now, let's use the scroll area content. 
            The App Header is fixed. We render content below it.
        */}

        <div className="flex-1 overflow-y-auto p-4 space-y-2 no-scrollbar pb-32">
            
            {isEmpty ? (
                <HomeHero />
            ) : (
                <motion.div 
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.4 }}
                    className="space-y-6 pt-2"
                >
                    {/* Today */}
                    {groups.today.length > 0 && (
                        <section>
                            <SectionHeader title="Today" icon={<Clock size={12} />} />
                            <div className="space-y-3">
                                {groups.today.map(s => (
                                    <SessionCard key={s.id} session={s} onClick={() => onNavigate('Coach')} />
                                ))}
                            </div>
                        </section>
                    )}

                    {/* Yesterday */}
                    {groups.yesterday.length > 0 && (
                        <section>
                            <SectionHeader title="Yesterday" icon={<Calendar size={12} />} />
                            <div className="space-y-3">
                                {groups.yesterday.map(s => (
                                    <SessionCard key={s.id} session={s} onClick={() => onNavigate('Analyst')} />
                                ))}
                            </div>
                        </section>
                    )}

                    {/* Older */}
                    {groups.older.length > 0 && (
                        <section>
                            <SectionHeader title="History" icon={<MoreHorizontal size={12} />} />
                            <div className="space-y-3">
                                {groups.older.map(s => (
                                    <SessionCard key={s.id} session={s} onClick={() => onNavigate('Coach')} />
                                ))}
                            </div>
                        </section>
                    )}
                </motion.div>
            )}
        </div>
    </div>
  );
};
