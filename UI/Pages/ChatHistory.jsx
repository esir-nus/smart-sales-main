import React, { useState } from 'react';
import { ChevronLeft, Search, Edit2, Trash2, Pin } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
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
    // Navigate to chat detail
    console.log("Navigate to chat", item.id);
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
 