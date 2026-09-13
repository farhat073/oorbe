package com.oorbitt.launcher.vault

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorbitt.launcher.data.repository.VaultRepository
import com.oorbitt.launcher.model.ChecklistItem
import com.oorbitt.launcher.model.MemoBlock
import com.oorbitt.launcher.model.MemoColor
import com.oorbitt.launcher.model.VaultMemo
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    vaultRepository: VaultRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val memos by vaultRepository.getAllMemos().collectAsState(initial = emptyList())

    var editingMemo by remember { mutableStateOf<VaultMemo?>(null) }
    var confirmDeleteMemo by remember { mutableStateOf<VaultMemo?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Secure Vault", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingMemo = VaultMemo(
                            title = "",
                            blocks = listOf(MemoBlock.Text(id = java.util.UUID.randomUUID().toString(), markdown = ""))
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Memo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (memos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your Vault is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Add encrypted memos to store private notes, checklists, and sensitive records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        editingMemo = VaultMemo(
                            title = "",
                            blocks = listOf(MemoBlock.Text(id = java.util.UUID.randomUUID().toString(), markdown = ""))
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Memo")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = memos,
                        key = { it.id }
                    ) { memo ->
                        MemoCard(
                            memo = memo,
                            onClick = { editingMemo = memo },
                            onPinToggle = {
                                coroutineScope.launch {
                                    vaultRepository.saveMemo(memo.copy(isPinned = !memo.isPinned))
                                }
                            },
                            onDelete = { confirmDeleteMemo = memo }
                        )
                    }
                }
            }

            // Edit dialog/bottom-sheet overlay
            editingMemo?.let { memo ->
                MemoEditor(
                    memo = memo,
                    onDismiss = { editingMemo = null },
                    onSave = { updated ->
                        coroutineScope.launch {
                            vaultRepository.saveMemo(updated)
                            editingMemo = null
                        }
                    }
                )
            }

            // Delete confirmation dialog
            confirmDeleteMemo?.let { memo ->
                AlertDialog(
                    onDismissRequest = { confirmDeleteMemo = null },
                    title = { Text("Delete Memo") },
                    text = { Text("Are you sure you want to permanently delete \"${memo.title.ifBlank { "Untitled Note" }}\"? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    vaultRepository.deleteMemo(memo.id)
                                    confirmDeleteMemo = null
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDeleteMemo = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MemoCard(
    memo: VaultMemo,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = remember(memo.color) {
        try {
            Color(android.graphics.Color.parseColor(memo.color.hexValue))
        } catch (e: Exception) {
            Color(0xFF1E1E2E)
        }
    }
    
    val textColor = remember(cardColor) {
        val r = cardColor.red
        val g = cardColor.green
        val b = cardColor.blue
        val luma = 0.299 * r + 0.587 * g + 0.114 * b
        if (luma > 0.6) Color.Black else Color.White
    }

    val subtitleColor = textColor.copy(alpha = 0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (memo.isPinned) "Pinned" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    fontWeight = FontWeight.SemiBold
                )
                Row {
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (memo.isPinned) Icons.Default.PushPin else Icons.Default.Pin,
                            contentDescription = "Pin Note",
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = memo.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Render text preview or checklist items
            val textBlock = memo.blocks.firstOrNull { it is MemoBlock.Text } as? MemoBlock.Text
            if (textBlock != null) {
                Text(
                    text = textBlock.markdown.ifBlank { "Empty note" },
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                val checklistBlock = memo.blocks.firstOrNull { it is MemoBlock.Checklist } as? MemoBlock.Checklist
                if (checklistBlock != null) {
                    checklistBlock.items.take(3).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = if (item.checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = subtitleColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
            Text(
                text = sdf.format(Date(memo.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoEditor(
    memo: VaultMemo,
    onDismiss: () -> Unit,
    onSave: (VaultMemo) -> Unit
) {
    var title by remember { mutableStateOf(memo.title) }
    var selectedColor by remember { mutableStateOf(memo.color) }
    var isPinned by remember { mutableStateOf(memo.isPinned) }
    
    val textBlock = remember(memo) {
        memo.blocks.firstOrNull { it is MemoBlock.Text } as? MemoBlock.Text 
            ?: MemoBlock.Text(id = java.util.UUID.randomUUID().toString(), markdown = "")
    }
    var markdownText by remember { mutableStateOf(textBlock.markdown) }

    val checklistBlock = remember(memo) {
        memo.blocks.firstOrNull { it is MemoBlock.Checklist } as? MemoBlock.Checklist
    }
    val checklistItems = remember {
        mutableStateListOf<ChecklistItem>().apply {
            checklistBlock?.let { addAll(it.items) }
        }
    }
    var newChecklistItemText by remember { mutableStateOf("") }
    var editorMode by remember { mutableStateOf(if (checklistBlock != null) "checklist" else "text") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (memo.id == 0L) "New Memo" else "Edit Memo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.Pin,
                                contentDescription = "Pin note",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
                
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("Enter memo title...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Color Picker Row
                Text(
                    "Note Theme Color",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MemoColor.entries.forEach { color ->
                        val hexColor = try {
                            Color(android.graphics.Color.parseColor(color.hexValue))
                        } catch (e: Exception) {
                            Color(0xFF1E1E2E)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(hexColor)
                                .clickable { selectedColor = color }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == color) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher for Editor Mode
                TabRow(
                    selectedTabIndex = if (editorMode == "text") 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Tab(
                        selected = editorMode == "text",
                        onClick = { editorMode = "text" }
                    ) {
                        Text("Note", fontSize = 13.sp)
                    }
                    Tab(
                        selected = editorMode == "checklist",
                        onClick = { editorMode = "checklist" }
                    ) {
                        Text("Checklist", fontSize = 13.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Editor Content Box
                Box(modifier = Modifier.weight(1f)) {
                    if (editorMode == "text") {
                        OutlinedTextField(
                            value = markdownText,
                            onValueChange = { markdownText = it },
                            placeholder = { Text("Start typing...") },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Add Checklist Item
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newChecklistItemText,
                                    onValueChange = { newChecklistItemText = it },
                                    placeholder = { Text("Add item...") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    if (newChecklistItemText.isNotBlank()) {
                                        checklistItems.add(ChecklistItem(newChecklistItemText))
                                        newChecklistItemText = ""
                                    }
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add item")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Checklist items scrollable list
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(checklistItems.size) { index ->
                                    val item = checklistItems[index]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.checked,
                                            onCheckedChange = { checked ->
                                                checklistItems[index] = item.copy(checked = checked)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.text,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        IconButton(onClick = { checklistItems.removeAt(index) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete item")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = {
                        val blocks = if (editorMode == "text") {
                            listOf(MemoBlock.Text(id = textBlock.id, markdown = markdownText))
                        } else {
                            listOf(
                                MemoBlock.Checklist(
                                    id = checklistBlock?.id ?: java.util.UUID.randomUUID().toString(),
                                    items = checklistItems.toList()
                                )
                            )
                        }
                        onSave(
                            memo.copy(
                                title = title,
                                color = selectedColor,
                                isPinned = isPinned,
                                blocks = blocks,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Memo")
                }
            }
        }
    }
}
