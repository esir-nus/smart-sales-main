import React from 'react';
import { FileText, Package } from 'lucide-react';

export const RightToolbar = () => {
  return (
    <div className="absolute right-4 top-1/2 -translate-y-1/2 flex flex-col gap-4 z-20">
      {/* Document/List FAB */}
      <button className="w-12 h-12 bg-prism-surface/80 backdrop-blur-xl rounded-[16px] border border-prism-border/50 shadow-glass flex items-center justify-center active:scale-95 transition-transform">
        <FileText size={20} className="text-prism-secondary" />
      </button>

      {/* Package/Artifacts FAB */}
      <button className="w-12 h-12 bg-prism-surface/80 backdrop-blur-xl rounded-[16px] border border-prism-border/50 shadow-glass flex items-center justify-center active:scale-95 transition-transform">
        <Package size={20} className="text-prism-secondary" />
      </button>
    </div>
  );
};
