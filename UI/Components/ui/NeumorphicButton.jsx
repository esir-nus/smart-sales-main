import React from 'react';

export default function NeumorphicButton({ 
  children, 
  onClick, 
  variant = 'default', // default, primary, icon
  className = '',
  type = 'button',
  disabled = false
}) {
  const baseClass = variant === 'primary' ? 'neu-btn-primary' : 'neu-btn';
  
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`${baseClass} ${className} ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
    >
      {children}
    </button>
  );
}