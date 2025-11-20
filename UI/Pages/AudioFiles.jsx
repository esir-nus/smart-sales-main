import React, { useState, useEffect } from 'react';
import { base44 } from '@/api/base44Client';
import { Play, Pause, Download, Clock, MapPin, Check, RefreshCw } from 'lucide-react';
import NeumorphicCard from '@/components/ui/NeumorphicCard';
import { motion, AnimatePresence } from 'framer-motion';

export default function AudioFilesPage() {
  const [recordings, setRecords] = useState([]);
  const [isSyncing, setIsSyncing] = useState(false);
  const [playingId, setPlayingId] = useState(null);

  useEffect(() => {
    loadRecordings();
  }, []);

  const loadRecordings = async () => {
    try {
      const data = await base44.entities.AudioRecord.list({ sort: { date: -1 } });
      setRecords(data);
    } catch (e) {
      console.error("Failed load", e);
      // Mock data
      setRecords([
        { id: 1, filename: '2025/10/30/15/41', date: '2025-10-30T15:41:00', location: '福田区', duration: '45:20', is_synced: true },
        { id: 2, filename: '2025/12/30/22/21', date: '2025-12-30T22:21:00', location: '南山区', duration: '12:05', is_synced: true },
        { id: 3, filename: '2025/12/30/11/21', date: '2025-12-30T11:21:00', location: '南山区', duration: '05:42', is_synced: false },
      ]);
    }
  };

  const handleSync = () => {
    setIsSyncing(true);
    setTimeout(() => {
      setIsSyncing(false);
      // Add a mock new recording
      const newRec = {
        id: Date.now(),
        filename: '2025/01/05/09/00',
        date: new Date().toISOString(),
        location: '总部办公室',
        duration: '01:00',
        is_synced: true
      };
      setRecords([newRec, ...recordings]);
    }, 2000);
  };

  const togglePlay = (id) => {
    if (playingId === id) {
      setPlayingId(null);
    } else {
      setPlayingId(id);
    }
  };

  return (
    <div className="h-full flex flex-col max-w-3xl mx-auto">
      <div className="flex justify-between items-end mb-6 px-2">
        <div>
           <h1 className="text-2xl font-bold text-black">录音文件</h1>
           <p className="text-[#8E8E93]">同步并管理设备录音</p>
        </div>
        <button 
          onClick={handleSync}
          disabled={isSyncing}
          className={`p-3 rounded-full bg-white border border-[#E5E5EA] hover:bg-[#F2F2F7] transition-all shadow-sm ${isSyncing ? 'animate-spin text-[#007AFF]' : 'text-black'}`}
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
              <NeumorphicCard className="flex items-center justify-between hover:scale-[1.01] transition-transform duration-200 bg-white border border-[#E5E5EA] shadow-sm">
                <div className="flex items-center gap-4">
                  <button 
                    onClick={() => togglePlay(rec.id)}
                    className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 transition-all ${
                      playingId === rec.id 
                        ? 'bg-[#007AFF] text-white' 
                        : 'bg-[#F2F2F7] text-[#007AFF]'
                    }`}
                  >
                    {playingId === rec.id ? <Pause size={16} /> : <Play size={16} className="ml-0.5" />}
                  </button>
                  
                  <div>
                    <div className="font-bold text-black mb-1 text-sm md:text-base">
                      录音: {rec.filename}
                    </div>
                    <div className="flex items-center gap-3 text-xs text-[#8E8E93]">
                       <span className="flex items-center gap-1">
                         <Clock size={12} /> {rec.duration}
                       </span>
                       <span className="flex items-center gap-1">
                         <MapPin size={12} /> {rec.location}
                       </span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                   {rec.is_synced ? (
                     <div className="px-3 py-1 rounded-full bg-[#F2F2F7] border border-[#E5E5EA] text-[#34C759] text-xs font-bold flex items-center gap-1">
                       <Check size={12} /> 已同步
                     </div>
                   ) : (
                     <button className="p-2 text-[#8E8E93] hover:text-[#007AFF]">
                       <Download size={18} />
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
    </div>
  );
}