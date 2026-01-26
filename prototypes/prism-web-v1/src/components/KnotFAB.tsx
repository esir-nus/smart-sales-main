import React from 'react';
import { motion } from 'framer-motion';
import { Sparkles } from 'lucide-react';

interface KnotFABProps {
  isThinking?: boolean;
  onClick?: () => void;
}

export const KnotFAB: React.FC<KnotFABProps> = ({ isThinking = false, onClick }) => {
  return (
    <div className="absolute right-6 bottom-[130px] z-30">
        {/* Tip Bubble (Placeholder for now) */}
        
        {/* The Knot Orb */}
        <motion.button
            className="w-14 h-14 rounded-full bg-white/40 backdrop-blur-md border border-white/60 shadow-glass flex items-center justify-center text-prism-knot"
            animate={{
                scale: isThinking ? [1, 0.9, 1] : [1, 1.15, 1], // Breathing scale
                rotate: isThinking ? 360 : 0,
            }}
            transition={{
                scale: {
                    duration: isThinking ? 0.5 : 4, // 4s breathing cycle
                    repeat: Infinity,
                    ease: "easeInOut"
                },
                rotate: {
                    duration: 1,
                    repeat: Infinity,
                    ease: "linear",
                    repeatType: "loop"
                }
            }}
            onClick={onClick}
        >
            <Sparkles size={28} strokeWidth={2} />
        </motion.button>
        
        {/* Glow Layer */}
        <motion.div 
            className="absolute inset-0 rounded-full bg-prism-knot blur-xl -z-10"
            animate={{ opacity: [0.2, 0.4, 0.2] }}
            transition={{ duration: 4, repeat: Infinity }}
        />
    </div>
  );
};
