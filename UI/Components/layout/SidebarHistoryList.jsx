import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createPageUrl } from '@/utils';
import { Trash2, Edit2, Pin } from 'lucide-react';
import { 
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuTrigger,
  ContextMenuSeparator,
} from "@/components/ui/context-menu";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export default function SidebarHistoryList({ onItemClick }) {
  const navigate = useNavigate();
  const [renamingItem, setRenamingItem] = useState(null);
  const [newName, setNewName] = useState("");
  const [isRenameOpen, setIsRenameOpen] = useState(false);

  const [historyData, setHistoryData] = useState([
    {
      group: "7天内",
      items: [
        { id: 1, date: "02-15", person: "张经理", theme: "产品报价方案", isPinned: false },
        { id: 2, date: "02-14", person: "李总", theme: "跟进邮件草稿", isPinned: false },
        { id: 3, date: "02-12", person: "王小姐", theme: "竞品分析", isPinned: false },
        { id: 4, date: "02-11", person: "采购部", theme: "询价单确认", isPinned: false }
      ]
    },
    {
      group: "30天内",
      items: [
        { id: 5, date: "01-28", person: "赵总", theme: "新季度目标设定", isPinned: false },
        { id: 6, date: "01-20", person: "陈经理", theme: "培训材料审核", isPinned: false },
        { id: 7, date: "01-15", person: "刘工", theme: "对接产品定制", isPinned: false },
        { id: 8, date: "01-12", person: "公司例会", theme: "新品发布计划", isPinned: false }
      ]
    },
    {
      group: "2025年1月",
      items: [
        { id: 9, date: "01-10", person: "刘总", theme: "合同细节确认", isPinned: false },
        { id: 10, date: "01-05", person: "HR", theme: "招聘需求讨论", isPinned: false }
      ]
    }
  ]);

  const handleDelete = (itemId) => {
    const newData = historyData.map(group => ({
      ...group,
      items: group.items.filter(item => item.id !== itemId)
    })).filter(group => group.items.length > 0);
    setHistoryData(newData);
  };

  const handlePin = (itemId) => {
    const newData = historyData.map(group => ({
      ...group,
      items: group.items.map(item => 
        item.id === itemId ? { ...item, isPinned: !item.isPinned } : item
      ).sort((a, b) => (b.isPinned === a.isPinned ? 0 : b.isPinned ? 1 : -1))
    }));
    setHistoryData(newData);
  };

  const openRenameDialog = (item) => {
    setRenamingItem(item);
    setNewName(item.theme);
    setIsRenameOpen(true);
  };

  const handleRename = () => {
    if (!renamingItem) return;
    const newData = historyData.map(group => ({
      ...group,
      items: group.items.map(item => 
        item.id === renamingItem.id ? { ...item, theme: newName } : item
      )
    }));
    setHistoryData(newData);
    setIsRenameOpen(false);
    setRenamingItem(null);
  };

  const handleItemClick = (item) => {
    if (onItemClick) onItemClick();
    navigate(createPageUrl('Home'));
  };

  return (
    <div className="flex-1 overflow-y-auto py-2">
      {historyData.map((group, groupIdx) => (
        <div key={groupIdx} className="mb-6">
          <div className="sticky top-0 bg-[#F2F2F7] z-10 px-2 py-2 mb-1 border-b border-[#E5E5EA]/50">
            <h2 className="text-[#8E8E93] text-sm font-medium">{group.group}</h2>
          </div>
          <div className="space-y-1 px-2">
            {group.items.map((item) => (
              <ContextMenu key={item.id}>
                <ContextMenuTrigger>
                  <div 
                    onClick={() => handleItemClick(item)}
                    className={`py-3 px-2 rounded-lg active:bg-[#E5E5EA] transition-colors cursor-pointer relative ${item.isPinned ? 'bg-[#F2F2F7]/50' : ''}`}
                  >
                    {item.isPinned && (
                      <Pin size={12} className="absolute top-1 right-1 text-[#007AFF] fill-current" />
                    )}
                    <div className="text-black text-[15px] leading-snug line-clamp-2">
                      <span className="font-mono text-[#8E8E93] mr-2 text-xs">{item.date}</span>
                      <span className="font-bold text-[#333] mr-2">{item.person}</span>
                      <span className="text-[#666]">{item.theme}</span>
                    </div>
                  </div>
                </ContextMenuTrigger>
                <ContextMenuContent className="w-40 bg-white border-[#E5E5EA] text-black shadow-lg rounded-xl">
                  <ContextMenuItem 
                    className="focus:bg-[#F2F2F7] flex items-center gap-2 cursor-pointer py-2"
                    onClick={() => handlePin(item.id)}
                  >
                    <Pin size={14} className={item.isPinned ? "fill-[#007AFF] text-[#007AFF]" : ""} />
                    {item.isPinned ? "取消置顶" : "置顶"}
                  </ContextMenuItem>
                  <ContextMenuItem 
                    className="focus:bg-[#F2F2F7] flex items-center gap-2 cursor-pointer py-2"
                    onClick={() => openRenameDialog(item)}
                  >
                    <Edit2 size={14} />
                    重命名
                  </ContextMenuItem>
                  <ContextMenuSeparator className="bg-[#E5E5EA]" />
                  <ContextMenuItem 
                    className="focus:bg-[#F2F2F7] focus:text-red-500 text-red-500 flex items-center gap-2 cursor-pointer py-2"
                    onClick={() => handleDelete(item.id)}
                  >
                    <Trash2 size={14} />
                    删除
                  </ContextMenuItem>
                </ContextMenuContent>
              </ContextMenu>
            ))}
          </div>
        </div>
      ))}

      <Dialog open={isRenameOpen} onOpenChange={setIsRenameOpen}>
        <DialogContent className="sm:max-w-[425px] bg-white text-black">
          <DialogHeader>
            <DialogTitle>重命名</DialogTitle>
          </DialogHeader>
          <div className="py-4">
            <Input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="输入新主题"
              className="col-span-3 bg-[#F2F2F7] border-transparent"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsRenameOpen(false)}>取消</Button>
            <Button onClick={handleRename} className="bg-[#007AFF] text-white hover:bg-[#0066CC]">保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}