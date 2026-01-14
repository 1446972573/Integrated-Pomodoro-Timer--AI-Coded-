import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  icon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({ 
  children, 
  variant = 'primary', 
  icon, 
  className = '', 
  ...props 
}) => {
  const baseStyles = "flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-semibold transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed";
  
  const variants = {
    primary: "bg-primary hover:bg-indigo-500 text-white shadow-lg shadow-indigo-500/30",
    secondary: "bg-surface hover:bg-slate-700 text-slate-200 border border-slate-700",
    danger: "bg-accent hover:bg-rose-600 text-white shadow-lg shadow-rose-500/30",
    ghost: "bg-transparent hover:bg-white/5 text-slate-400 hover:text-white"
  };

  return (
    <button 
      className={`${baseStyles} ${variants[variant]} ${className}`} 
      {...props}
    >
      {icon}
      {children}
    </button>
  );
};