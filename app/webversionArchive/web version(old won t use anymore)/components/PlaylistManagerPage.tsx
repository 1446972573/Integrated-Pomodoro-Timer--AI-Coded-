import React, { useState, useEffect, useRef } from 'react';
import { 
  Folder, Play, Edit2, Save, Trash2, Plus, 
  ArrowLeft, FileAudio, GripVertical, Check, AlertCircle, X, Info 
} from 'lucide-react';
import { Playlist, Track, GLOBAL_TRACK_POOL, MusicFileSave } from '../types';

interface PlaylistManagerPageProps {
  playlists: Playlist[];
  setPlaylists: (playlists: Playlist[]) => void;
  onPlayRequest: (playlistId: string) => void;
  setScrollLocked: (locked: boolean) => void;
}

const PlaylistManagerPage: React.FC<PlaylistManagerPageProps> = ({ 
  playlists, 
  setPlaylists, 
  onPlayRequest,
  setScrollLocked
}) => {
  // Modes: 
  // 'list' = View all playlists
  // 'edit-list' = Reorder/Delete playlists
  // 'single' = View one playlist tracks
  // 'edit-single' = Edit one playlist (rename, reorder tracks, add/remove tracks)
  const [mode, setMode] = useState<'list' | 'edit-list' | 'single' | 'edit-single'>('list');
  const [selectedPlaylistId, setSelectedPlaylistId] = useState<string | null>(null);
  
  // Temporary State for editing
  const [tempPlaylist, setTempPlaylist] = useState<Playlist | null>(null);
  const [tempPlaylistsOrder, setTempPlaylistsOrder] = useState<Playlist[]>([]);
  
  // Input Ref for renaming
  const renameInputRef = useRef<HTMLInputElement>(null);

  // Popups
  const [showInvalidPopup, setShowInvalidPopup] = useState(false);
  const [invalidPopupSeen, setInvalidPopupSeen] = useState<Set<string>>(new Set());
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null); // playlist ID
  const [showTrackSelector, setShowTrackSelector] = useState(false);
  const [showInfoPopup, setShowInfoPopup] = useState(false);
  
  // Attribute.html Content
  const [attributeContent, setAttributeContent] = useState<string>('<p>Loading attributes...</p>');

  // Lock scroll when in any edit mode
  useEffect(() => {
    setScrollLocked(mode === 'edit-list' || mode === 'edit-single' || showTrackSelector);
  }, [mode, showTrackSelector, setScrollLocked]);

  // Load Attribute.html when info popup opens
  useEffect(() => {
    if (showInfoPopup) {
      fetch('Attribute.html')
        .then(response => {
          if (!response.ok) throw new Error("Attribute file missing");
          return response.text();
        })
        .then(text => setAttributeContent(text))
        .catch(err => {
          console.error(err);
          setAttributeContent('<p style="color: #f87171;">Failed to load Attribute.html</p>');
        });
    }
  }, [showInfoPopup]);

  // --- Helpers ---

  const getUniqueName = (baseName: string, existingNames: string[], excludeId?: string): string => {
    let name = baseName.trim() || '1';
    let suffix = '1';
    
    // Check collision against playlists other than self
    const isCollision = (n: string) => existingNames.includes(n);
    
    while (isCollision(name)) {
      name = `${name}${suffix}`;
    }
    return name;
  };

  // --- Handlers: List View ---

  const handleOpenPlaylist = (playlist: Playlist) => {
    if (mode === 'edit-list') return;
    
    setSelectedPlaylistId(playlist.id);
    setMode('single');

    // Check invalid tracks
    const hasInvalid = playlist.tracks.some(t => t.isValid === false);
    if (hasInvalid && !invalidPopupSeen.has(playlist.id)) {
      setShowInvalidPopup(true);
      setInvalidPopupSeen(prev => new Set(prev).add(playlist.id));
    }
  };

  const startEditList = () => {
    setTempPlaylistsOrder([...playlists]);
    setMode('edit-list');
  };

  const saveEditList = () => {
    setPlaylists(tempPlaylistsOrder);
    setMode('list');
  };

  const confirmDeletePlaylist = (id: string) => {
    setShowDeleteConfirm(id);
  };

  const executeDeletePlaylist = () => {
    if (showDeleteConfirm) {
      setTempPlaylistsOrder(prev => prev.filter(p => p.id !== showDeleteConfirm));
      setShowDeleteConfirm(null);
    }
  };

  // --- Handlers: Single View ---

  const startEditSingle = () => {
    const pl = playlists.find(p => p.id === selectedPlaylistId);
    if (pl) {
      setTempPlaylist(JSON.parse(JSON.stringify(pl))); // Deep copy
      setMode('edit-single');
    }
  };

  const saveEditSingle = () => {
    if (!tempPlaylist) return;

    // Handle Rename Logic
    const existingNames = playlists
      .filter(p => p.id !== tempPlaylist.id)
      .map(p => p.name);
    
    const finalName = getUniqueName(tempPlaylist.name, existingNames);
    const finalPlaylist = { ...tempPlaylist, name: finalName };

    // Update Global State
    setPlaylists(playlists.map(p => p.id === finalPlaylist.id ? finalPlaylist : p));
    
    // Update Local selection to reflect changes
    setSelectedPlaylistId(finalPlaylist.id);
    setMode('single');
    setTempPlaylist(null);
  };

  const exitSingleView = () => {
    setMode('list');
    setSelectedPlaylistId(null);
  };

  // --- Drag & Drop (Simple Implementation) ---
  
  const [draggedItemIndex, setDraggedItemIndex] = useState<number | null>(null);

  const onDragStart = (index: number) => {
    setDraggedItemIndex(index);
  };

  const onDragEnter = (index: number, listType: 'playlists' | 'tracks') => {
    if (draggedItemIndex === null || draggedItemIndex === index) return;
    
    if (listType === 'playlists') {
      const newList = [...tempPlaylistsOrder];
      const item = newList[draggedItemIndex];
      newList.splice(draggedItemIndex, 1);
      newList.splice(index, 0, item);
      setTempPlaylistsOrder(newList);
      setDraggedItemIndex(index);
    } else if (listType === 'tracks' && tempPlaylist) {
      const newTracks = [...tempPlaylist.tracks];
      const item = newTracks[draggedItemIndex];
      newTracks.splice(draggedItemIndex, 1);
      newTracks.splice(index, 0, item);
      setTempPlaylist({ ...tempPlaylist, tracks: newTracks });
      setDraggedItemIndex(index);
    }
  };

  const onDragEnd = () => {
    setDraggedItemIndex(null);
  };

  // --- Renderers ---

  const renderInfoPopup = () => {
    if (!showInfoPopup) return null;
    
    return (
      <div className="absolute inset-0 z-[60] flex items-center justify-center bg-black/80 backdrop-blur-md p-6 animate-in fade-in duration-200" onClick={() => setShowInfoPopup(false)}>
        <div className="bg-surface border border-white/10 rounded-3xl p-6 w-full max-w-sm shadow-2xl relative flex flex-col max-h-[80vh]" onClick={e => e.stopPropagation()}>
          <div className="flex items-center justify-between mb-4 flex-shrink-0">
            <h3 className="text-xl font-bold flex items-center gap-2">
              <Info size={24} className="text-primary" />
              Properties
            </h3>
            <button onClick={() => setShowInfoPopup(false)} className="p-2 hover:bg-white/10 rounded-full">
              <X size={20} />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto no-scrollbar space-y-4 pr-1">
            
            {/* 1. MusicFileSave - Pinned Top Variable */}
            <div className="bg-indigo-500/10 border border-indigo-500/20 rounded-xl p-4 space-y-2 shrink-0">
               <h4 className="text-xs font-bold text-indigo-400 uppercase tracking-widest flex items-center gap-2">
                 <span>📂 Path Variable</span>
                 <span className="bg-indigo-500 text-white text-[9px] px-1.5 py-0.5 rounded">MusicFileSave</span>
               </h4>
               <div className="text-[11px] font-mono text-slate-300 bg-black/30 p-3 rounded-lg break-all border border-white/5">
                 {MusicFileSave}
               </div>
            </div>

            {/* 2. Attribute.html Content - Rendered Below */}
            <div className="bg-background/50 rounded-xl p-4">
              <h4 className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3 border-b border-white/5 pb-2">
                External Attributes
              </h4>
              <div 
                className="text-sm text-slate-300 [&_strong]:text-white [&_strong]:font-bold"
                dangerouslySetInnerHTML={{ __html: attributeContent }} 
              />
            </div>

          </div>
        </div>
      </div>
    );
  };

  const renderTrackSelector = () => {
    if (!showTrackSelector || !tempPlaylist) return null;
    
    const toggleTrack = (track: Track) => {
       const exists = tempPlaylist.tracks.some(t => t.id === track.id);
       if (exists) return; // "不可重复" (Cannot duplicate)
       setTempPlaylist({
         ...tempPlaylist,
         tracks: [...tempPlaylist.tracks, track]
       });
    };

    return (
      <div className="absolute inset-0 z-50 bg-background/95 backdrop-blur-md flex flex-col animate-in slide-in-from-bottom duration-300">
        <div className="p-4 border-b border-white/10 flex justify-between items-center">
          <h3 className="font-bold text-white">Add Songs</h3>
          <button onClick={() => setShowTrackSelector(false)} className="p-2 bg-white/10 rounded-full">
            <Check size={20} />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-4 space-y-2">
           {GLOBAL_TRACK_POOL.map(track => {
             const isAdded = tempPlaylist.tracks.some(t => t.id === track.id);
             return (
               <div key={track.id} 
                    onClick={() => !isAdded && toggleTrack(track)}
                    className={`p-3 rounded-xl border flex justify-between items-center ${isAdded ? 'border-primary/50 bg-primary/10 opacity-50' : 'border-white/5 bg-surface hover:bg-white/5 cursor-pointer'}`}>
                  <div className="flex items-center gap-3 overflow-hidden">
                    <FileAudio size={18} className="text-slate-400 shrink-0" />
                    <span className="truncate text-sm">{track.fileName}</span>
                  </div>
                  {isAdded && <Check size={16} className="text-primary" />}
               </div>
             );
           })}
        </div>
      </div>
    );
  };

  const renderDeleteConfirm = () => {
    if (!showDeleteConfirm) return null;
    return (
      <div className="absolute inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
        <div className="bg-surface border border-white/10 rounded-2xl p-6 w-full max-w-sm shadow-2xl text-center">
          <h3 className="text-xl font-bold mb-2">Delete Playlist?</h3>
          <p className="text-slate-400 text-sm mb-6">This action cannot be undone.</p>
          <div className="flex gap-4">
            <button onClick={() => setShowDeleteConfirm(null)} className="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10">Cancel</button>
            <button onClick={executeDeletePlaylist} className="flex-1 py-3 rounded-xl bg-red-500/80 hover:bg-red-500 text-white">Delete</button>
          </div>
        </div>
      </div>
    );
  };

  const renderInvalidPopup = () => {
    if (!showInvalidPopup) return null;
    return (
      <div className="absolute inset-x-4 top-4 z-40 bg-orange-500/90 text-white p-4 rounded-xl shadow-lg flex items-start gap-3 backdrop-blur-md animate-in slide-in-from-top duration-300">
        <AlertCircle size={24} className="shrink-0 mt-0.5" />
        <div className="flex-1 text-sm">
          <p className="font-bold">Missing Files</p>
          <p className="opacity-90">Some songs in this playlist are no longer available and will be skipped.</p>
        </div>
        <button onClick={() => setShowInvalidPopup(false)} className="shrink-0 p-1 hover:bg-black/20 rounded">
          <span className="text-xs font-bold border border-white/40 px-2 py-1 rounded">CLOSE</span>
        </button>
      </div>
    );
  };

  // --- Main Render ---

  return (
    <div 
      className="h-full w-full bg-background relative overflow-hidden flex flex-col select-none"
      onMouseUp={onDragEnd}
      onMouseLeave={onDragEnd}
    >
      {renderInfoPopup()}
      {renderDeleteConfirm()}
      {renderTrackSelector()}
      {renderInvalidPopup()}

      {/* Header */}
      <div className="h-16 shrink-0 border-b border-white/5 flex items-center justify-between px-4 bg-surface/30 backdrop-blur-md z-10">
        <div className="flex items-center gap-3">
          {(mode === 'single' || mode === 'edit-single') && (
            <button onClick={mode === 'edit-single' ? undefined : exitSingleView} disabled={mode === 'edit-single'} className={`p-2 rounded-full hover:bg-white/5 ${mode === 'edit-single' ? 'opacity-30' : ''}`}>
              <ArrowLeft size={20} />
            </button>
          )}
          <h2 className="font-bold text-lg truncate max-w-[200px]">
             {mode === 'list' || mode === 'edit-list' ? 'My Playlists' : (tempPlaylist?.name || playlists.find(p => p.id === selectedPlaylistId)?.name)}
          </h2>
        </div>

        <div className="flex items-center gap-2">
          {/* Info Button (Always Visible) */}
          <button onClick={() => setShowInfoPopup(true)} className="p-2 text-slate-400 hover:text-white hover:bg-white/5 rounded-full">
            <Info size={18} />
          </button>

          {/* Top Right Buttons Logic */}
          {mode === 'list' && (
            <button onClick={startEditList} className="p-2 bg-white/5 hover:bg-white/10 rounded-full text-slate-300">
              <Edit2 size={18} />
            </button>
          )}

          {mode === 'edit-list' && (
            <>
              <button onClick={() => {
                 const newId = `pl-${Date.now()}`;
                 setTempPlaylistsOrder(prev => [...prev, { id: newId, name: `New Playlist`, tracks: [] }]);
              }} className="p-2 bg-primary/20 text-primary rounded-full mr-2">
                 <Plus size={18} />
              </button>
              <button onClick={saveEditList} className="p-2 bg-primary text-white rounded-full shadow-lg shadow-primary/30">
                <Save size={18} />
              </button>
            </>
          )}

          {mode === 'single' && (
            <>
              <button onClick={() => onPlayRequest(selectedPlaylistId!)} className="p-2 bg-primary text-white rounded-full mr-1 hover:scale-105 transition-transform">
                <Play size={18} fill="currentColor" />
              </button>
              <button onClick={startEditSingle} className="p-2 bg-white/5 hover:bg-white/10 rounded-full text-slate-300">
                <Edit2 size={18} />
              </button>
            </>
          )}

          {mode === 'edit-single' && (
            <button onClick={saveEditSingle} className="p-2 bg-primary text-white rounded-full shadow-lg shadow-primary/30">
              <Save size={18} />
            </button>
          )}
        </div>
      </div>

      {/* Content Area */}
      <div className="flex-1 overflow-y-auto no-scrollbar p-4">
        
        {/* VIEW: ALL PLAYLISTS */}
        {(mode === 'list' || mode === 'edit-list') && (
          <div className="space-y-3">
            {(mode === 'edit-list' ? tempPlaylistsOrder : playlists).map((playlist, index) => (
              <div 
                key={playlist.id}
                onClick={() => handleOpenPlaylist(playlist)}
                onMouseDown={() => mode === 'edit-list' && onDragStart(index)}
                onTouchStart={() => mode === 'edit-list' && onDragStart(index)}
                onDragOver={(e) => e.preventDefault()}
                onMouseEnter={() => mode === 'edit-list' && onDragEnter(index, 'playlists')}
                onTouchMove={(e) => {}}
                className={`
                  relative group p-4 rounded-2xl border transition-all active:scale-[0.98]
                  ${mode === 'list' ? 'bg-surface/50 border-white/5 hover:border-white/10 cursor-pointer' : 'bg-surface/80 border-primary/30 cursor-grab'}
                  ${draggedItemIndex === index && mode === 'edit-list' ? 'opacity-50 scale-95 border-primary' : ''}
                `}
              >
                <div className="flex items-center gap-4">
                  <div className={`p-3 rounded-xl ${mode === 'edit-list' ? 'bg-slate-700' : 'bg-indigo-500/20 text-indigo-400'}`}>
                    {mode === 'edit-list' ? <GripVertical size={20} /> : <Folder size={20} />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-white truncate">{playlist.name}</h3>
                    <p className="text-xs text-slate-500">{playlist.tracks.length} songs</p>
                  </div>
                  
                  {mode === 'edit-list' && (
                    <button 
                      onClick={(e) => { e.stopPropagation(); confirmDeletePlaylist(playlist.id); }}
                      onMouseDown={(e) => e.stopPropagation()} 
                      className="p-2 text-red-400 hover:bg-red-500/10 rounded-full"
                    >
                      <Trash2 size={18} />
                    </button>
                  )}
                </div>
              </div>
            ))}
            {playlists.length === 0 && mode === 'list' && (
               <div className="text-center text-slate-500 mt-10">No playlists. Edit to create one.</div>
            )}
          </div>
        )}

        {/* VIEW: SINGLE PLAYLIST */}
        {(mode === 'single' || mode === 'edit-single') && (
          <div className="space-y-2">
            
            {/* Rename Input */}
            {mode === 'edit-single' && tempPlaylist && (
              <div className="mb-6 animate-in fade-in slide-in-from-top-2">
                <label className="text-xs text-slate-500 uppercase tracking-widest font-bold ml-1 mb-1 block">Playlist Name</label>
                <input 
                  ref={renameInputRef}
                  type="text"
                  value={tempPlaylist.name}
                  onChange={(e) => setTempPlaylist({ ...tempPlaylist, name: e.target.value })}
                  placeholder="Playlist Name"
                  className="w-full bg-surface border border-primary/50 rounded-xl p-4 text-white focus:outline-none focus:ring-2 focus:ring-primary no-drag"
                />
              </div>
            )}

            {/* Tracks List */}
            {((mode === 'edit-single' ? tempPlaylist?.tracks : playlists.find(p => p.id === selectedPlaylistId)?.tracks) || []).map((track, index) => (
               <div 
                 key={track.id}
                 onMouseEnter={() => mode === 'edit-single' && onDragEnter(index, 'tracks')}
                 onMouseDown={() => mode === 'edit-single' && onDragStart(index)}
                 onTouchStart={() => mode === 'edit-single' && onDragStart(index)}
                 className={`
                   relative flex items-center gap-3 p-3 rounded-xl border transition-colors
                   ${mode === 'edit-single' ? 'bg-surface/80 border-dashed border-white/20 cursor-grab' : 'bg-transparent border-transparent hover:bg-white/5'}
                   ${track.isValid === false ? 'opacity-50 grayscale' : ''}
                   ${draggedItemIndex === index && mode === 'edit-single' ? 'opacity-20' : ''}
                 `}
               >
                 {mode === 'edit-single' ? (
                   <GripVertical size={16} className="text-slate-500" />
                 ) : (
                   <span className="text-xs font-mono text-slate-500 w-6 text-center">{index + 1}</span>
                 )}

                 <div className="flex-1 min-w-0">
                   <div className="text-sm font-medium text-slate-200 truncate">{track.fileName}</div>
                   {track.isValid === false && <div className="text-[10px] text-red-400">File not found</div>}
                 </div>

                 {mode === 'edit-single' && (
                    <button 
                      onClick={() => setTempPlaylist(prev => prev ? ({...prev, tracks: prev.tracks.filter(t => t.id !== track.id)}) : null)}
                      className="p-2 text-slate-500 hover:text-red-400"
                    >
                      <X size={16} />
                    </button>
                 )}
               </div>
            ))}

            {mode === 'edit-single' && (
              <button 
                onClick={() => setShowTrackSelector(true)}
                className="w-full py-4 mt-4 border-2 border-dashed border-white/10 rounded-xl text-slate-400 hover:text-white hover:border-white/20 flex items-center justify-center gap-2 transition-colors"
              >
                <Plus size={20} />
                <span>Add Songs</span>
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default PlaylistManagerPage;