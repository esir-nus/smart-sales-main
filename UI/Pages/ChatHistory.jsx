import React, { useState } from 'react';
import { ChevronLeft, Search, Edit2, Trash2, Pin } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { createPageUrl } from '@/utils';
import { motion, AnimatePresence } from 'framer-motion';
import HistoryItem from '@/components/chat/HistoryItem';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";

export default function ChatHistoryPage() {
  const navigate = useNavigate();
  const [selectedItem, setSelectedItem] = useState(null);
  const [showMenu, setShowMenu] = useState(false);
  const [showRenameDialog, setShowRenameDialog] = useState(false);
  const [newName, setNewName] = useState("");

  // Using state for data to allow modifications
  const [historyGroups, setHistoryGroups] = useState([
    {
      group: "7天内",
      items: [
        { id: 1, date: "02-15", title: "张经理 产品报价方案", isPinned: false },
        { id: 2, date: "02-14", title: "李总 跟进邮件草稿", isPinned: false },
        { id: 3, date: "02-12", title: "王小姐 竞品分析", isPinned: false }
      ]
    },
    {
      group: "30天内",
      items: [
        { id: 4, date: "01-28", title: "赵总 新季度目标", isPinned: false },
        { id: 5, date: "01-20", title: "陈经理 培训材料", isPinned: false }
      ]
    },
    {
      group: "2025年1月",
      items: [
        { id: 6, date: "01-10", title: "刘总 合同细节确认", isPinned: false }
      ]
    }
  ]);

  const handleLongPress = (item) => {
    setSelectedItem(item);
    setShowMenu(true);
  };

  const handleItemClick = (item) => {
    // Navigate to chat detail (Home page acts as Chat)
    navigate(createPageUrl('Home') + `?sessionId=${item.id}`);
  };

  const handleDelete = () => {
    if (!selectedItem) return;
    
    setHistoryGroups(prevGroups => 
      prevGroups.map(group => ({
        ...group,
        items: group.items.filter(item => item.id !== selectedItem.id)
      })).filter(group => group.items.length > 0)
    );
    setShowMenu(false);
    setSelectedItem(null);
  };

  const handlePin = () => {
    if (!selectedItem) return;

    setHistoryGroups(prevGroups => {
      // Toggle pin status
      const newGroups = prevGroups.map(group => ({
        ...group,
        items: group.items.map(item => 
          item.id === selectedItem.id 
            ? { ...item, isPinned: !item.isPinned }
            : item
        )
      }));

      // Sort items: pinned first
      newGroups.forEach(group => {
        group.items.sort((a, b) => (b.isPinned === a.isPinned) ? 0 : b.isPinned ? 1 : -1);
      });
      
      return newGroups;
    });

    setShowMenu(false);
    setSelectedItem(null);
  };

  const openRenameDialog = () => {
    setNewName(selectedItem.title);
    setShowMenu(false);
    setShowRenameDialog(true);
  };

  const handleRename = () => {
    if (!selectedItem || !newName.trim()) return;

    setHistoryGroups(prevGroups => 
      prevGroups.map(group => ({
        ...group,
        items: group.items.map(item => 
          item.id === selectedItem.id 
            ? { ...item, title: newName }
            : item
        )
      }))
    );
    setShowRenameDialog(false);
    setSelectedItem(null);
  };

  return (
    <div className="min-h-screen bg-[#F2F2F7] text-black pb-10 font-sans relative">
      {/* Custom Header for this page */}
      <div className="sticky top-0 z-30 bg-[#F2F2F7] flex items-center justify-between px-4 py-6 border-b border-[#E5E5EA]">
        <button 
          onClick={() => navigate(-1)}
          className="p-2 -ml-2 text-black hover:text-gray-600"
        >
          <ChevronLeft size={28} />
        </button>
        <h1 className="text-lg font-bold">聊天记录</h1>
        <button className="p-2 -mr-2 text-black hover:text-gray-600">
          <Search size={24} />
        </button>
      </div>

      <div className="px-4 mt-4">
        {historyGroups.map((group, groupIdx) => (
          <div key={groupIdx} className="mb-8">
            <h2 className="text-[#8E8E93] text-sm mb-3 pl-1">{group.group}</h2>
            <div className="space-y-3">
              {group.items.map((item) => (
                <HistoryItem 
                  key={item.id}
                  item={item}
                  onClick={handleItemClick}
                  onLongPress={handleLongPress}
                />
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Context Menu Bottom Sheet */}
      <AnimatePresence>
        {showMenu && (
          <>
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowMenu(false)}
              className="fixed inset-0 bg-black/40 z-40"
            />
            <motion.div
              initial={{ y: "100%" }}
              animate={{ y: 0 }}
              exit={{ y: "100%" }}
              transition={{ type: "spring", damping: 25, stiffness: 200 }}
              className="fixed bottom-0 left-0 right-0 bg-[#F2F2F7] rounded-t-2xl z-50 p-4 pb-8 shadow-2xl"
            >
              <div className="w-12 h-1 bg-[#C7C7CC] rounded-full mx-auto mb-6" />
              <div className="space-y-2">
                 <button 
                   onClick={handlePin}
                   className="w-full bg-white p-4 rounded-xl font-medium text-black active:bg-[#E5E5EA] flex items-center gap-3"
                 >
                   <Pin className="w-5 h-5 text-[#007AFF]" />
                   {selectedItem?.isPinned ? "取消置顶" : "置顶"}
                 </button>
                 <button 
                   onClick={openRenameDialog}
                   className="w-full bg-white p-4 rounded-xl font-medium text-black active:bg-[#E5E5EA] flex items-center gap-3"
                 >
                   <Edit2 className="w-5 h-5 text-[#007AFF]" />
                   重命名
                 </button>
                 <button 
                   onClick={handleDelete}
                   className="w-full bg-white p-4 rounded-xl font-medium text-red-500 active:bg-[#E5E5EA] flex items-center gap-3"
                 >
                   <Trash2 className="w-5 h-5" />
                   删除
                 </button>
              </div>
              <button 
                onClick={() => setShowMenu(false)}
                className="w-full mt-4 p-4 rounded-xl font-bold text-black bg-transparent"
              >
                取消
              </button>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Rename Dialog */}
      <Dialog open={showRenameDialog} onOpenChange={setShowRenameDialog}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>重命名</DialogTitle>
          </DialogHeader>
          <div className="py-4">
            <Input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="输入新标题"
              className="col-span-3"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowRenameDialog(false)}>取消</Button>
            <Button onClick={handleRename} className="bg-[#007AFF]">保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  );
}