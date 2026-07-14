package com.example.expirytracker1.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expirytracker1.data.PantryItem
import com.example.expirytracker1.ui.theme.ExpiryTracker1Theme
import com.example.expirytracker1.ui.theme.TextGray
import com.example.expirytracker1.viewmodel.ProductViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProductViewModel,
    onNavigate: (String) -> Unit = {}
) {
    // --- State Management ---
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Use the shared list from ViewModel
    val itemsState = viewModel.products

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddOptions by remember { mutableStateOf(false) }
    var showManualAdd by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    
    // Filtering and sorting logic
    val filteredItems = remember(selectedTabIndex, itemsState.toList()) {
        when (selectedTabIndex) {
            0 -> itemsState.sortedBy { it.daysLeft } // Expiring Soon
            1 -> itemsState.toList() // All Items
            2 -> itemsState.sortedByDescending { it.addedTimestamp } // Recently Added
            else -> itemsState.toList()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomNavigationBar(onNavigate) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOptions = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .semantics { contentDescription = "Add Product" }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                HeaderSection(
                    onProfileClick = { onNavigate("PROFILE") },
                    onNotificationClick = { onNavigate("NOTIFICATIONS") }
                )
            }

            item {
                ScanBanner(onScanClick = { onNavigate("SCANNER") })
            }

            item {
                FilterTabs(
                    selectedTab = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )
            }

            if (filteredItems.isEmpty()) {
                item {
                    EmptyInventoryState()
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    InventoryCard(item)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // --- Bottom Sheets ---

    // Option Sheet: Scan or Manual
    if (showAddOptions) {
        ModalBottomSheet(
            onDismissRequest = { showAddOptions = false },
            sheetState = sheetState
        ) {
            AddOptionsContent(
                onScanClick = {
                    showAddOptions = false
                    onNavigate("SCANNER")
                },
                onManualClick = {
                    showAddOptions = false
                    showManualAdd = true
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAddOptions = false
                    }
                }
            )
        }
    }

    // Manual Add Sheet
    if (showManualAdd) {
        ModalBottomSheet(
            onDismissRequest = { showManualAdd = false },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            ManualAddContent(
                onSave = { newItem ->
                    viewModel.addProduct(newItem)
                    showManualAdd = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Product Added Successfully")
                    }
                },
                onCancel = { showManualAdd = false }
            )
        }
    }
}

@Composable
fun HeaderSection(onProfileClick: () -> Unit, onNotificationClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable(onClickLabel = "View Profile") { onProfileClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hello, User!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "You have items expiring soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
        }
        IconButton(onClick = onNotificationClick) {
            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = TextGray)
        }
    }
}

@Composable
fun ScanBanner(onScanClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClickLabel = "Scan Product") { onScanClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "Scan Product",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Instantly add items to\ninventory",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun FilterTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf("Expiring Soon", "All Items", "Recently Added")
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable(onClickLabel = "Switch to $title tab") { onTabSelected(index) }
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextGray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

@Composable
fun InventoryCard(item: PantryItem) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Inventory item: ${item.name}, ${item.daysLeft} days left" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = item.statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${item.daysLeft} days left",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = item.statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Qty: ${item.quantity}",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }
            }
            // Expiry Progress Indicator line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (item.daysLeft < 3) 1f else 0.3f) 
                        .fillMaxHeight()
                        .background(item.statusColor)
                )
            }
        }
    }
}

@Composable
fun EmptyInventoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Inventory,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextGray.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No products available",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Tap the + button to add your first product.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddOptionsContent(onScanClick: () -> Unit, onManualClick: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Add Product",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose how you want to add a product.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        ListItem(
            headlineContent = { Text("Scan Barcode (Recommended)") },
            leadingContent = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
            modifier = Modifier.clickable { onScanClick() }
        )
        ListItem(
            headlineContent = { Text("Add Manually") },
            leadingContent = { Icon(Icons.Default.EditNote, contentDescription = null) },
            modifier = Modifier.clickable { onManualClick() }
        )
        ListItem(
            headlineContent = { Text("Cancel", color = MaterialTheme.colorScheme.error) },
            leadingContent = { Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            modifier = Modifier.clickable { onCancel() }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddContent(onSave: (PantryItem) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vegetables") }
    var quantity by remember { mutableStateOf("") }
    var manDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var expDate by remember { mutableLongStateOf(System.currentTimeMillis() + 86400000 * 7) }
    var reminder by remember { mutableStateOf("On Expiry Date") }
    
    var showCatDropdown by remember { mutableStateOf(false) }
    var showRemDropdown by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePickerFor by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Vegetables", "Fruits", "Dairy", "Meat", "Bakery", "Frozen", "Beverages", "Others")
    val reminders = listOf("On Expiry Date", "1 Day Before", "3 Days Before", "7 Days Before")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Add Manually", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Box {
            OutlinedTextField(
                value = category,
                onValueChange = { },
                label = { Text("Category") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showCatDropdown = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            )
            DropdownMenu(expanded = showCatDropdown, onDismissRequest = { showCatDropdown = false }) {
                categories.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; showCatDropdown = false })
                }
            }
        }
        
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth()
        )
        
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = displayDateFormat.format(Date(manDate)),
                onValueChange = { },
                label = { Text("Mfg. Date") },
                readOnly = true,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    IconButton(onClick = { showDatePickerFor = "man" }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    }
                }
            )
            OutlinedTextField(
                value = displayDateFormat.format(Date(expDate)),
                onValueChange = { },
                label = { Text("Exp. Date") },
                readOnly = true,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    IconButton(onClick = { showDatePickerFor = "exp" }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    }
                }
            )
        }
        
        Box {
            OutlinedTextField(
                value = reminder,
                onValueChange = { },
                label = { Text("Reminder") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showRemDropdown = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            )
            DropdownMenu(expanded = showRemDropdown, onDismissRequest = { showRemDropdown = false }) {
                reminders.forEach { rem ->
                    DropdownMenuItem(text = { Text(rem) }, onClick = { reminder = rem; showRemDropdown = false })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val diff = expDate - System.currentTimeMillis()
                        val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                        
                        onSave(PantryItem(
                            name = name,
                            category = category,
                            daysLeft = daysLeft,
                            quantity = quantity.ifBlank { "1" },
                            expiryDate = dateFormat.format(Date(expDate))
                        ))
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Product")
            }
        }
    }

    if (showDatePickerFor != null) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        if (showDatePickerFor == "man") manDate = it
                        else expDate = it
                    }
                    showDatePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun BottomNavigationBar(onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { 
                Box(
                    modifier = Modifier
                        .size(48.dp, 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.primary) 
                }
            },
            label = { Text("Home", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
            selected = true,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Inventory2, contentDescription = "Inventory") },
            label = { Text("Inventory") },
            selected = false,
            onClick = { onNavigate("INVENTORY") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "Assistant") },
            label = { Text("Assistant") },
            selected = false,
            onClick = { onNavigate("ASSISTANT") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = false,
            onClick = { onNavigate("SETTINGS") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ExpiryTracker1Theme(dynamicColor = false) {
        HomeScreen(viewModel = viewModel())
    }
}
