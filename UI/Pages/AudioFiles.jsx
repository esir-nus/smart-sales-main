import React, { useState, useEffect } from 'react';
import { base44 } from '@/api/base44Client';
import { Play, Pause, Download, Clock, MapPin, Check, RefreshCw, MessageSquare, Loader2, AlertCircle, FileText, ChevronUp, Smartphone, Cloud } from 'lucide-react';
import { createPageUrl } from '@/utils';
import { Link, useNavigate } from 'react-router-dom';
import NeumorphicCard from '@/components/ui/NeumorphicCard';
import { motion, AnimatePresence } from 'framer-motion';
import AudioDrawer from '@/components/audio/AudioDrawer';

// Mock transcript content
const MOCK_TRANSCRIPT = `**A:** 您好，张经理，我是 SmartSales 的销售小李。

**B:** 你好小李。

**A:** 这次主要是想跟您确认一下之前发给您的报价方案，不知道您那边看得怎么样了？

**B:** 方案我看了，整体还可以，就是价格方面我觉得还是有点偏高。

**A:** 我理解您的顾虑。其实我们的定价是包含了全套售后服务和两年的延保的。如果扣除这部分价值，其实非常有竞争力。

**B:** 嗯... 这个我知道，但是竞品那边给的价格确实低不少。

**A:** 明白。除了价格，您对我们的功能模块满足度如何？

**B:** 功能倒是挺全的，特别是那个自动报表功能，我很喜欢。

**A:** 那太好了。如果我们在付款方式上给您申请一些灵活的账期，您看是否能推动一下？

**B:** 如果有账期的话，倒是可以考虑。你先发个具体的条款给我看看吧。

**A:** 好的，没问题，我马上整理发给您。`;

export default function AudioFilesPage() {
  const [recordings, setRecords] = useState([]);
  const [isSyncing, setIsSyncing] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadRecordings();
  }, []);

  const loadRecordings = async () => {
    try {
      // Simulate existing data
      // States: NotSynced, Syncing, Synced, Error
      // Trans States: None, Uploading, Transcribing, Ready, Error
      const mockData = [
        { 
           id: 1, 
           filename: '20251030_1541_Meeting.wav', 
           date: '2025-10-30T15:41:00', 
           duration: '45:20', 
           location: '福田区',
           source: 'cloud', 
           sync_status: 'Synced', 
           transcription_status: 'Ready',
           transcript: MOCK_TRANSCRIPT
        },
        { 
           id: 2, 
           filename: '20251230_2221_Call.wav', 
           date: '2025-12-30T22:21:00', 
           duration: '12:05', 
           location: '南山区',
           source: 'phone', 
           sync_status: 'NotSynced', 
           transcription_status: 'None',
           transcript: null
        },
        { 
           id: 3, 
           filename: '20251230_1121_Demo.wav', 
           date: '2025-12-30T11:21:00', 
           duration: '05:42', 
           location: '南山区',
           source: 'cloud', 
           sync_status: 'Synced', 
           transcription_status: 'Transcribing',
           transcript: null
        },
        { 
           id: 4, 
           filename: '20250101_0900_Brief.wav', 
           date: '2025-01-01T09:00:00', 
           duration: '20:15', 
           location: '宝安区',
           source: 'cloud', 
           sync_status: 'Error', 
           transcription_status: 'None',
           transcript: null
        },
      ];
      setRecords(mockData);
    } catch (e) {
      console.error("Failed load", e);
    }
  };

  const handleSync = (e) => {
    if(e) e.stopPropagation();
    setIsSyncing(true);
    
    // Simulate Global Sync
    setTimeout(() => {
      setIsSyncing(false);
      // Simulate a new file appearing or status update
      setRecords(prev => prev.map(r => 
         r.sync_status === 'NotSynced' || r.sync_status === 'Error'
           ? { ...r, sync_status: 'Synced' } 
           : r
      ));
    }, 2000);
  };

  const handleTranscribe = (e, id) => {
    e.stopPropagation();
    
    // 1. Uploading
    setRecords(prev => prev.map(r => r.id === id ? { ...r, transcription_status: 'Uploading' } : r));
    
    setTimeout(() => {
      // 2. Transcribing
      setRecords(prev => prev.map(r => r.id === id ? { ...r, transcription_status: 'Transcribing' } : r));
      
      setTimeout(() => {
        // 3. Ready
        setRecords(prev => prev.map(r => r.id === id ? { ...r, transcription_status: 'Ready', transcript: MOCK_TRANSCRIPT } : r));
      }, 3500);
    }, 1500);
  };

  const openTranscriptViewer = (file) => {
    if (file.transcription_status === 'Ready') {
      setSelectedFile(file);
    }
  };

  return (
    <div className="h-full flex flex-col max-w-3xl mx-auto relative pt-6">
      <div className="flex justify-between items-end mb-6 px-2">
        <div>
           <h1 className="text-2xl font-bold text-black">录音文件</h1>
           <p className="text-[#8E8E93]">同步并管理设备录音</p>
        </div>
        <button 
          onClick={handleSync}
          disabled={isSyncing}
          className={`p-3 rounded-full bg-white border border-[#E5E5EA] hover:bg-[#F2F2F7] active:scale-95 transition-all duration-200 shadow-sm ${isSyncing ? 'animate-spin text-[#007AFF]' : 'text-black'}`}
          aria-label="同步录音"
        >
          <RefreshCw size={20} />
        </button>
      </div>

      <div className="space-y-4 pb-20 px-2">
        <AnimatePresence>
          {recordings.map((rec, index) => (
            <motion.div
              key={rec.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
            >
              <NeumorphicCard 
                 className="flex items-center justify-between hover:shadow-md active:scale-[0.99] transition-all duration-200 bg-white border border-[#E5E5EA] shadow-sm cursor-pointer"
                 onClick={() => openTranscriptViewer(rec)}
              >
                <div className="flex items-center gap-4">
                  {/* Source Icon */}
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${rec.transcription_status === 'Ready' ? 'bg-[#007AFF]/10 text-[#007AFF]' : 'bg-[#F2F2F7] text-[#8E8E93]'}`}>
                     {rec.source === 'cloud' ? <Cloud size={20} /> : <Smartphone size={20} />}
                  </div>
                  
                  <div>
                    <div className="font-bold text-black mb-1 text-sm md:text-base">
                      {rec.filename}
                    </div>
                    <div className="flex items-center gap-3 text-xs text-[#8E8E93]">
                       <span className="flex items-center gap-1">
                         <Clock size={12} /> {rec.duration}
                       </span>
                       <span className="flex items-center gap-1">
                         {new Date(rec.date).toLocaleDateString()}
                       </span>
                       {/* Sync Status Indicator */}
                       {rec.sync_status === 'Syncing' && <span className="text-blue-500">Syncing...</span>}
                       {rec.sync_status === 'Error' && <span className="text-red-500">Sync Error</span>}
                       {rec.sync_status === 'NotSynced' && <span className="text-orange-500">Not Synced</span>}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {/* Right Side Actions / Status */}
                  
                  {/* Case 1: Ready -> Show "View Transcript" CTA or Preview */}
                  {rec.transcription_status === 'Ready' && (
                     <div className="flex items-center gap-2">
                        <div className="text-xs text-[#8E8E93] bg-[#F2F2F7] px-2 py-1 rounded-md hidden md:block max-w-[100px] truncate">
                           {rec.transcript ? "..." + rec.transcript.substring(0, 10) : "Transcript Ready"}
                        </div>
                        <button className="px-3 py-1.5 bg-[#F2F2F7] hover:bg-[#E5E5EA] active:bg-[#E5E5EA]/80 rounded-full text-xs font-medium text-[#007AFF] flex items-center gap-1 transition-all duration-200 shadow-sm">
                           <FileText size={12} /> 查看转写
                        </button>
                     </div>
                  )}

                  {/* Case 2: Transcribing / Uploading -> Spinner */}
                  {(rec.transcription_status === 'Transcribing' || rec.transcription_status === 'Uploading') && (
                    <div className="flex items-center gap-1 text-xs text-orange-500 px-3">
                      <Loader2 size={14} className="animate-spin" /> 
                      {rec.transcription_status === 'Uploading' ? '上传中...' : '转写中...'}
                    </div>
                  )}

                  {/* Case 3: None/Idle -> Transcribe Button */}
                  {rec.transcription_status === 'None' && rec.sync_status === 'Synced' && (
                     <button 
                       onClick={(e) => handleTranscribe(e, rec.id)}
                       className="p-2 text-[#007AFF] hover:bg-[#F2F2F7] active:bg-[#E5E5EA] rounded-full border border-transparent hover:border-[#E5E5EA] transition-all duration-200"
                       title="Start Transcription"
                       aria-label="开始转写"
                     >
                       <div className="flex items-center gap-1 text-xs font-medium">
                          <Download size={16} />
                          <span className="hidden sm:inline">转写</span>
                       </div>
                     </button>
                  )}
                  
                  {/* Case 4: Not Synced -> Sync Button (Individual) */}
                  {rec.sync_status === 'NotSynced' && (
                     <button 
                       onClick={handleSync}
                       className="p-2 text-[#8E8E93] hover:text-[#007AFF] active:bg-[#F2F2F7] rounded-full transition-all duration-200"
                       title="Sync File"
                       aria-label="同步文件"
                     >
                       <RefreshCw size={16} />
                     </button>
                  )}

                </div>
              </NeumorphicCard>
            </motion.div>
          ))}
        </AnimatePresence>

        {recordings.length === 0 && (
          <div className="text-center py-12 text-[#8E8E93]">
            暂无录音，下拉同步。
          </div>
        )}
      </div>

      {/* Transcript Viewer Drawer */}
      <AudioDrawer 
        isOpen={!!selectedFile} 
        onClose={() => setSelectedFile(null)} 
        file={selectedFile}
      />

    </div>
  );
}