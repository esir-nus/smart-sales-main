export const AuroraBackground: React.FC = () => {
  return (
    <div className="absolute inset-0 z-0 overflow-hidden pointer-events-none bg-prism-bg">
      <div className="absolute inset-0 opacity-60 filter blur-[60px] transition-opacity duration-500">
          {/* Blob 1: Top-Left (Blue) - V12: radial-gradient(circle at 20% 10%, rgba(10, 132, 255, 0.28) 0%, transparent 60%) */}
          <div 
            className="absolute rounded-full w-[500px] h-[500px] top-[-100px] left-[-100px] animate-breathing"
            style={{ 
                background: 'radial-gradient(circle, rgba(10, 132, 255, 0.28) 0%, transparent 70%)',
                animationDelay: '0s' 
            }}
          />
          
          {/* Blob 2: Center-Right (Indigo) - V12: radial-gradient(circle at 80% 45%, rgba(94, 92, 230, 0.24) 0%, transparent 60%) */}
          <div 
            className="absolute rounded-full w-[400px] h-[400px] top-[30%] right-[-100px] animate-breathing"
            style={{ 
                background: 'radial-gradient(circle, rgba(94, 92, 230, 0.24) 0%, transparent 70%)',
                animationDelay: '2s' 
            }}
          />
          
          {/* Blob 3: Bottom-Left (Cyan) - V12: radial-gradient(circle at 30% 85%, rgba(100, 210, 255, 0.20) 0%, transparent 60%) */}
          <div 
            className="absolute rounded-full w-[450px] h-[450px] bottom-[-50px] left-[-50px] animate-breathing"
            style={{ 
                background: 'radial-gradient(circle, rgba(100, 210, 255, 0.20) 0%, transparent 70%)',
                animationDelay: '4s' 
            }}
          />
      </div>
    </div>
  );
};
