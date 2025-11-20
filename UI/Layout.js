import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Menu, User } from 'lucide-react';
import { createPageUrl } from '@/utils';
import { motion, AnimatePresence } from 'framer-motion';
import DeviceStatusCard from './components/layout/DeviceStatusCard';
import SidebarHistoryList from './components/layout/SidebarHistoryList';

export default function Layout({ children, currentPageName }) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const location = useLocation();

  const toggleSidebar = () => setIsSidebarOpen(!isSidebarOpen);

  const getPageTitle = (name) => {
    switch(name) {
      case 'Home': return '新对话';
      case 'UserCenter': return '用户中心';
      case 'DeviceManager': return '设备管理';
      case 'DeviceSetup': return '设备初始化';
      case 'AudioFiles': return '录音文件';
      case 'ChatHistory': return '聊天记录';
      default: return name;
    }
  };

  return (
    <div className="min-h-screen bg-[#F2F2F7] text-black font-sans relative overflow-hidden">
      {/* Mobile Header */}
      <header className="flex items-center justify-between px-4 py-6 fixed top-0 left-0 right-0 z-40 bg-[#F2F2F7]/90 backdrop-blur-sm border-b border-[#E5E5EA]">
        <button 
          onClick={toggleSidebar}
          className="p-2 rounded-lg hover:bg-[#E5E5EA] transition-colors"
        >
          <Menu className="w-6 h-6 text-black" />
        </button>
        <h1 className="text-lg font-bold text-black">
          {getPageTitle(currentPageName)}
        </h1>
        <Link to={createPageUrl('UserCenter')}>
          <button className="p-2 rounded-full neu-btn hover:text-[#007AFF] bg-white shadow-sm">
            <User className="w-5 h-5" />
          </button>
        </Link>
      </header>

      {/* Sidebar / Drawer */}
      <AnimatePresence>
        {isSidebarOpen && (
          <>
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={toggleSidebar}
              className="fixed inset-0 bg-black/20 z-40 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: "spring", bounce: 0, duration: 0.3 }}
              className="fixed top-0 left-0 bottom-0 w-[85%] max-w-sm bg-[#F2F2F7] text-black z-50 shadow-2xl flex flex-col border-r border-[#E5E5EA]"
            >
              {/* Top: Device Status */}
              <div className="p-4 pt-6 bg-[#F2F2F7] border-b border-[#E5E5EA] shadow-sm z-20">
                <DeviceStatusCard status="connected" />
              </div>
              
              {/* Middle: History List */}
              <div className="flex-1 overflow-hidden flex flex-col bg-[#F2F2F7]">
                <SidebarHistoryList onItemClick={toggleSidebar} />
              </div>

              {/* Bottom: User Profile Link */}
              <div className="p-4 border-t border-[#E5E5EA] bg-[#F2F2F7] z-20">
                 <Link to={createPageUrl('UserCenter')} onClick={toggleSidebar}>
                    <div className="flex items-center gap-3 p-2 rounded-xl hover:bg-[#E5E5EA] transition-colors">
                      <div className="w-10 h-10 rounded-full bg-white border border-[#E5E5EA] flex items-center justify-center shadow-sm">
                        <User className="w-5 h-5 text-[#8E8E93]" />
                      </div>
                      <div>
                         <div className="font-medium text-black text-sm">用户资料</div>
                         <div className="text-xs text-[#8E8E93]">账号设置</div>
                      </div>
                    </div>
                 </Link>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Main Content Area */}
      <main className="pt-24 pb-6 px-4 min-h-screen">
        {children}
      </main>
    </div>
  );
}