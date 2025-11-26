import React, { useState, useEffect } from 'react';
import { motion, useMotionValue, useTransform, animate } from 'framer-motion';
import HomeView from '@/components/home/HomeView';
import AudioFilesPage from '@/pages/AudioFiles';
import DeviceManagerPage from '@/pages/DeviceManager';

export default function HomeWithVerticalOverlays() {
  // 0: Home, -1: Audio (Top), 1: Device (Bottom)
  const [pageIndex, setPageIndex] = useState(0);
  const y = useMotionValue(0);
  const screenHeight = typeof window !== 'undefined' ? window.innerHeight : 800;
  
  // Config - reduce gap to prevent blank space
  const GAP = 0;
  const LIMIT = screenHeight;

  // Sync internal state with URL params
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('context')) {
      setPageIndex(0);
    }
  }, []);

  // Animation logic
  useEffect(() => {
    let targetY = 0;
    if (pageIndex === -1) targetY = LIMIT; // Drag down -> Audio
    if (pageIndex === 1) targetY = -LIMIT; // Drag up -> Device

    animate(y, targetY, { 
      type: "spring", 
      stiffness: 260, 
      damping: 28,
      mass: 0.8
    });
  }, [pageIndex, screenHeight, y, LIMIT]);

  // Inverse transform for Home to simulate overlay effect (Home stays roughly in place)
  const homeY = useTransform(y, value => -value);
  
  // Backdrop visuals
  const backdropOpacity = useTransform(y, [-LIMIT, 0, LIMIT], [0.3, 0, 0.3]);
  const backdropBlur = useTransform(y, [-LIMIT, 0, LIMIT], ["4px", "0px", "4px"]);
  const backdropPointerEvents = useTransform(y, value => Math.abs(value) > 50 ? 'auto' : 'none');

  const handleDragEnd = (e, { offset, velocity }) => {
    const swipeThreshold = screenHeight * 0.15;
    const velocityThreshold = 400;

    if (pageIndex === 0) {
      if (offset.y > swipeThreshold || velocity.y > velocityThreshold) setPageIndex(-1);
      else if (offset.y < -swipeThreshold || velocity.y < -velocityThreshold) setPageIndex(1);
    } else {
      // If open, closing is easier
      if (pageIndex === -1 && (offset.y < -swipeThreshold || velocity.y < -velocityThreshold)) setPageIndex(0);
      if (pageIndex === 1 && (offset.y > swipeThreshold || velocity.y > velocityThreshold)) setPageIndex(0);
    }
  };

  return (
    <div className="h-screen w-full overflow-hidden bg-black relative">
      <motion.div
        className="h-full w-full relative"
        style={{ y }}
        drag="y"
        dragConstraints={{ 
            top: pageIndex === 1 ? -LIMIT : (pageIndex === 0 ? -LIMIT : 0), 
            bottom: pageIndex === -1 ? LIMIT : (pageIndex === 0 ? LIMIT : 0) 
        }}
        dragElastic={0.05}
        dragTransition={{ bounceStiffness: 300, bounceDamping: 30 }}
        onDragEnd={handleDragEnd}
      >
        {/* Top Layer: Audio Sync - "Drawer" style */}
        <motion.div 
          className="absolute left-0 w-full bg-[#F2F2F7] rounded-b-[24px] overflow-hidden z-30 shadow-2xl"
          style={{ 
            top: -LIMIT, 
            height: screenHeight
          }}
        >
           <AudioFilesPage />
           {/* Handle Bar */}
           <div className="absolute bottom-4 left-0 right-0 flex justify-center pointer-events-none">
             <div className="w-10 h-1 bg-[#C7C7CC] rounded-full" />
           </div>
        </motion.div>

        {/* Middle Layer: Home - Counter-transformed to appear static */}
        <motion.div 
          className="absolute top-0 left-0 w-full h-screen bg-[#F2F2F7] z-10 overflow-hidden"
          style={{ 
            y: homeY
          }}
        >
           {/* Invisible Touch Targets for Opening */}
           <div className="absolute top-0 left-0 right-0 h-16 z-50 cursor-grab active:cursor-grabbing" />
           
           <HomeView />
           
           {/* Backdrop Overlay for "Click to Close" */}
           <motion.div 
             className="absolute inset-0 bg-black/20 z-40"
             style={{ 
               opacity: backdropOpacity, 
               backdropFilter: backdropBlur,
               pointerEvents: backdropPointerEvents 
             }}
             onClick={() => setPageIndex(0)}
           />

           <div className="absolute bottom-0 left-0 right-0 h-16 z-50 cursor-grab active:cursor-grabbing" />
        </motion.div>

        {/* Bottom Layer: Device Manager - "Drawer" style */}
        <div 
          className="absolute left-0 w-full bg-[#F2F2F7] rounded-t-[24px] overflow-hidden z-30 shadow-2xl"
          style={{ 
            top: screenHeight, 
            height: screenHeight 
          }}
        >
           {/* Handle Bar */}
           <div className="absolute top-4 left-0 right-0 flex justify-center z-50 pointer-events-none">
             <div className="w-10 h-1 bg-[#C7C7CC] rounded-full" />
           </div>
           <DeviceManagerPage />
        </div>
      </motion.div>
    </div>
  );
}