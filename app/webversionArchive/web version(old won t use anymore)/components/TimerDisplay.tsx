import React, { useMemo } from 'react';
import { TimerStatus } from '../types';

interface TimerDisplayProps {
  totalSeconds: number;
  initialTotalSeconds: number;
  status: TimerStatus;
}

export const TimerDisplay: React.FC<TimerDisplayProps> = ({ 
  totalSeconds, 
  initialTotalSeconds,
  status 
}) => {
  // Format seconds into HH:MM:SS
  const formattedTime = useMemo(() => {
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;
    return {
      h: h.toString().padStart(2, '0'),
      m: m.toString().padStart(2, '0'),
      s: s.toString().padStart(2, '0')
    };
  }, [totalSeconds]);

  // Calculate progress for the circular ring
  const progress = initialTotalSeconds > 0 
    ? ((initialTotalSeconds - totalSeconds) / initialTotalSeconds) * 100 
    : 0;
  
  const radius = 120;
  const stroke = 8;
  const normalizedRadius = radius - stroke * 2;
  const circumference = normalizedRadius * 2 * Math.PI;
  const strokeDashoffset = circumference - (progress / 100) * circumference;

  return (
    <div className="relative flex flex-col items-center justify-center py-10">
      {/* Background Glow */}
      <div className={`absolute inset-0 bg-primary/20 blur-[100px] rounded-full transition-opacity duration-700 ${status === TimerStatus.RUNNING ? 'opacity-100 animate-pulse-slow' : 'opacity-20'}`} />

      <div className="relative">
        {/* SVG Ring */}
        <svg
          height={radius * 2}
          width={radius * 2}
          className="rotate-[-90deg] transform transition-all duration-500"
        >
          {/* Track */}
          <circle
            stroke="#1e293b"
            strokeWidth={stroke}
            fill="transparent"
            r={normalizedRadius}
            cx={radius}
            cy={radius}
          />
          {/* Progress Indicator */}
          <circle
            stroke="#6366f1"
            strokeWidth={stroke}
            strokeDasharray={circumference + ' ' + circumference}
            style={{ strokeDashoffset, transition: 'stroke-dashoffset 1s linear' }}
            strokeLinecap="round"
            fill="transparent"
            r={normalizedRadius}
            cx={radius}
            cy={radius}
            className={`${status === TimerStatus.COMPLETED ? 'stroke-green-500' : 'stroke-primary'}`}
          />
        </svg>

        {/* Digital Time Center */}
        <div className="absolute inset-0 flex flex-col items-center justify-center font-mono text-white">
          <div className="text-5xl md:text-6xl font-bold tracking-tighter">
            {formattedTime.h}:{formattedTime.m}
          </div>
          <div className="text-2xl md:text-3xl font-medium text-slate-400 mt-1">
            {formattedTime.s}
          </div>
          <div className="mt-2 text-xs text-slate-500 tracking-[0.2em] uppercase">
            {status === TimerStatus.COMPLETED ? 'Finished' : status === TimerStatus.PAUSED ? 'Paused' : 'Remaining'}
          </div>
        </div>
      </div>
    </div>
  );
};