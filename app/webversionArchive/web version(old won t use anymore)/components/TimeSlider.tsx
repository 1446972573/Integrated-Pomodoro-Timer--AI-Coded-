import React from 'react';

interface TimeSliderProps {
  label: string;
  value: number;
  max: number;
  onChange: (val: number) => void;
  disabled?: boolean;
}

export const TimeSlider: React.FC<TimeSliderProps> = ({ 
  label, 
  value, 
  max, 
  onChange, 
  disabled = false 
}) => {
  return (
    <div className={`flex flex-col gap-2 transition-opacity duration-300 ${disabled ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
      <div className="flex justify-between items-end">
        <label className="text-sm font-medium text-slate-400 uppercase tracking-wider">{label}</label>
        <span className="text-xl font-mono text-primary font-bold">{value.toString().padStart(2, '0')}</span>
      </div>
      <input
        type="range"
        min="0"
        max={max}
        value={value}
        onChange={(e) => onChange(parseInt(e.target.value, 10))}
        disabled={disabled}
        className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer"
      />
    </div>
  );
};