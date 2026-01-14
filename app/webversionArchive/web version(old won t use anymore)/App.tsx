import React, { useRef, useEffect, useState, MouseEvent, useCallback } from 'react';
import TimerPage from './components/TimerPage';
import AudioPlayerPage from './components/AudioPlayerPage';
import PlaylistManagerPage from './components/PlaylistManagerPage';
import { Playlist, Track, GLOBAL_TRACK_POOL } from './types';

const PageContainer: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = "" }) => (
  <div className={`w-screen h-[100dvh] flex-shrink-0 snap-center overflow-hidden ${className}`}>
    {children}
  </div>
);

const BlankPage: React.FC<{ label: string }> = ({ label }) => (
  <div className="w-full h-full flex flex-col items-center justify-center bg-background/50 text-slate-700 font-mono text-sm space-y-4">
    <div className="w-16 h-16 border-2 border-dashed border-slate-700 rounded-xl flex items-center justify-center opacity-50">
       <span className="text-2xl">+</span>
    </div>
    <span>{label}</span>
  </div>
);

const App: React.FC = () => {
  const scrollRef = useRef<HTMLDivElement>(null);
  
  // Navigation State
  const [isDragging, setIsDragging] = useState(false);
  const [startX, setStartX] = useState(0);
  const [scrollLeft, setScrollLeft] = useState(0);
  const [isScrollLocked, setIsScrollLocked] = useState(false);

  // Data State
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [activePlaylistId, setActivePlaylistId] = useState<string | null>(null);
  
  // Controls communication between pages
  const [forcePlayTrigger, setForcePlayTrigger] = useState<number>(0); 
  const [forceStopTrigger, setForceStopTrigger] = useState<number>(0);

  // --- Persistence Logic ---
  
  // Load on startup
  useEffect(() => {
    const saved = localStorage.getItem('zentimer_playlists');
    if (saved) {
      try {
        setPlaylists(JSON.parse(saved));
      } catch (e) {
        console.error("Failed to load playlists", e);
        initializeDefaults();
      }
    } else {
      initializeDefaults();
    }
  }, []);

  const initializeDefaults = () => {
    const defaultList: Playlist = {
      id: 'default-pl-1',
      name: 'Chill Vibes',
      tracks: GLOBAL_TRACK_POOL.slice(0, 3)
    };
    setPlaylists([defaultList]);
    setActivePlaylistId(defaultList.id);
  };

  // Auto-save every minute
  useEffect(() => {
    const interval = setInterval(() => {
      if (!isScrollLocked) { // Only save if not in edit mode (simple heuristic based on prompt requirements)
         localStorage.setItem('zentimer_playlists', JSON.stringify(playlists));
         console.log("Auto-saved playlists");
      }
    }, 60000);
    return () => clearInterval(interval);
  }, [playlists, isScrollLocked]);

  // Save immediately when requested
  const handleSavePlaylists = (newPlaylists: Playlist[]) => {
    setPlaylists(newPlaylists);
    localStorage.setItem('zentimer_playlists', JSON.stringify(newPlaylists));
  };

  // --- Navigation Logic ---

  useEffect(() => {
    // Start at the middle page (Timer)
    if (scrollRef.current) {
      const width = window.innerWidth;
      scrollRef.current.scrollLeft = width * 2;
    }
  }, []);

  const handleMouseDown = (e: MouseEvent) => {
    if (isScrollLocked) return;
    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'BUTTON' || target.closest('button') || target.closest('.no-drag')) {
      return;
    }
    setIsDragging(true);
    if (scrollRef.current) {
      setStartX(e.pageX - scrollRef.current.offsetLeft);
      setScrollLeft(scrollRef.current.scrollLeft);
    }
  };

  const handleMouseLeave = () => setIsDragging(false);
  const handleMouseUp = () => setIsDragging(false);

  const handleMouseMove = (e: MouseEvent) => {
    if (!isDragging || !scrollRef.current || isScrollLocked) return;
    e.preventDefault();
    const x = e.pageX - scrollRef.current.offsetLeft;
    const walk = (x - startX) * 1.5;
    scrollRef.current.scrollLeft = scrollLeft - walk;
  };

  // --- Handlers for Playlist Page ---

  const onPlayRequest = (playlistId: string) => {
    setActivePlaylistId(playlistId);
    setForcePlayTrigger(prev => prev + 1);
  };

  return (
    <div 
      ref={scrollRef}
      onMouseDown={handleMouseDown}
      onMouseLeave={handleMouseLeave}
      onMouseUp={handleMouseUp}
      onMouseMove={handleMouseMove}
      className={`
        flex w-screen h-[100dvh] overflow-x-auto overflow-y-hidden no-scrollbar touch-pan-x
        ${isDragging ? 'cursor-grabbing snap-none' : isScrollLocked ? 'overflow-hidden snap-none' : 'cursor-grab snap-x snap-mandatory'}
      `}
    >
      <PageContainer>
        <BlankPage label="Left Page 2" />
      </PageContainer>
      
      <PageContainer>
        <BlankPage label="Left Page 1" />
      </PageContainer>
      
      <PageContainer>
        <TimerPage />
      </PageContainer>
      
      <PageContainer>
        <AudioPlayerPage 
          playlists={playlists}
          activePlaylistId={activePlaylistId}
          setPlaylists={handleSavePlaylists}
          forcePlayTrigger={forcePlayTrigger}
        />
      </PageContainer>
      
      <PageContainer>
        <PlaylistManagerPage 
          playlists={playlists}
          setPlaylists={handleSavePlaylists}
          onPlayRequest={onPlayRequest}
          setScrollLocked={setIsScrollLocked}
        />
      </PageContainer>
    </div>
  );
};

export default App;