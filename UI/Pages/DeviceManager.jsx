import React, { useState, useEffect } from 'react';
import { base44 } from '@/api/base44Client';
import { Wifi, RefreshCw, Upload, Smartphone, CheckCircle, Loader2, AlertCircle, Play, Pause, Maximize2 } from 'lucide-react';
import NeumorphicCard from '@/components/ui/NeumorphicCard';
import NeumorphicButton from '@/components/ui/NeumorphicButton';
import { motion, AnimatePresence } from 'framer-motion';
import DeviceFileItem from '@/components/device/DeviceFileItem';
import { Link } from 'react-router-dom';
import { createPageUrl } from '@/utils';

export default function DeviceManagerPage() {
  // Start with 'connecting' to simulate auto-discovery on mount
  const [connectionState, setConnectionState] = useState('connecting'); 
  const [files, setFiles] = useState([]);
  const [isLoadingFiles, setIsLoadingFiles] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false); 

  // Auto-connect simulation on mount
  useEffect(() => {
    const timer = setTimeout(() => {
      setConnectionState('connected');
    }, 1500);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (connectionState === 'connected') {
      loadFiles();
    } else {
      setFiles([]);
      setSelectedFile(null);
    }
  }, [connectionState]);

  const loadFiles = async () => {
    setIsLoadingFiles(true);
    try {
      // Simulate API delay
      await new Promise(r => setTimeout(r, 800));
      const data = await base44.entities.GadgetFile.list();
      
      // Mock data if list is empty or for dev
      const mockData = [
        { id: 1, filename: 'Promo_Summer_2025.png', type: 'image', url: 'https://images.unsplash.com/photo-1557804506-669a67965ba0?w=800&q=80', is_applied: true },
        { id: 2, filename: 'Product_Demo.mp4', type: 'video', duration: '00:45', url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4', is_applied: false },
        { id: 3, filename: 'Feature_Highlight.gif', type: 'gif', url: 'https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExcDdtY2J6eG52YnJ6YnJ6YnJ6YnJ6YnJ6YnJ6YnJ6YnJ6YnJ6YnJ6/3o7TKSjRrfIPjeiB56/giphy.gif', is_applied: false },
        { id: 4, filename: 'Store_Front.jpg', type: 'image', url: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800&q=80', is_applied: false }
      ];
      
      // Merge real data if exists, otherwise use mock
      const finalData = (data && data.length > 0) ? data : mockData;
      setFiles(finalData);
      
      // Auto-select the currently applied file, or the first one
      const applied = finalData.find(f => f.is_applied);
      setSelectedFile(applied || finalData[0] || null);

    } catch (e) {
      console.error("Failed to load files", e);
      // Error state handled by UI below if files is empty
    } finally {
      setIsLoadingFiles(false);
    }
  };

  const handleConnect = () => {
    setConnectionState('connecting');
    setTimeout(() => {
      setConnectionState('connected');
    }, 2500);
  };

  const handleRefresh = () => {
    loadFiles();
  };

  const handleApply = async (file) => {
    // Update local state to reflect change
    const updatedFiles = files.map(f => ({
      ...f,
      is_applied: f.id === file.id
    }));
    setFiles(updatedFiles);
    setSelectedFile(file); // Also select it
    // In real app: await base44.entities.GadgetFile.update(...)
  };

  const handleDelete = async (id) => {
    setFiles(files.filter(f => f.id !== id));
    if (selectedFile?.id === id) {
        setSelectedFile(null);
    }
  };

  return (
    <div className="h-full flex flex-col max-w-2xl mx-auto relative bg-[#F2F2F7]">
      <div className="flex-1 overflow-y-auto px-4 pt-6 pb-24 scrollbar-hide">
        
        {/* Header */}
        <div className="flex justify-between items-start mb-6">
          <div>
            <h1 className="text-2xl font-bold text-black">设备管理</h1>
            <p className="text-[#8E8E93] text-sm">管理您的销售助手设备</p>
          </div>
          {connectionState === 'connected' && (
             <button 
               onClick={handleRefresh}
               disabled={isLoadingFiles}
               className={`p-2 rounded-full bg-white border border-[#E5E5EA] text-[#8E8E93] hover:text-[#007AFF] active:scale-95 shadow-sm transition-all duration-200 ${isLoadingFiles ? 'animate-spin' : ''}`}
               aria-label="刷新文件列表"
             >
               <RefreshCw size={20} />
             </button>
          )}
        </div>

        {/* State: Disconnected */}
        {connectionState === 'disconnected' && (
           <NeumorphicCard className="flex flex-col items-center justify-center py-12 bg-white border border-[#E5E5EA] shadow-md rounded-3xl mt-4">
              <div className="w-24 h-24 bg-[#F2F2F7] rounded-full flex items-center justify-center mb-6 text-[#8E8E93] shadow-inner">
                 <Smartphone size={40} strokeWidth={1.5} />
              </div>
              <h3 className="text-lg font-bold text-black mb-2">设备未连接</h3>
              <p className="text-[#8E8E93] text-sm mb-8 max-w-[220px] text-center leading-relaxed">
                请连接设备以管理文件和查看实时预览
              </p>
              <div className="flex flex-col gap-3 w-full max-w-xs px-4">
                <NeumorphicButton 
                  onClick={() => setConnectionState('connecting')}
                  variant="primary" 
                  className="bg-[#007AFF] hover:opacity-90 active:scale-[0.98] text-white px-8 py-3 shadow-lg w-full transition-all duration-200"
                >
                   重试连接
                </NeumorphicButton>

                <Link to={createPageUrl('DeviceSetup')} className="w-full">
                  <NeumorphicButton className="bg-transparent border border-[#E5E5EA] text-[#8E8E93] px-8 py-3 w-full hover:bg-[#F2F2F7] active:bg-[#E5E5EA] transition-all duration-200">
                     配对新设备
                  </NeumorphicButton>
                </Link>
              </div>
           </NeumorphicCard>
        )}

        {/* State: Connecting */}
        {connectionState === 'connecting' && (
           <div className="flex flex-col items-center justify-center py-20 mt-4">
              <Loader2 className="w-10 h-10 text-[#007AFF] animate-spin mb-4" />
              <h3 className="text-lg font-medium text-black">正在连接设备...</h3>
              <p className="text-sm text-[#8E8E93] mt-1">请确保设备在附近</p>
           </div>
        )}

        {/* State: Connected */}
        {connectionState === 'connected' && (
           <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
              
              {/* Persistent Simulator Viewer */}
              <div className="bg-[#3A3A3C] rounded-2xl overflow-hidden shadow-xl border border-[#E5E5EA] relative aspect-video group">
                 {selectedFile ? (
                    <div className="w-full h-full relative flex items-center justify-center bg-black">
                       {selectedFile.type === 'video' ? (
                          <video 
                             src={selectedFile.url} 
                             className="w-full h-full object-contain"
                             controls
                             autoPlay={false}
                          />
                       ) : (
                          <img 
                             src={selectedFile.url} 
                             alt="Preview" 
                             className="w-full h-full object-contain"
                          />
                       )}
                       
                       {/* Overlay Info */}
                       <div className="absolute top-0 left-0 right-0 p-3 bg-gradient-to-b from-black/60 to-transparent flex justify-between items-start">
                          <div className="text-white text-xs font-medium drop-shadow-md">
                             <span className="opacity-70">预览:</span> {selectedFile.filename}
                          </div>
                          {selectedFile.is_applied && (
                             <span className="bg-[#34C759] text-white text-[10px] px-2 py-0.5 rounded-full font-bold shadow-sm flex items-center gap-1">
                                <CheckCircle size={10} /> 当前展示
                             </span>
                          )}
                       </div>
                    </div>
                 ) : (
                    <div className="w-full h-full flex flex-col items-center justify-center text-white/30">
                       <Smartphone size={48} strokeWidth={1} className="mb-2 opacity-50" />
                       <span className="text-sm">选择文件预览</span>
                    </div>
                 )}
                 
                 {/* Simulator Label */}
                 <div className="absolute bottom-3 right-3 text-[10px] text-white/40 font-mono uppercase tracking-wider pointer-events-none">
                    Device Simulator
                 </div>
              </div>

              {/* Unified Media Grid */}
              <div>
                 <h3 className="text-sm font-bold text-[#8E8E93] uppercase tracking-wider mb-3 px-1">
                    文件列表 ({files.length})
                 </h3>
                 
                 {isLoadingFiles ? (
                    <div className="grid grid-cols-2 gap-3">
                       {[1,2,3,4].map(i => (
                          <div key={i} className="aspect-[4/3] bg-white rounded-2xl animate-pulse" />
                       ))}
                    </div>
                 ) : (
                    <div className="grid grid-cols-2 gap-3">
                       {/* Upload Tile */}
                       <div className="bg-white rounded-2xl border border-dashed border-[#C7C7CC] hover:border-[#007AFF] hover:bg-blue-50/30 active:scale-[0.98] transition-all duration-200 cursor-pointer flex flex-col items-center justify-center gap-2 min-h-[140px] group">
                          <div className="w-10 h-10 rounded-full bg-[#F2F2F7] group-hover:bg-white group-hover:shadow-md transition-all duration-200 flex items-center justify-center text-[#007AFF]">
                             <Upload size={20} />
                          </div>
                          <span className="text-xs font-bold text-[#8E8E93] group-hover:text-[#007AFF] transition-colors duration-200">上传新文件</span>
                       </div>

                       {/* File Items */}
                       {files.map(file => (
                          <DeviceFileItem 
                             key={file.id}
                             file={file}
                             isSelected={selectedFile?.id === file.id}
                             onSelect={setSelectedFile}
                             onApply={handleApply}
                             onDelete={handleDelete}
                          />
                       ))}
                    </div>
                 )}

                 {!isLoadingFiles && files.length === 0 && (
                    <div className="text-center py-10 text-[#8E8E93] text-sm bg-white rounded-2xl border border-[#E5E5EA] mt-3">
                       暂无文件，请上传。
                    </div>
                 )}
              </div>
           </div>
        )}
      </div>
    </div>
  );
}