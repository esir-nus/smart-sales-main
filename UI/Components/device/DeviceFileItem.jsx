import React from 'react';
import { Check, Trash2, Video, Play, FileType } from 'lucide-react';
import { motion } from 'framer-motion';

export default function DeviceFileItem({ file, onApply, onDelete, isSelected, onSelect }) {
  const isVideo = file.type === 'video';
  const isGif = file.type === 'gif' || file.filename.toLowerCase().endsWith('.gif');

  return (
    <motion.div 
      layout
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.2 }}
      onClick={() => onSelect(file)}
      className={`group relative bg-white rounded-2xl p-2 shadow-sm border transition-all duration-200 cursor-pointer active:scale-[0.98]
        ${isSelected ? 'border-[#007AFF] ring-2 ring-[#007AFF]/20 shadow-md' : 'border-[#E5E5EA] hover:border-[#007AFF]/50 hover:shadow-md'}
      `}
    >
       {/* Thumbnail Container */}
       <div className="aspect-[4/3] rounded-xl overflow-hidden bg-[#F2F2F7] relative">
          {file.type === 'image' || isGif ? (
            <img src={file.url} alt={file.filename} className="w-full h-full object-cover" loading="lazy" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-[#C7C7CC] bg-[#F2F2F7]">
              <Video size={32} />
            </div>
          )}
          
          {/* Type Indicators */}
          {isVideo && file.duration && (
            <div className="absolute bottom-2 right-2 bg-black/70 backdrop-blur-sm text-white text-[10px] px-2 py-1 rounded-md font-mono flex items-center gap-1 shadow-sm">
               <Play size={8} fill="currentColor" /> {file.duration}
            </div>
          )}

          {isGif && (
            <div className="absolute top-2 right-2 bg-black/70 backdrop-blur-sm text-white text-[9px] px-2 py-1 rounded-md font-bold shadow-sm">
              GIF
            </div>
          )}

          {/* Current Applied Status Badge */}
          {file.is_applied && (
             <div className="absolute top-2 left-2 bg-[#34C759] text-white text-[10px] px-2 py-1 rounded-full font-bold flex items-center gap-1 shadow-md z-10">
                <Check size={10} strokeWidth={3} />
             </div>
          )}
          
          {/* Hover Actions */}
          <div className="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
             {/* Actions can go here if needed, but selection is primary */}
          </div>
       </div>
       
       {/* Simple Footer */}
       <div className="mt-2 px-1">
          <div className={`text-xs font-bold truncate ${isSelected ? 'text-[#007AFF]' : 'text-gray-700'}`}>
            {file.filename}
          </div>
       </div>
    </motion.div>
  );
}