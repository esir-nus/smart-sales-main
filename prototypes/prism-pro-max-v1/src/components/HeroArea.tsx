import React from 'react';
import { Star, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';

export const HeroArea = () => {
  return (
    <div className="flex-1 flex flex-col items-center justify-center relative -mt-20">
        
        {/* Background Ambient Glows */}
        <div className="absolute top-1/4 -left-12 w-64 h-64 bg-purple-400/20 blur-[100px] rounded-full mix-blend-multiply" />
        <div className="absolute bottom-1/3 -right-12 w-64 h-64 bg-teal-400/20 blur-[100px] rounded-full mix-blend-multiply" />

        {/* Central Hub */}
        <motion.div 
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="flex flex-col items-center gap-6 mt-12"
        >
            {/* The Star (System Source) - Pure Floating Glass */}
            <div className="relative group">
                <div className="absolute inset-0 bg-prism-primary/20 blur-3xl rounded-full scale-150 group-hover:bg-prism-primary/30 transition-all duration-700" />
                
                {/* 3D Glass Object */}
                <div className="w-24 h-24 rounded-[32px] bg-gradient-to-br from-white/10 to-transparent backdrop-blur-md border border-white/20 shadow-[0_8px_32px_0_rgba(31,38,135,0.07)] flex items-center justify-center transform hover:scale-110 transition-transform duration-500 hover:rotate-6">
                     <Star size={44} className="text-prism-primary drop-shadow-[0_2px_10px_rgba(255,255,255,0.5)] fill-prism-primary/20" strokeWidth={1.5} />
                </div>

                {/* Badge floating outside */}
                <div className="absolute -top-4 -right-4 bg-black/90 text-white text-[10px] font-bold px-2.5 py-1 rounded-full border border-white/10 shadow-xl tracking-wider">
                    PRISM
                </div>
            </div>

            {/* Connection Line - Organic Trace */}
            <div className="h-16 w-[1.5px] bg-gradient-to-b from-prism-primary/30 to-transparent rounded-full" />

            {/* Greeting - Matches Wireframe */}
            <div className="text-center">
                <h1 className="text-2xl font-semibold tracking-tight text-prism-primary flex items-center gap-2">
                    <Sparkles size={18} className="text-amber-500 fill-amber-500" />
                    上午好, Frank
                </h1>
            </div>

        </motion.div>
    </div>
  );
};
