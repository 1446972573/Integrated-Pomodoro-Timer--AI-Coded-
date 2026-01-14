# ZenTimer & Audio Player Architecture Documentation

## Overview
ZenTimer is a hybrid web application combining a minimalist countdown timer and a feature-rich audio playlist manager. Built with **React 19**, **TypeScript**, and **Tailwind CSS**, it features a swipe-based multi-page interface, persistent state management, and a high-fidelity audio player visualization.

## Architecture

### 1. State Management (`App.tsx`)
The application uses a "Lifted State" pattern. `App.tsx` acts as the single source of truth for the playlist data, ensuring consistency between the Playlist Manager and the Audio Player.
*   **Playlists**: Stored in `App` state and synchronized with `localStorage` every 60 seconds (unless in edit mode).
*   **Navigation**: Uses a horizontal scrolling container with snap points. Logic is included to "lock" scrolling when the user is performing drag-and-drop operations in the playlist manager.

### 2. File System Abstraction (`MusicFileSave`)
To prepare this web-based prototype for native deployment (e.g., Electron, React Native), we abstract the file system path using the `MusicFileSave` variable.

*   **Location**: Defined in `types.ts`.
*   **Type**: `string`.
*   **Current Value**: `"user://documents/music/zen_timer_data/"` (Mock Protocol).
*   **Implementation Role**:
    *   In the current web version, this variable is informational.
    *   In a **Native Implementation**, this variable will hold the absolute path to the user's music directory or a SQLite connection string.
    *   **Binding**: When porting to Electron, `MusicFileSave` would be dynamically assigned via `ipcRenderer` fetching `app.getPath('music')`.
    *   **Usage**: File I/O services would import `MusicFileSave` to prefix all relative track paths (`Track.url`) before attempting to load audio data.

### 3. Playlist Logic (`PlaylistManagerPage.tsx`)
*   **Modes**: The component switches between `list`, `edit-list`, `single`, and `edit-single` modes.
*   **Editing**: 
    *   Deep copies of objects (`tempPlaylist`) are used during editing to allow for "Cancel" operations.
    *   **Naming Collision**: An algorithm ensures no two playlists have the same name by appending numeric suffixes (e.g., "Chill Vibes 1").
*   **Drag & Drop**:
    *   Implemented using standard React state array manipulation.
    *   Compatible with both **Touch** (mobile) and **Mouse** (desktop) input methods via `onMouseDown`/`onTouchStart` triggers and `onMouseEnter` swapping logic.

### 4. Audio Playback (`AudioPlayerPage.tsx`)
*   **Validation**: Before playing any track, the player checks `checkFileExists()`.
*   **Skip Logic**: If a file is missing (`isValid: false`), it recursively calls `handleNext()` until a valid track is found or the playlist is exhausted.
*   **Visuals**: A CSS-based rotating disc animation (`conic-gradient`) simulates a physical record player.

## Data Structures (`types.ts`)

```typescript
export interface Track {
  id: string;
  url: string;      // Web URL or local file:// path
  fileName: string; // Display name (Unicode supported)
  type: string;     // MIME type
  isValid?: boolean; // Runtime check status
}

export interface Playlist {
  id: string;
  name: string;
  tracks: Track[];
}
```

## Attribute Information System

The application now includes a dynamic property display system located in the "Info" popup (accessible via the `(i)` button in the Playlist Manager).

### 1. `MusicFileSave` Variable
*   **Role**: Top-level Configuration.
*   **Display Logic**: This variable is **pinned to the top** of the properties popup. It appears independently of the user-editable HTML content to ensure the critical storage path is always visible first.
*   **Implementation**: It is imported directly from `types.ts` and rendered in a dedicated, styled container above the attribute content.

### 2. `Attribute.html` Integration
The system renders the content of an external `Attribute.html` file below the `MusicFileSave` variable.

*   **File Path**: `./Attribute.html` (Relative to project root).
*   **Called By**: `components/PlaylistManagerPage.tsx`.
*   **Implementation Details**:
    *   **Fetch**: When the Info popup is opened (`showInfoPopup: true`), a `useEffect` hook triggers a `fetch('Attribute.html')` request.
    *   **Rendering**: The text response is sanitized and rendered using React's `dangerouslySetInnerHTML`.
    *   **Encoding**: Supports UTF-8/Unicode (e.g., Chinese characters, Emojis) natively.
    *   **Styling**:
        *   Uses the default system sans-serif font (`font-family: sans-serif` in container).
        *   Supports basic HTML tags (`<b>`, `<strong>`, `<br/>`, `<p>`, `<span>`) for formatting (bolding, color, line breaks).
        *   Font sizes can be adjusted via inline CSS within `Attribute.html`.

**Internal Format Example (`Attribute.html`):**
```html
<div>
  <p><strong>Version:</strong> 1.0</p>
  <p>Unicode Test: 中文测试</p>
</div>
```

## Future Native Implementation Plan

1.  **Replace Audio Element**: Switch HTML5 `<audio>` to `react-native-track-player` (mobile) or `Howler.js` (desktop).
2.  **Real I/O**: Replace `GLOBAL_TRACK_POOL` with a recursive directory scan rooted at `MusicFileSave`.
3.  **Persistence**: Replace `localStorage` with `SQLite` or JSON file writes to `MusicFileSave/playlists.json`.
