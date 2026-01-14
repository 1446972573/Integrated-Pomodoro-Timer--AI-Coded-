import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Play, Pause, Square, RefreshCcw, Clock } from 'lucide-react';
import { TimerStatus, TimeConfig } from '../types';
import { Button } from './Button';
import { TimeSlider } from './TimeSlider';
import { TimerDisplay } from './TimerDisplay';

const TimerPage: React.FC = () => {
  // Config State
  const [config, setConfig] = useState<TimeConfig>({ hours: 0, minutes: 5, seconds: 0 });
  
  // Runtime State
  const [remainingSeconds, setRemainingSeconds] = useState<number>(300);
  const [initialSeconds, setInitialSeconds] = useState<number>(300);
  const [status, setStatus] = useState<TimerStatus>(TimerStatus.IDLE);
  
  const timerRef = useRef<number | null>(null);
  const endTimeRef = useRef<number>(0);

  // Calculate total seconds from config
  const getConfigSeconds = useCallback(() => {
    return config.hours * 3600 + config.minutes * 60 + config.seconds;
  }, [config]);

  // Sync remaining time with config when IDLE
  useEffect(() => {
    if (status === TimerStatus.IDLE) {
      const total = getConfigSeconds();
      setRemainingSeconds(total);
      setInitialSeconds(total);
    }
  }, [config, status, getConfigSeconds]);

  // Timer Logic
  useEffect(() => {
    if (status === TimerStatus.RUNNING) {
      if (!endTimeRef.current) {
        endTimeRef.current = Date.now() + remainingSeconds * 1000;
      }

      timerRef.current = window.setInterval(() => {
        const now = Date.now();
        const diff = Math.ceil((endTimeRef.current - now) / 1000);

        if (diff <= 0) {
          setRemainingSeconds(0);
          setStatus(TimerStatus.COMPLETED);
          if (timerRef.current) clearInterval(timerRef.current);
          endTimeRef.current = 0;
        } else {
          setRemainingSeconds(diff);
        }
      }, 100); 
    } else {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
      if (status === TimerStatus.PAUSED) {
        endTimeRef.current = 0;
      }
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [status, remainingSeconds]);

  // Handlers
  const handleStart = () => {
    if (remainingSeconds === 0) return;
    setStatus(TimerStatus.RUNNING);
  };

  const handlePause = () => {
    setStatus(TimerStatus.PAUSED);
  };

  const handleStop = () => {
    setStatus(TimerStatus.IDLE);
    endTimeRef.current = 0;
    const total = getConfigSeconds();
    setRemainingSeconds(total);
  };

  return (
    <div className="h-full w-full flex items-center justify-center p-4 select-none">
      <div className="w-full max-w-md bg-surface/50 backdrop-blur-xl border border-white/10 rounded-3xl shadow-2xl overflow-hidden p-6 flex flex-col gap-6 md:gap-8">
        
        {/* Header */}
        <div className="flex items-center gap-3 border-b border-white/5 pb-4 md:pb-6">
          <div className="p-2 bg-primary/20 rounded-lg text-primary">
            <Clock size={20} className="md:w-6 md:h-6" />
          </div>
          <div>
            <h1 className="text-lg md:text-xl font-bold text-white tracking-tight">ZenTimer</h1>
            <p className="text-[10px] md:text-xs text-slate-400">Focus on what matters</p>
          </div>
          <div className="ml-auto text-[10px] md:text-xs px-2 py-1 rounded-full bg-white/5 text-slate-400 font-mono">
            {status}
          </div>
        </div>

        {/* Main Display */}
        <div className="flex-1 flex flex-col items-center justify-center min-h-[250px] md:min-h-[300px]">
           <div className="scale-90 md:scale-100">
             <TimerDisplay 
               totalSeconds={remainingSeconds} 
               initialTotalSeconds={initialSeconds}
               status={status}
             />
           </div>
        </div>

        {/* Sliders */}
        <div className={`flex flex-col gap-4 md:gap-6 transition-all duration-500 ${status === TimerStatus.IDLE ? 'opacity-100 max-h-96' : 'opacity-40 pointer-events-none grayscale max-h-0 overflow-hidden'}`}>
          <TimeSlider 
            label="Hours" 
            value={config.hours} 
            max={23} 
            onChange={(v) => setConfig(prev => ({ ...prev, hours: v }))} 
          />
          <TimeSlider 
            label="Minutes" 
            value={config.minutes} 
            max={59} 
            onChange={(v) => setConfig(prev => ({ ...prev, minutes: v }))} 
          />
          <TimeSlider 
            label="Seconds" 
            value={config.seconds} 
            max={59} 
            onChange={(v) => setConfig(prev => ({ ...prev, seconds: v }))} 
          />
        </div>

        {/* Controls */}
        <div className="grid grid-cols-2 gap-4 pt-4 border-t border-white/5">
          {status === TimerStatus.RUNNING ? (
            <Button onClick={handlePause} variant="secondary" icon={<Pause size={18} />}>
              Pause
            </Button>
          ) : (
            <Button 
              onClick={handleStart} 
              variant="primary" 
              icon={<Play size={18} />}
              disabled={remainingSeconds === 0 && status !== TimerStatus.COMPLETED}
            >
              {status === TimerStatus.PAUSED ? 'Resume' : 'Start'}
            </Button>
          )}

          {status === TimerStatus.IDLE ? (
             <Button onClick={() => setConfig({hours: 0, minutes: 0, seconds: 0})} variant="ghost" icon={<RefreshCcw size={18} />}>
               Clear
             </Button>
          ) : (
            <Button onClick={handleStop} variant="danger" icon={<Square size={18} />}>
              Stop
            </Button>
          )}
        </div>
        
        {status === TimerStatus.COMPLETED && (
           <div className="absolute inset-0 z-10 bg-black/80 backdrop-blur-sm flex items-center justify-center rounded-3xl animate-in fade-in duration-300 p-4">
             <div className="bg-surface border border-white/10 p-6 md:p-8 rounded-2xl flex flex-col items-center text-center shadow-2xl w-full max-w-sm">
                <div className="text-4xl mb-4">🎉</div>
                <h2 className="text-xl md:text-2xl font-bold text-white mb-2">Time's Up!</h2>
                <p className="text-slate-400 mb-6">Your session has finished.</p>
                <Button onClick={handleStop} variant="primary" icon={<RefreshCcw size={18} />}>
                  Start New Timer
                </Button>
             </div>
           </div>
        )}

      </div>
    </div>
  );
};

export default TimerPage;