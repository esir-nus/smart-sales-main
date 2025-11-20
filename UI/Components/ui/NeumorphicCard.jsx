import React from 'react';

export default function NeumorphicCard({ children, className = '', onClick }) {
  return (
    <div 
      onClick={onClick}
      className={`neu-card p-4 ${className} ${onClick ? 'cursor-pointer transition-transform active:scale-[0.99]' : ''}`}
    >
      {children}
    </div>
  );
}