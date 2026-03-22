import { AlignJustify, Package } from 'lucide-react';

export const FloatingActions = () => {
  return (
    <div className="absolute right-4 bottom-48 flex flex-col gap-4 z-20">
        <button className="w-12 h-12 bg-prism-surface backdrop-blur-xl rounded-[16px] shadow-glass border-retina border-prism-border flex items-center justify-center text-prism-primary hover:scale-105 transition-transform active:scale-95">
            <AlignJustify size={20} />
        </button>
        <button className="w-12 h-12 bg-prism-surface backdrop-blur-xl rounded-[16px] shadow-glass border-retina border-prism-border flex items-center justify-center text-prism-primary hover:scale-105 transition-transform active:scale-95">
            <Package size={20} />
        </button>
    </div>
  );
};
