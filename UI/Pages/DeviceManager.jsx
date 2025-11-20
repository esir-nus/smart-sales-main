import React, { useState, useEffect } from 'react';
import { base44 } from '@/api/base44Client';
import { Wifi, RefreshCw, Upload, Trash2, CheckCircle, Smartphone, Image as ImageIcon, Video } from 'lucide-react';
import NeumorphicCard from '@/components/ui/NeumorphicCard';
import NeumorphicButton from '@/components/ui/NeumorphicButton';
import { motion } from 'framer-motion';
import DeviceFileItem from '@/components/device/DeviceFileItem';

export default function DeviceManagerPage() {
  // States: disconnected, connecting, connected
  const [connectionState, setConnectionState] = useState('disconnected');
  const [files, setFiles] = useState([]);
  const [activeTab, setActiveTab] = useState('image');
  const [scanning, setScanning] = useState(false);

  useEffect(() => {
    // Simulate fetching files when connected
    if (connectionState === 'connected') {
      loadFiles();
    }
  }, [connectionState]);

  const loadFiles = async () => {
    try {
      const data = await base44.entities.GadgetFile.list();
      setFiles(data);
    } catch (e) {
      console.error("Failed to load files", e);
      // Fallback mock data if entity fails
      setFiles([
        { id: 1, filename: 'Promo_Poster.png', type: 'image', url: 'https://images.unsplash.com/photo-1557804506-669a67965ba0?w=800&q=80', is_applied: true },
        { id: 2, filename: 'Sales_Chart.png', type: 'image', url: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&q=80', is_applied: false }
      ]);
    }
  };

  const handleConnect = () => {
    setScanning(true);
    setConnectionState('connecting');
    
    // Simulate scanning and connection delay
    setTimeout(() => {
      setScanning(false);
      setConnectionState('connected');
    }, 3000);
  };

  const handleApply = async (file) => {
    // Optimistic update
    const updatedFiles = files.map(f => ({
      ...f,
      is_applied: f.id === file.id
    }));
    setFiles(updatedFiles);
    
    // In real app: await base44.entities.GadgetFile.update(file.id, { is_applied: true });
    // and unset others
  };

  const handleDelete = async (id) => {
    setFiles(files.filter(f => f.id !== id));
    // await base44.entities.GadgetFile.delete(id);
  };

  const filteredFiles = files.filter(f => f.type === activeTab);

  return (
    <div className="h-full flex flex-col p-4 max-w-2xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-black">设备管理</h1>
        <p className="text-[#8E8E93]">管理您的销售助手设备</p>
      </div>

      {/* Connection Status Card */}
      <NeumorphicCard className="mb-8 flex flex-col items-center justify-center py-8 min-h-[200px] bg-white border border-[#E5E5EA] shadow-sm rounded-3xl">
        {connectionState === 'disconnected' && (
          <motion.div 
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="text-center"
          >
            <div className="w-20 h-20 bg-[#F2F2F7] border border-[#E5E5EA] rounded-2xl flex items-center justify-center mx-auto mb-4">
              <Smartphone className="w-10 h-10 text-[#8E8E93]" />
            </div>
            <h3 className="text-lg font-bold text-black mb-2">设备已断开</h3>
            <p className="text-[#8E8E93] text-sm mb-6 max-w-xs mx-auto">
              请确保您的设备已开启并处于连接范围内。
            </p>
            <NeumorphicButton onClick={handleConnect} variant="primary" className="px-8 py-3 bg-[#007AFF] hover:bg-[#0066CC] text-white">
              连接设备
            </NeumorphicButton>
          </motion.div>
        )}

        {connectionState === 'connecting' && (
          <div className="text-center">
            <div className="relative w-24 h-24 mx-auto mb-6">
              <motion.div 
                animate={{ scale: [1, 1.5, 1], opacity: [1, 0, 1] }}
                transition={{ duration: 2, repeat: Infinity }}
                className="absolute inset-0 bg-[#007AFF]/20 rounded-full"
              />
              <div className="absolute inset-0 flex items-center justify-center">
                <Wifi className="w-10 h-10 text-[#007AFF]" />
              </div>
            </div>
            <h3 className="text-lg font-bold text-black">正在扫描设备...</h3>
            <p className="text-[#8E8E93] text-sm mt-2">请稍候，正在建立连接</p>
          </div>
        )}

        {connectionState === 'connected' && (
          <div className="w-full px-4">
             <div className="flex items-center justify-between mb-6">
               <div className="flex items-center gap-3">
                 <div className="p-3 bg-[#34C759]/10 rounded-full text-[#34C759]">
                   <CheckCircle className="w-6 h-6" />
                 </div>
                 <div>
                   <h3 className="font-bold text-black">销售助手 X1</h3>
                   <p className="text-xs text-[#34C759]">已连接 • 电量 85%</p>
                 </div>
                 </div>
                 <NeumorphicButton 
                 onClick={() => setConnectionState('disconnected')}
                 className="p-2 text-[#8E8E93] hover:bg-[#F2F2F7]"
                 >
                 <RefreshCw className="w-5 h-5" />
                 </NeumorphicButton>
             </div>

             {/* Device Screen Preview (Placeholder) */}
             <div className="bg-[#3A3A3C] rounded-xl aspect-video w-full mb-6 relative overflow-hidden shadow-inner flex items-center justify-center border border-[#E5E5EA]">
                {files.find(f => f.is_applied) ? (
                  <img 
                    src={files.find(f => f.is_applied).url} 
                    alt="Current Display" 
                    className="w-full h-full object-cover opacity-90"
                  />
                ) : (
                  <span className="text-white/50 text-sm">默认显示</span>
                )}
                <div className="absolute bottom-2 right-2 text-[10px] text-white/50 font-mono">实时预览</div>
             </div>
          </div>
        )}
      </NeumorphicCard>

      {/* File Manager Section (Only when connected) */}
      {connectionState === 'connected' && (
        <div className="flex-1">
           <div className="flex gap-4 mb-6">
             <button 
               onClick={() => setActiveTab('image')}
               className={`flex-1 py-3 rounded-xl text-sm font-bold transition-all ${
                 activeTab === 'image' 
                   ? 'bg-[#007AFF] text-white shadow-md' 
                   : 'text-[#8E8E93] bg-white border border-[#E5E5EA] hover:bg-[#F2F2F7]'
               }`}
             >
               <div className="flex items-center justify-center gap-2">
                 <ImageIcon size={16} /> 图片
               </div>
             </button>
             <button 
               onClick={() => setActiveTab('video')}
               className={`flex-1 py-3 rounded-xl text-sm font-bold transition-all ${
                 activeTab === 'video' 
                   ? 'bg-[#007AFF] text-white shadow-md' 
                   : 'text-[#8E8E93] bg-white border border-[#E5E5EA] hover:bg-[#F2F2F7]'
               }`}
             >
               <div className="flex items-center justify-center gap-2">
                 <Video size={16} /> 视频
               </div>
             </button>
           </div>

           <div className="grid grid-cols-2 gap-4 pb-20">
              {/* Upload New Card */}
              <div className="neu-card rounded-2xl flex flex-col items-center justify-center p-6 border border-dashed border-[#C7C7CC] cursor-pointer hover:border-[#007AFF] transition-colors bg-[#F2F2F7]">
                <Upload className="w-8 h-8 text-[#8E8E93] mb-2" />
                <span className="text-xs text-[#8E8E93] font-bold">上传新文件</span>
              </div>

              {filteredFiles.map(file => (
                <DeviceFileItem 
                  key={file.id}
                  file={file}
                  onApply={handleApply}
                  onDelete={handleDelete}
                />
              ))}
           </div>
        </div>
      )}
    </div>
  );
}