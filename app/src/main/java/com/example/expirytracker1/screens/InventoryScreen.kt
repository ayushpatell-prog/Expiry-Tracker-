
package com.example.expirytracker1.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.expirytracker1.data.PantryItem
import com.example.expirytracker1.ui.theme.ExpiryTracker1Theme
import com.example.expirytracker1.ui.theme.TextGray
import com.example.expirytracker1.viewmodel.ProductViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: ProductViewModel = viewModel(),
    onNavigate: (String) -> Unit = {}
) {
    // --- State Management ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    // Use shared list from ViewModel
    val allItems by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val error by viewModel.error.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Filter & Sort States
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedItemForDetails by remember { mutableStateOf<PantryItem?>(null) }
    var selectedItemForReminder by remember { mutableStateOf<PantryItem?>(null) }
    var itemToEdit by remember { mutableStateOf<PantryItem?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Applied states
    var appliedSortBy by remember { mutableStateOf("Expiry Date (Nearest First)") }
    var appliedCategories by remember { mutableStateOf(setOf("All", "Vegetables", "Dairy", "Meat")) }
    var appliedStatuses by remember { mutableStateOf(setOf("Fresh", "Expiring Soon", "Expired")) }

    // Derived State for performance: counts active filters (excluding defaults)
    val activeFilterCount by remember {
        derivedStateOf {
            var count = 0
            if (appliedSortBy != "Expiry Date (Nearest First)") count++
            if (appliedCategories.size < 4) count++
            if (appliedStatuses.size < 3) count++
            count
        }
    }

    // --- Helper Functions ---

    fun getStatus(item: PantryItem): String {
        return when (item.statusColor) {
            Color(0xFF4CAF50) -> "Fresh"
            Color(0xFFFBC02D) -> "Expiring Soon"
            else -> "Expired"
        }
    }

    fun sortItems(items: List<PantryItem>): List<PantryItem> {
        return when (appliedSortBy) {
            "Product Name (A-Z)" -> items.sortedBy { it.name }
            "Quantity" -> items.sortedBy { it.quantity }
            "Expiry Date (Nearest First)" -> {
                val months = mapOf("Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4, "May" to 5, "Jun" to 6, "Jul" to 7, "Aug" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12)
                items.sortedBy { exp ->
                    val parts = exp.expiryDate.split(" ")
                    val day = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val month = months[parts.getOrNull(1)] ?: 0
                    month * 100 + day
                }
            }
            else -> items
        }
    }

    // Combined filtering logic
    val filteredItems = remember(searchQuery, selectedCategory, appliedSortBy, appliedCategories, appliedStatuses, allItems.size) {
        allItems.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
            val matchesQuickCategory = selectedCategory == "All" || selectedCategory == item.category
            val matchesAdvancedCategory = appliedCategories.contains("All") || appliedCategories.contains(item.category)
            val matchesStatus = appliedStatuses.contains(getStatus(item))
            matchesSearch && matchesQuickCategory && matchesAdvancedCategory && matchesStatus
        }.let { sortItems(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { InventoryBottomNavigation(onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).height(56.dp),
                    placeholder = { Text("Search your pantry...", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = TextGray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                // Tune Icon with Filter Badge
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge { Text(activeFilterCount.toString()) }
                        }
                    }
                ) {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Open Filters", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Category Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All" to null, "Vegetables" to Icons.Default.Eco, "Dairy" to Icons.Default.LocalDrink, "Meat" to Icons.Default.SetMeal).forEach { (cat, icon) ->
                    CategoryChip(cat, icon = icon, isSelected = selectedCategory == cat) { selectedCategory = cat }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Inventory List with Animations
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && allItems.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredItems.isEmpty()) {
                    EmptyState(
                        isSearch = searchQuery.isNotEmpty(),
                        onClear = { searchQuery = "" }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            PantryItemCard(
                                item = item,
                                statusLabel = getStatus(item),
                                onViewDetails = { selectedItemForDetails = item },
                                onEdit = { itemToEdit = item },
                                onSetReminder = { selectedItemForReminder = item },
                                onGetAiSuggestions = {
                                    onNavigate("ASSISTANT?name=${item.name}&brand=${item.brand}&category=${item.category}&quantity=${item.quantity}&expiry=${item.expiryDate}&image=${item.imageUrl}")
                                },
                                onDelete = {
                                    // Remove from shared ViewModel
                                    viewModel.deleteProduct(item)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "${item.name} deleted",
                                            actionLabel = "UNDO",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addProduct(item)
                                        }
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // --- Details and Edit Sheets ---
        
        if (selectedItemForDetails != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedItemForDetails = null },
                modifier = Modifier.fillMaxHeight(0.8f)
            ) {
                ItemDetailsContent(
                    item = selectedItemForDetails!!,
                    onEditClick = {
                        itemToEdit = selectedItemForDetails
                        selectedItemForDetails = null
                    },
                    onClose = { selectedItemForDetails = null }
                )
            }
        }

        if (itemToEdit != null) {
            ModalBottomSheet(
                onDismissRequest = { itemToEdit = null },
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                EditProductContent(
                    item = itemToEdit!!,
                    onSave = { updatedItem ->
                        viewModel.addProduct(updatedItem) // Firestore 'set' handles update if ID matches
                        itemToEdit = null
                        scope.launch { snackbarHostState.showSnackbar("Product updated successfully") }
                    },
                    onCancel = { itemToEdit = null }
                )
            }
        }

        if (selectedItemForReminder != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedItemForReminder = null }
            ) {
                ReminderSelectionContent(
                    currentItem = selectedItemForReminder!!,
                    onSave = { updatedItem ->
                        viewModel.addProduct(updatedItem)
                        selectedItemForReminder = null
                        scope.launch { snackbarHostState.showSnackbar("Reminder updated") }
                    },
                    onCancel = { selectedItemForReminder = null }
                )
            }
        }

        // --- Bottom Sheet Logic ---
        if (showFilterSheet) {
            FilterSortBottomSheet(
                sheetState = sheetState,
                scope = scope,
                initialSortBy = appliedSortBy,
                initialCategories = appliedCategories,
                initialStatuses = appliedStatuses,
                onApply = { sort, cats, stats ->
                    appliedSortBy = sort
                    appliedCategories = cats
                    appliedStatuses = stats
                    showFilterSheet = false
                },
                onReset = {
                    searchQuery = ""
                    selectedCategory = "All"
                    appliedSortBy = "Expiry Date (Nearest First)"
                    appliedCategories = setOf("All", "Vegetables", "Dairy", "Meat")
                    appliedStatuses = setOf("Fresh", "Expiring Soon", "Expired")
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
fun EmptyState(isSearch: Boolean, onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearch) Icons.Default.SearchOff else Icons.Default.Inventory,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = TextGray.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearch) "No products found" else "Inventory is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isSearch) "Try another search keyword." else "Start adding items to track them.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        if (isSearch) {
            TextButton(onClick = onClear, modifier = Modifier.padding(top = 8.dp)) {
                Text("Clear Search")
            }
        }
    }
}

@Composable
fun PantryItemCard(
    item: PantryItem, 
    statusLabel: String, 
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onSetReminder: () -> Unit,
    onGetAiSuggestions: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Icon
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    // Status Badge
                    Surface(
                        color = item.statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(item.statusColor))
                            Spacer(Modifier.width(6.dp))
                            Text(text = if (item.daysLeft == 0) "Expires Today" else "$statusLabel (${item.daysLeft}d)", color = item.statusColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.quantity, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(text = "  •  ", color = TextGray)
                        Text(text = "Exp: ${item.expiryDate}", color = item.statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Product Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = { 
                                showMenu = false
                                onViewDetails()
                            },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = "View Details") }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Product") },
                            onClick = { 
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit Product") }
                        )
                        DropdownMenuItem(
                            text = { Text("Set Reminder") },
                            onClick = { 
                                showMenu = false
                                onSetReminder()
                            },
                            leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = "Set Reminder") }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Delete Product", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showDeleteDialog = true },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            // AI Recipe Suggestion Button - Visible for all items
            FilledTonalButton(
                onClick = onGetAiSuggestions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("✨ Get AI Recipe Suggestions", style = MaterialTheme.typography.labelMedium)
            }

            // Expiry Line
            val progress = remember(item.daysLeft) {
                when {
                    item.daysLeft <= 0 -> 1f
                    item.daysLeft >= 30 -> 0.1f
                    else -> 1f - (item.daysLeft.toFloat() / 30f)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))) {
                Box(
                    modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(item.statusColor)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Product?") },
            text = { Text("Are you sure you want to remove ${item.name}?") },
            confirmButton = {
                Button(onClick = { showDeleteDialog = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortBottomSheet(
    sheetState: SheetState,
    scope: kotlinx.coroutines.CoroutineScope,
    initialSortBy: String,
    initialCategories: Set<String>,
    initialStatuses: Set<String>,
    onApply: (String, Set<String>, Set<String>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempSortBy by remember { mutableStateOf(initialSortBy) }
    var tempCategories by remember { mutableStateOf(initialCategories) }
    var tempStatuses by remember { mutableStateOf(initialStatuses) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Filter & Sort", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))

            Text("Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val sortOptions = listOf("Expiry Date (Nearest First)", "Product Name (A-Z)", "Recently Added", "Quantity")
            Column(Modifier.selectableGroup()) {
                sortOptions.forEach { text ->
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).selectable(selected = (text == tempSortBy), onClick = { tempSortBy = text }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (text == tempSortBy), onClick = null)
                        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val categoryOptions = listOf("All", "Vegetables", "Dairy", "Meat")
            categoryOptions.forEach { text ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clickable {
                        tempCategories = if (text == "All") {
                            if (tempCategories.contains("All")) emptySet() else categoryOptions.toSet()
                        } else {
                            val newSet = tempCategories.toMutableSet()
                            if (newSet.contains(text)) newSet.remove(text) else newSet.add(text)
                            if (newSet.size == categoryOptions.size - 1 && !newSet.contains("All")) newSet.add("All")
                            else if (newSet.size < categoryOptions.size) newSet.remove("All")
                            newSet
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = tempCategories.contains(text), onCheckedChange = null)
                    Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val statusOptions = listOf("Fresh", "Expiring Soon", "Expired")
            statusOptions.forEach { text ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clickable {
                        val newSet = tempStatuses.toMutableSet()
                        if (newSet.contains(text)) newSet.remove(text) else newSet.add(text)
                        tempStatuses = newSet
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = tempStatuses.contains(text), onCheckedChange = null)
                    Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = { onReset() }) { Text("Reset") }
                Button(modifier = Modifier.weight(1f), onClick = { onApply(tempSortBy, tempCategories, tempStatuses) }) { Text("Apply") }
            }
        }
    }
}

@Composable
fun CategoryChip(text: String, icon: ImageVector? = null, isSelected: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
fun ItemDetailsContent(item: PantryItem, onEditClick: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Product Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(item.category, color = MaterialTheme.colorScheme.primary)
            }
        }

        DetailRow(label = "Quantity", value = item.quantity)
        DetailRow(label = "Expiry Date", value = item.expiryDate, valueColor = Color(0xFFD32F2F))
        DetailRow(label = "Purchase Date", value = item.purchaseDate.ifBlank { "Not set" })
        DetailRow(label = "Reminder", value = item.reminder)
        
        if (item.notes.isNotBlank()) {
            Column {
                Text("Notes", style = MaterialTheme.typography.labelLarge, color = TextGray)
                Text(item.notes, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onEditClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Edit Product")
        }
    }
}

@Composable
fun InventoryBottomNavigation(onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        val navItems = listOf(
            Triple("Home", Icons.Outlined.Home, "HOME"),
            Triple("Inventory", Icons.Filled.Inventory2, "INVENTORY"),
            Triple("Alerts", Icons.Outlined.NotificationsActive, "ALERTS"),
            Triple("Settings", Icons.Outlined.Settings, "PROFILE")
        )
        navItems.forEach { (label, icon, route) ->
            val isSelected = route == "INVENTORY"
            NavigationBarItem(
                icon = {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(48.dp, 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Icon(icon, contentDescription = label)
                    }
                },
                label = { 
                    Text(
                        label, 
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                selected = isSelected,
                onClick = { if (!isSelected) onNavigate(route) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductContent(item: PantryItem, onSave: (PantryItem) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity) }
    var notes by remember { mutableStateOf(item.notes) }
    var expDate by remember { mutableLongStateOf(item.expiryTimestamp) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = item.expiryTimestamp)
    var showDatePicker by remember { mutableStateOf(false) }
    val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Edit Product", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
        
        OutlinedTextField(
            value = displayDateFormat.format(Date(expDate)),
            onValueChange = {},
            label = { Text("Expiry Date") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarToday, null) } }
        )

        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = {
                onSave(item.copy(name = name, quantity = quantity, notes = notes, expiryTimestamp = expDate, expiryDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expDate))))
            }, enabled = name.isNotBlank()) {
                Text("Save Changes")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { expDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Preview(showBackground = true)
@Composable
fun InventoryScreenPreview() {
    ExpiryTracker1Theme { InventoryScreen(viewModel = viewModel()) }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextGray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = valueColor)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    }
}

@Composable
fun ReminderSelectionContent(currentItem: PantryItem, onSave: (PantryItem) -> Unit, onCancel: () -> Unit) {
    val reminders = listOf("On Expiry Day", "1 Day Before", "3 Days Before", "7 Days Before")
    var selectedReminder by remember { mutableStateOf(currentItem.reminder) }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Set Reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        reminders.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedReminder = option }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (option == selectedReminder), onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(option, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = { onSave(currentItem.copy(reminder = selectedReminder)) }) {
                Text("Save")
            }
        }
    }
}
