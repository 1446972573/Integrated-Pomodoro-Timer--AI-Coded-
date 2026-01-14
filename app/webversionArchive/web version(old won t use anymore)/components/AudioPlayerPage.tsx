import React, { useState, useRef, useEffect, useCallback } from 'react';
import { Play, Pause, SkipBack, SkipForward, Shuffle, Repeat, AlertTriangle } from 'lucide-react';
import { Playlist, Track } from '../types';

interface AudioPlayerPageProps {
  playlists: Playlist[];
  activePlaylistId: string | null;
  setPlaylists: (playlists: Playlist[]) => void;
  forcePlayTrigger: number;
}

const AudioPlayerPage: React.FC<AudioPlayerPageProps> = ({ 
  playlists, 
  activePlaylistId, 
  setPlaylists,
  forcePlayTrigger 
}) => {
  // --- Derived State ---
  const activePlaylist = playlists.find(p => p.id === activePlaylistId);
  const currentTracks = activePlaylist ? activePlaylist.tracks : [];

  // --- Local State ---
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTrackIndex, setCurrentTrackIndex] = useState(0);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [isShuffle, setIsShuffle] = useState(true); // Default random as per prompt for "start play"
  const [isDragging, setIsDragging] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const audioRef = useRef<HTMLAudioElement>(null);
  
  // Helpers
  const formatTime = (time: number) => {
    if (isNaN(time)) return "00:00";
    const m = Math.floor(time / 60);
    const s = Math.floor(time % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // --- File Validation Logic ---
  const checkFileExists = async (url: string): Promise<boolean> => {
    // In a real app, we might do a HEAD request. 
    // Here we simulate checking. 
    // For our mock data, 'invalid_url_test' is defined as missing.
    if (url.includes('invalid_url_test')) return false;
    return true;
  };

  const markTrackInvalid = (trackId: string) => {
    const updatedPlaylists = playlists.map(p => {
      if (p.id === activePlaylistId) {
        return {
          ...p,
          tracks: p.tracks.map(t => t.id === trackId ? { ...t, isValid: false } : t)
        };
      }
      return p;
    });
    setPlaylists(updatedPlaylists);
  };

  // --- Playback Logic ---

  const playTrack = useCallback(async (index: number) => {
    if (!currentTracks || currentTracks.length === 0) return;
    if (index < 0 || index >= currentTracks.length) return;

    const track = currentTracks[index];

    // Check validity
    const isValid = await checkFileExists(track.url);
    
    if (!isValid) {
      console.warn(`Track ${track.fileName} is missing.`);
      markTrackInvalid(track.id);
      // Auto skip
      handleNext(true); // Pass flag to avoid infinite loops if all broken
      return;
    }

    // Recover validity if it was marked false but now works
    if (track.isValid === false) {
       const updatedPlaylists = playlists.map(p => {
        if (p.id === activePlaylistId) {
          return {
            ...p,
            tracks: p.tracks.map(t => t.id === track.id ? { ...t, isValid: true } : t)
          };
        }
        return p;
      });
      setPlaylists(updatedPlaylists);
    }

    setCurrentTrackIndex(index);
    setIsPlaying(true);
    // audioRef src update handled in effect
  }, [currentTracks, activePlaylistId, playlists, setPlaylists]);


  // Handle "Play Request" from Playlist Page
  useEffect(() => {
    if (forcePlayTrigger > 0 && currentTracks.length > 0) {
      // Prompt: "If playing or paused, next song starts... if idle, start play"
      // "Randomly select from target playlist"
      let nextIndex = 0;
      if (isShuffle) {
        nextIndex = Math.floor(Math.random() * currentTracks.length);
      }
      
      // If we are already playing a song from this playlist, we skip to next.
      // If we were playing a different playlist, we start new.
      playTrack(nextIndex);
    }
  }, [forcePlayTrigger]);


  const handleNext = useCallback((isAutoSkip = false) => {
    if (!currentTracks.length) return;
    
    // Safety for infinite skip loop
    if (isAutoSkip && currentTracks.every(t => t.isValid === false)) {
      setIsPlaying(false);
      setErrorMsg("All tracks in this playlist are invalid.");
      return;
    }

    let nextIndex;
    if (isShuffle) {
      // Pick random
      do {
        nextIndex = Math.floor(Math.random() * currentTracks.length);
      } while (nextIndex === currentTrackIndex && currentTracks.length > 1);
    } else {
      nextIndex = (currentTrackIndex + 1) % currentTracks.length;
    }
    playTrack(nextIndex);
  }, [currentTrackIndex, isShuffle, currentTracks, playTrack]);

  const handlePrev = useCallback(() => {
    if (!currentTracks.length) return;
    if (audioRef.current && audioRef.current.currentTime > 3) {
      audioRef.current.currentTime = 0;
      return;
    }
    let prevIndex;
    if (isShuffle) {
       prevIndex = Math.floor(Math.random() * currentTracks.length);
    } else {
      prevIndex = (currentTrackIndex - 1 + currentTracks.length) % currentTracks.length;
    }
    playTrack(prevIndex);
  }, [currentTrackIndex, isShuffle, currentTracks, playTrack]);

  // --- Effects ---

  // Sync audio source
  useEffect(() => {
    if (currentTracks.length > 0 && audioRef.current) {
      const track = currentTracks[currentTrackIndex];
      // Only update src if different to prevent reloading on minor re-renders
      const currentSrc = audioRef.current.getAttribute('src');
      if (currentSrc !== track.url) {
        audioRef.current.src = track.url;
        audioRef.current.load();
        if (isPlaying) {
            const playPromise = audioRef.current.play();
            if (playPromise !== undefined) {
                playPromise.catch(e => console.log("Auto-play prevented"));
            }
        }
      }
    }
  }, [currentTrackIndex, currentTracks]);

  // Handle Play/Pause toggle state
  useEffect(() => {
    if (audioRef.current) {
        if (isPlaying) audioRef.current.play().catch(() => {});
        else audioRef.current.pause();
    }
  }, [isPlaying]);

  const handlePlayPause = () => {
    if (!activePlaylist) return;
    setIsPlaying(!isPlaying);
  };

  const handleTimeUpdate = () => {
    if (audioRef.current && !isDragging) {
      setCurrentTime(audioRef.current.currentTime);
    }
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const time = parseFloat(e.target.value);
    setCurrentTime(time);
    if (audioRef.current) {
      audioRef.current.currentTime = time;
    }
  };

  if (!activePlaylist) {
    return (
      <div className="h-full w-full flex items-center justify-center text-slate-500 font-mono text-sm p-8 text-center">
        Select a playlist from the right to start listening.
      </div>
    );
  }

  const currentTrack: Track = currentTracks[currentTrackIndex] || { 
    id: 'unknown',
    url: '',
    fileName: 'Unknown', 
    type: 'audio/mp3',
    isValid: true
  };

  return (
    <div className="h-full w-full flex items-center justify-center p-4 md:p-8 select-none relative">
      <audio
        ref={audioRef}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={() => audioRef.current && setDuration(audioRef.current.duration)}
        onEnded={() => handleNext()}
        onError={() => handleNext(true)}
        crossOrigin="anonymous" 
      />

      {errorMsg && (
        <div className="absolute top-4 bg-red-500/90 text-white px-4 py-2 rounded-lg text-xs z-50 animate-pulse">
            {errorMsg}
        </div>
      )}

      <div className="w-full max-w-md bg-surface/50 backdrop-blur-xl border border-white/10 rounded-3xl shadow-2xl overflow-hidden p-6 md:p-8 flex flex-col items-center gap-8">
        
        {/* Header */}
        <div className="w-full flex justify-between items-center text-slate-400 text-xs tracking-wider uppercase">
          <span className="truncate max-w-[150px]">{activePlaylist.name}</span>
          <div className="flex items-center gap-1">
             <div className={`w-2 h-2 rounded-full ${isPlaying ? 'bg-green-500 animate-pulse' : 'bg-slate-600'}`}></div>
             <span>{currentTrack.type?.split('/')[1]?.toUpperCase() || 'MP3'}</span>
          </div>
        </div>

        {/* CD / Disc Visualization */}
        <div className="relative w-64 h-64 md:w-72 md:h-72 flex-shrink-0">
          <div className={`w-full h-full rounded-full shadow-2xl shadow-black/50 border-4 border-slate-800 relative overflow-hidden transition-transform duration-[2000ms] ${isPlaying ? 'animate-[spin_4s_linear_infinite]' : ''}`}
               style={{ animationPlayState: isPlaying ? 'running' : 'paused' }}
          >
             <div className="absolute inset-0 bg-[conic-gradient(from_0deg,#333,#111,#333,#111,#333)] opacity-80"></div>
             <div className="absolute inset-0 bg-[conic-gradient(transparent,rgba(255,255,255,0.1),transparent,rgba(255,255,255,0.1),transparent)] rotate-45"></div>
             <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-24 h-24 bg-gradient-to-br from-primary to-indigo-900 rounded-full border-2 border-slate-700/50"></div>
             <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-4 h-4 bg-black rounded-full border border-slate-700"></div>
          </div>
        </div>

        {/* Info */}
        <div className="w-full text-center">
             <h2 className="text-white font-medium text-lg truncate px-4">{currentTrack.fileName.replace(/\.[^/.]+$/, "")}</h2>
             {currentTrack.isValid === false && <span className="text-red-400 text-xs flex items-center justify-center gap-1 mt-1"><AlertTriangle size={10}/> File Missing</span>}
        </div>

        {/* Controls Container */}
        <div className="w-full flex flex-col gap-6 mt-auto">
          {/* Progress Bar */}
          <div className="flex flex-col gap-2">
            <input
              type="range"
              min="0"
              max={duration || 100}
              value={currentTime}
              onChange={handleSeek}
              onMouseDown={() => setIsDragging(true)}
              onMouseUp={() => setIsDragging(false)}
              onTouchStart={() => setIsDragging(true)}
              onTouchEnd={() => setIsDragging(false)}
              className="w-full h-1.5 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-primary hover:accent-indigo-400 transition-all"
            />
            <div className="flex justify-between text-xs font-mono text-slate-400">
              <span>{formatTime(currentTime)}</span>
              <span>{formatTime(duration)}</span>
            </div>
          </div>

          {/* Main Controls */}
          <div className="flex items-center justify-between px-4">
            <button 
              onClick={() => setIsShuffle(!isShuffle)}
              className={`p-3 rounded-full transition-colors ${isShuffle ? 'text-primary bg-primary/10' : 'text-slate-500 hover:text-slate-300'}`}
            >
              {isShuffle ? <Shuffle size={20} /> : <Repeat size={20} />}
            </button>
            <button 
              onClick={handlePrev}
              className="p-3 text-slate-200 hover:text-white hover:bg-white/5 rounded-full transition-all active:scale-90"
            >
              <SkipBack size={28} fill="currentColor" />
            </button>
            <button 
              onClick={handlePlayPause}
              className="w-16 h-16 flex items-center justify-center bg-primary text-white rounded-full shadow-lg shadow-indigo-500/40 hover:bg-indigo-500 transition-all active:scale-95"
            >
              {isPlaying ? <Pause size={32} fill="currentColor" /> : <Play size={32} fill="currentColor" className="ml-1" />}
            </button>
            <button 
              onClick={() => handleNext()}
              className="p-3 text-slate-200 hover:text-white hover:bg-white/5 rounded-full transition-all active:scale-90"
            >
              <SkipForward size={28} fill="currentColor" />
            </button>
            <div className="w-11"></div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AudioPlayerPage;