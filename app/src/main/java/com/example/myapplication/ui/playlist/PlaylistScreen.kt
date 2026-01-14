package com.example.myapplication.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.database.entity.PlaylistEntity
import com.example.myapplication.data.database.entity.SongEntity
import com.example.myapplication.data.model.Song
import com.example.myapplication.playback.PlaybackStateManager
import com.example.myapplication.ui.util.DraggableItem
import com.example.myapplication.ui.util.rememberReorderableLazyListState
import com.example.myapplication.ui.util.reorderable

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    playlistViewModel: PlaylistViewModel = viewModel(),
    onNavigateToAllSongs: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by playlistViewModel.uiState.collectAsState()
    val activePlaylistId by PlaybackStateManager.activePlaylistId.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val playlistListState = rememberLazyListState()
    val reorderablePlaylistState = rememberReorderableLazyListState(playlistListState) { from, to ->
        playlistViewModel.onPlaylistMoved(from, to)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            PlaylistTopAppBar(uiState, playlistViewModel, onNavigateToAllSongs, onNavigateToSettings)
        },
        floatingActionButton = {
            if (uiState.isEditMode && !uiState.isInMultiSelectMode) {
                FloatingActionButton(onClick = { playlistViewModel.onDialogStateChange(PlaylistDialogState.ShowCreate) }) {
                    Icon(Icons.Default.Add, contentDescription = "创建歌单")
                }
            }
        }
    ) { innerPadding ->

        HandleDialogs(uiState = uiState, viewModel = playlistViewModel)

        LazyColumn(
            state = playlistListState,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .let { if (uiState.isEditMode && !uiState.isInMultiSelectMode) it.reorderable(reorderablePlaylistState, coroutineScope) else it }
        ) {
            itemsIndexed(uiState.playlists, key = { _, item -> item.playlist.playlistId }) { index, playlistWithSongs ->
                DraggableItem(reorderablePlaylistState, index, Modifier.fillMaxWidth()) { isDragging ->
                    PlaylistRow(playlistViewModel, uiState, playlistWithSongs, isDragging, activePlaylistId == playlistWithSongs.playlist.playlistId)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistTopAppBar(
    uiState: PlaylistUiState, 
    viewModel: PlaylistViewModel, 
    onNavigateToAllSongs: () -> Unit, 
    onNavigateToSettings: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("歌单管理") },
        actions = {
            if (uiState.isEditMode && uiState.isInMultiSelectMode) {
                IconButton(onClick = { viewModel.deleteSelectedPlaylists() }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除选中")
                }
            }
            IconButton(onClick = { onNavigateToAllSongs() }) {
                Icon(Icons.Default.Info, contentDescription = "所有歌曲")
            }
            IconButton(onClick = { onNavigateToSettings() }) {
                Icon(Icons.Default.Settings, contentDescription = "设置")
            }
            IconButton(onClick = { viewModel.onEditModeChange(!uiState.isEditMode) }) {
                Icon(Icons.Default.Edit, contentDescription = "编辑模式")
            }
            if (uiState.isEditMode) {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("多选") }, onClick = { 
                        viewModel.toggleMultiSelectMode()
                        showMenu = false 
                    })
                }
            }
        }
    )
}

@Composable
private fun PlaylistRow(
    playlistViewModel: PlaylistViewModel,
    uiState: PlaylistUiState,
    playlistWithSongs: com.example.myapplication.data.database.relation.PlaylistWithSongs,
    isDragging: Boolean,
    isActive: Boolean
) {
    val isSelected = uiState.selectedPlaylistIds.contains(playlistWithSongs.playlist.playlistId)

    Column(
        modifier = Modifier
            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .clickable(enabled = uiState.isInMultiSelectMode) { playlistViewModel.togglePlaylistSelection(playlistWithSongs.playlist.playlistId) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { if (!uiState.isEditMode) playlistViewModel.onPlaylistExpanded(playlistWithSongs.playlist.playlistId) }) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "正在播放", modifier = Modifier.padding(end = 8.dp))
            }
            if (uiState.isInMultiSelectMode) {
                Checkbox(checked = isSelected, onCheckedChange = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = playlistWithSongs.playlist.name, fontSize = 18.sp)
                Text(text = "${playlistWithSongs.songs.size} 首歌", fontSize = 14.sp)
            }
            if (uiState.isEditMode && !uiState.isInMultiSelectMode) {
                Row {
                    IconButton(onClick = { playlistViewModel.onDialogStateChange(PlaylistDialogState.ShowRename(playlistWithSongs.playlist)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "重命名")
                    }
                    IconButton(onClick = { playlistViewModel.onDialogStateChange(PlaylistDialogState.ShowDeleteConfirm(playlistWithSongs.playlist)) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            } else if (!uiState.isEditMode) {
                IconButton(onClick = { PlaybackStateManager.setActivePlaylist(playlistWithSongs.playlist.playlistId) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                }
            }
        }

        AnimatedVisibility(visible = !uiState.isInMultiSelectMode && uiState.expandedPlaylistId == playlistWithSongs.playlist.playlistId) {
            ExpandedPlaylistContent(playlistViewModel, uiState, playlistWithSongs)
        }
        HorizontalDivider()
    }
}

@Composable
private fun ExpandedPlaylistContent(
    playlistViewModel: PlaylistViewModel,
    uiState: PlaylistUiState,
    playlistWithSongs: com.example.myapplication.data.database.relation.PlaylistWithSongs
) {
    val coroutineScope = rememberCoroutineScope()
    val songListState = rememberLazyListState()
    val reorderableSongState = rememberReorderableLazyListState(songListState) { from, to ->
        playlistViewModel.onSongMoved(playlistWithSongs.playlist.playlistId, from, to)
    }

    LazyColumn(
        state = songListState,
        modifier = Modifier
            .height(200.dp) 
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .let { if (uiState.isEditMode) it.reorderable(reorderableSongState, coroutineScope) else it }
    ) {
        itemsIndexed(playlistWithSongs.songs, key = { _, song -> song.id }) { index, song ->
            DraggableItem(reorderableSongState, index, Modifier.fillMaxWidth()) { isDragging ->
                SongRow(playlistViewModel, uiState, playlistWithSongs.playlist.playlistId, song, isDragging)
            }
        }
        if (uiState.isEditMode) {
            item {
                Button(onClick = { playlistViewModel.onDialogStateChange(PlaylistDialogState.ShowAddSongs(playlistWithSongs.playlist.playlistId)) }) {
                    Text("添加歌曲")
                }
            }
        }
    }
}

@Composable
private fun SongRow(viewModel: PlaylistViewModel, uiState: PlaylistUiState, playlistId: Long, song: SongEntity, isDragging: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = song.title, modifier = Modifier.weight(1f))
        if (uiState.isEditMode) {
            IconButton(onClick = { viewModel.removeSongFromPlaylist(playlistId, song.id) }) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "从歌单移除")
            }
        }
    }
}

@Composable
private fun HandleDialogs(uiState: PlaylistUiState, viewModel: PlaylistViewModel) {
    when (val dialogState = uiState.dialogState) {
        is PlaylistDialogState.ShowCreate -> CreatePlaylistDialog(onDismiss = { viewModel.onDialogStateChange(PlaylistDialogState.Hidden) }, onCreate = viewModel::createPlaylist)
        is PlaylistDialogState.ShowRename -> RenamePlaylistDialog(playlist = dialogState.playlist, onDismiss = { viewModel.onDialogStateChange(PlaylistDialogState.Hidden) }, onRename = { newName -> viewModel.updatePlaylistName(dialogState.playlist, newName) })
        is PlaylistDialogState.ShowDeleteConfirm -> DeleteConfirmDialog(playlist = dialogState.playlist, onDismiss = { viewModel.onDialogStateChange(PlaylistDialogState.Hidden) }, onConfirm = { viewModel.deletePlaylist(dialogState.playlist) })
        is PlaylistDialogState.ShowAddSongs -> AddSongsToPlaylistDialog(localSongs = uiState.localSongs, onDismiss = { viewModel.onDialogStateChange(PlaylistDialogState.Hidden) }, onAdd = { songs -> viewModel.addSongsToPlaylist(dialogState.playlistId, songs) })
        PlaylistDialogState.Hidden -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongsToPlaylistDialog(localSongs: List<Song>, onDismiss: () -> Unit, onAdd: (List<Song>) -> Unit) {
    val (selectedSongs, setSelectedSongs) = remember { mutableStateOf<Set<Song>>(emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Scaffold {
            Column(modifier = Modifier.padding(it)){
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items = localSongs, key = { it.id }) { song ->
                        Row(
                            modifier = Modifier.clickable { 
                                val newSelection = selectedSongs.toMutableSet()
                                if (newSelection.contains(song)) newSelection.remove(song) else newSelection.add(song)
                                setSelectedSongs(newSelection)
                             }.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selectedSongs.contains(song), onCheckedChange = null)
                            Text(text = song.title, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onAdd(selectedSongs.toList()) }, enabled = selectedSongs.isNotEmpty()) { Text("添加") }
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建新歌单") },
        text = { TextField(value = text, onValueChange = { text = it }, label = { Text("歌单名称") }) },
        confirmButton = { Button(onClick = { if (text.isNotBlank()) onCreate(text) }, enabled = text.isNotBlank()) { Text("创建") } },
        dismissButton = { Button(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RenamePlaylistDialog(playlist: PlaylistEntity, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var text by remember { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名歌单") },
        text = { TextField(value = text, onValueChange = { text = it }, label = { Text("新名称") }) },
        confirmButton = { Button(onClick = { if (text.isNotBlank()) onRename(text) }, enabled = text.isNotBlank()) { Text("确认") } },
        dismissButton = { Button(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DeleteConfirmDialog(playlist: PlaylistEntity, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("您确定要删除歌单 \"${playlist.name}\" 吗？此操作无法撤销。") },
        confirmButton = { Button(onClick = onConfirm) { Text("删除") } },
        dismissButton = { Button(onClick = onDismiss) { Text("取消") } }
    )
}
