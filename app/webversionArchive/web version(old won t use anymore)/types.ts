export enum TimerStatus {
  IDLE = 'IDLE',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED'
}

export interface TimeConfig {
  hours: number;
  minutes: number;
  seconds: number;
}

export interface Track {
  id: string;
  url: string;
  fileName: string; // Supports full Unicode
  type: string;
  isValid?: boolean; // For tracking file existence
}

export interface Playlist {
  id: string;
  name: string;
  tracks: Track[];
}

/**
 * MusicFileSave
 * 
 * This variable is a placeholder for the future implementation of the real file system path.
 * In a production environment (Electron, React Native, etc.), this string would hold
 * the root directory URI or database connection string where audio files and playlist
 * metadata are permanently stored.
 * 
 * Current Type: string
 * Usage: Mock/Placeholder for local storage key or file path root.
 */
export const MusicFileSave: string = "user://documents/music/zen_timer_data/";

// Mock Database of all available files on the "device"
export const GLOBAL_TRACK_POOL: Track[] = [
  { id: 'f1', url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', fileName: 'SoundHelix-Song-1_测试.mp3', type: 'audio/mp3', isValid: true },
  { id: 'f2', url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', fileName: 'SoundHelix-Song-2_東京_City.mp3', type: 'audio/mp3', isValid: true },
  { id: 'f3', url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', fileName: 'SoundHelix-Song-3_🔥_Remix.mp3', type: 'audio/mp3', isValid: true },
  { id: 'f4', url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', fileName: 'Epic_Symphony_No1.mp3', type: 'audio/mp3', isValid: true },
  { id: 'f5', url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', fileName: 'Meditation_Flow_🧘.mp3', type: 'audio/mp3', isValid: true },
  { id: 'f6', url: 'invalid_url_test', fileName: 'Broken_File_❌.mp3', type: 'audio/mp3', isValid: true }, // Intentionally broken for testing
  { id: 'f7', url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', fileName: 'Cyberpunk_Vibes_2077.mp3', type: 'audio/mp3', isValid: true },
];