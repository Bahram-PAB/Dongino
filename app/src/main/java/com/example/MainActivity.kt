package com.example

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.DongTheme
import com.example.ui.viewmodel.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize DAO and Repository
        val database = DongDatabase.getDatabase(applicationContext)
        val repository = DongRepository(database.dongDao())

        setContent {
            DongTheme {
                // Instantiating ViewModel with local Repository safely
                val viewModel: DongViewModel = viewModel(
                    factory = DongViewModelFactory(application, repository)
                )

                // Force Right-to-Left (RTL) layout direction globally for perfect Persian alignment
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            DongAppMain(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER FORMATTING FUNCTIONS ---
fun formatToman(amount: Long): String {
    val formatter = NumberFormat.getInstance(Locale("fa", "IR"))
    return "${formatter.format(amount)} تومان"
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("fa", "IR"))
    return sdf.format(Date(timestamp))
}

@Composable
fun DongAppMain(viewModel: DongViewModel) {
    var currentTab by remember { mutableStateOf("home") }
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val allGroups by viewModel.allGroups.collectAsStateWithLifecycle()
    val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()

    var showGroupSelectorSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // High Fidelity Header Toolbar
            HeaderToolbar(
                activeGroup = activeGroup,
                onSwitchClick = { showGroupSelectorSheet = true }
            )

            // Dynamic screen loading area based on bottom tabs
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    "home" -> HomeScreen(viewModel, onViewGroupDetail = { currentTab = "dashboard" })
                    "dashboard" -> {
                        if (selectedGroupId == -1) {
                            NoGroupSelectedState(allGroups, onSelectGroup = { viewModel.setSelectedGroup(it) })
                        } else {
                            GroupDashboardScreen(viewModel)
                        }
                    }
                    "expenses" -> {
                        if (selectedGroupId == -1) {
                            NoGroupSelectedState(allGroups, onSelectGroup = { viewModel.setSelectedGroup(it) })
                        } else {
                            ExpensesScreen(viewModel)
                        }
                    }
                    "settlements" -> {
                        if (selectedGroupId == -1) {
                            NoGroupSelectedState(allGroups, onSelectGroup = { viewModel.setSelectedGroup(it) })
                        } else {
                            SettlementsScreen(viewModel)
                        }
                    }
                }
            }

            // Elegant Bottom Navigation Bar (RTL aligned automatically)
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    label = { Text("خانه", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.SansSerif) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "خانه") },
                    modifier = Modifier.testTag("nav_tab_home")
                )
                NavigationBarItem(
                    selected = currentTab == "dashboard",
                    onClick = { currentTab = "dashboard" },
                    label = { Text("تراز گروه", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.SansSerif) },
                    icon = { Icon(Icons.Default.Group, contentDescription = "تراز گروه") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == "expenses",
                    onClick = { currentTab = "expenses" },
                    label = { Text("هزینه‌ها", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.SansSerif) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "هزینه‌ها") },
                    modifier = Modifier.testTag("nav_tab_expenses")
                )
                NavigationBarItem(
                    selected = currentTab == "settlements",
                    onClick = { currentTab = "settlements" },
                    label = { Text("تسویه‌حساب", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.SansSerif) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = "تسویه‌حساب") },
                    modifier = Modifier.testTag("nav_tab_settlements")
                )
            }
        }

        // Beautiful Group Selector Bottom Dialog Dialog Modal
        if (showGroupSelectorSheet) {
            GroupSelectorDialog(
                allGroups = allGroups,
                selectedGroupId = selectedGroupId,
                onSelectGroup = {
                    viewModel.setSelectedGroup(it)
                    showGroupSelectorSheet = false
                },
                onDismiss = { showGroupSelectorSheet = false }
            )
        }
    }
}

// --- REUSABLE COMPOSABLES ---

@Composable
fun HeaderToolbar(activeGroup: DongGroup?, onSwitchClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "دُنگینو",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "نسخه ۱",
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "تقسیم آسان هزینه‌های گروهی",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Quick Active Group Badge & Switcher
            OutlinedButton(
                onClick = onSwitchClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("group_switcher_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "تغییر گروه",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeGroup?.name ?: "انتخاب گروه",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp)
                )
            }
        }
    }
}

@Composable
fun NoGroupSelectedState(allGroups: List<DongGroup>, onSelectGroup: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderShared,
                    contentDescription = "هیچ گروهی انتخاب نشده",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ابتدا یک گروه را انتخاب کنید",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "برای ثبت هزینه‌ها و محاسبه سهم هر نفر، لطفاً یکی از گروه‌های موجود را انتخاب کرده یا گروه جدیدی بسازید.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (allGroups.isEmpty()) {
                    Text(
                        text = "هنوز هیچ گروهی نساخته‌اید! به بخش خانه بروید و اولین گروه خود را ایجاد کنید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "انتخاب سریع گروه:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allGroups) { group ->
                            InputChip(
                                selected = false,
                                onClick = { onSelectGroup(group.id) },
                                label = { Text(group.name, fontWeight = FontWeight.Bold) },
                                trailingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 1. HOME TAB SCREEN ---
@Composable
fun HomeScreen(viewModel: DongViewModel, onViewGroupDetail: () -> Unit) {
    val allGroups by viewModel.allGroups.collectAsStateWithLifecycle()
    val activeGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredGroups = allGroups.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                (it.description != null && it.description.contains(searchQuery, ignoreCase = true))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Elegant Welcome & Summary Banner
            WelcomeCard(allGroups.size)

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_search_input"),
                placeholder = { Text("جستجو در بین گروه‌ها...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لیست گروه‌ها",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${filteredGroups.size} گروه",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "هیچ گروهی ایجاد نشده است" else "نتیجه‌ای یافت نشد!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "با زدن دکمه + پایین اولین گروه سفر یا مهمانی خود را ایجاد کنید." else "عبارت دیگری را جستجو کنید.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredGroups) { group ->
                        GroupListItemCard(
                            group = group,
                            isActive = group.id == activeGroupId,
                            onSelect = {
                                viewModel.setSelectedGroup(group.id)
                                onViewGroupDetail()
                            },
                            onDelete = {
                                viewModel.deleteGroup(group)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer with GitHub and LinkedIn Icons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "طراحی و توسعه با ❤️ برای دُنگینو",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val context = LocalContext.current
                    
                    // GitHub icon button
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Bahram-PAB"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("github_footer_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = "گیت‌هاب من",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // LinkedIn icon button
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/bahram-pouralibaba-1a992239"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("linkedin_footer_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_linkedin),
                            contentDescription = "لینکدین من",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Large Add Group FAB
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .testTag("add_group_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "ایجاد گروه جدید")
        }

        if (showCreateDialog) {
            CreateGroupDialog(
                onCreate = { name, desc, start, end, members ->
                    viewModel.createGroup(name, desc, start, end, members)
                    showCreateDialog = false
                    onViewGroupDetail()
                },
                onDismiss = { showCreateDialog = false }
            )
        }
    }
}

@Composable
fun WelcomeCard(groupCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary, // #10B981 Mint Green
                            Color(0xFF0F172A) // Deep Slate / Secondary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "دوست عزیز، به برنامه دُنگینو خوش آمدید!",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "بدون دردسر و بدون نیاز به حساب و کتاب پیچیده، هزینه‌های سفر، مهمانی و تراز اعضا را مدیریت کنید.",
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$groupCount",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "گروه",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupListItemCard(
    group: DongGroup,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { showDeleteConfirm = true }
            )
            .testTag("group_card_${group.id}"),
        shape = RoundedCornerShape(24.dp), // Premium rounded-[2rem] equivalent for elegant corners
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Info Row with Dynamic Pastel Icon Box matching Professional Polish
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Determine theme styling dynamically based on group name
                    val (icon, bgColor, iconColor) = when {
                        group.name.contains("سفر") || group.name.contains("شمال") || group.name.contains("کوه") || group.name.contains("کمپ") -> {
                            Triple(Icons.Default.Terrain, Color(0xFFFEF3C7), Color(0xFFD97706)) // Amber Terrain style
                        }
                        group.name.contains("تولد") || group.name.contains("جشن") || group.name.contains("مهمانی") || group.name.contains("دورهمی") || group.name.contains("مریم") -> {
                            Triple(Icons.Default.Celebration, Color(0xFFFCE7F3), Color(0xFFDB2777)) // Pink Celebration style
                        }
                        group.name.contains("خانه") || group.name.contains("اتاق") || group.name.contains("خرید") || group.name.contains("اجاره") -> {
                            Triple(Icons.Default.Home, Color(0xFFE0E7FF), Color(0xFF4F46E5)) // Indigo Home style
                        }
                        else -> {
                            Triple(Icons.Default.Group, Color(0xFFD1FAE5), Color(0xFF059669)) // Emerald Mint Group style
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isActive) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                            Text(
                                text = group.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!group.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف گروه",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Creation Date Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ایجاد: ${formatDate(group.createdAt)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Subtitle specs (duration if added)
                if (!group.startDate.isNullOrBlank()) {
                    Text(
                        text = "سفر: ${group.startDate} تا ${group.endDate ?: "نامعلوم"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف گروه دنگ", fontWeight = FontWeight.Bold) },
            text = { Text("آیا واقعاً می‌خواهید گروه «${group.name}» را به همراه تمام اعضا و مخارج آن برای همیشه حذف کنید؟ این عمل غیرقابل بازگشت است.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("بله، حذف کن", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

// --- 2. GROUP DASHBOARD SCREEN (تراز گروه / تراز اعضا) ---
@Composable
fun GroupDashboardScreen(viewModel: DongViewModel) {
    val context = LocalContext.current
    val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()
    val activeMembers by viewModel.activeMembers.collectAsStateWithLifecycle()
    val summaryState by viewModel.groupSummaryState.collectAsStateWithLifecycle()

    var showAddMemberDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // High fidelity Dashboard Card (Total Expenses & cost per individual)
            GroupCostDashboardCard(
                groupName = activeGroup?.name ?: "",
                totalExpenses = summaryState.totalExpenses,
                sharePerPerson = summaryState.sharePerPerson,
                memberCount = activeMembers.size,
                onShareClick = {
                    val shareText = buildShareString(
                        group = activeGroup,
                        summaryState = summaryState
                    )
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, "ارسال به دنگ بقیه اعضا"))
                }
            )
        }

        item {
            // Members List header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وضعیت حساب اعضا",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(
                    onClick = { showAddMemberDialog = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("عضو جدید", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List balances
        if (summaryState.memberBalances.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنوز هیچ عضوی به این گروه اضافه نشده است.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(summaryState.memberBalances) { memberBalance ->
                MemberBalanceRow(
                    memberBalance = memberBalance,
                    onDeleteMember = { viewModel.removeMember(memberBalance.member) }
                )
            }
        }

        // Settlement matching suggestions section
        item {
            Text(
                text = "پیشنهاد‌های تسویه حساب بهینه",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (summaryState.suggestions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "همه حساب‌ها کاملاً تراز و تسویه هستند! هیچ دنگی باقی نمانده است.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            items(summaryState.suggestions) { suggestion ->
                SettleSuggestionCard(
                    suggestion = suggestion,
                    onQuickSettle = {
                        viewModel.addSettlement(
                            fromId = suggestion.fromMember.id,
                            toId = suggestion.toMember.id,
                            amount = suggestion.amount
                        )
                        Toast.makeText(context, "پرداخت دنگ با موفقیت اضافه شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onAdd = { name ->
                viewModel.addMember(name)
                showAddMemberDialog = false
            },
            onDismiss = { showAddMemberDialog = false }
        )
    }
}

@Composable
fun GroupCostDashboardCard(
    groupName: String,
    totalExpenses: Long,
    sharePerPerson: Long,
    memberCount: Int,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary, // Mint Green
                            Color(0xFF0F172A) // Deep Slate / Secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "خلاصه دنگ گروه $groupName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "اشتراک‌گذاری وضعیت مالی",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Giant Expense Sum
            Text(
                text = "جمع کل هزینه‌ها",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = formatToman(totalExpenses),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "تعداد کل اعضا",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$memberCount نفر",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "سهم هر نفر",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatToman(sharePerPerson),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}
}

@Composable
fun MemberBalanceRow(
    memberBalance: MemberBalance,
    onDeleteMember: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = memberBalance.member.name.take(1),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = memberBalance.member.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Tag of billing status
                    val (statusText, statusColor) = when {
                        memberBalance.netBalance > 0 -> Pair("طلبکار", Color(0xFF10B981))
                        memberBalance.netBalance < 0 -> Pair("بدهکار", Color(0xFFEF4444))
                        else -> Pair("بی‌حساب تسویه شده", MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatToman(abs(memberBalance.netBalance)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (memberBalance.netBalance > 0) Color(0xFF10B981) else if (memberBalance.netBalance < 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Delete member from group
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "حذف عضو",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف عضو گروه دنگ", fontWeight = FontWeight.Bold) },
            text = { Text("آیا مطمئن هستید که می‌خواهید «${memberBalance.member.name}» را از گروه حذف کنید؟ با این کار حساب‌های وی پاک می‌شود.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMember()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("بله حذف کن", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun SettleSuggestionCard(
    suggestion: SettlementSuggestion,
    onQuickSettle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = suggestion.fromMember.name,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                    Text(
                        text = " دنگ بدهد به ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = suggestion.toMember.name,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        fontSize = 14.sp
                    )
                }

                // Amount
                Text(
                    text = formatToman(suggestion.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CTA Button to quick record
            Button(
                onClick = onQuickSettle,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ثبت دنگ انجام شد",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Format Farsi receipt share summary
fun buildShareString(group: DongGroup?, summaryState: GroupSummaryState): String {
    val sb = java.lang.StringBuilder()
    sb.append("📊 خلاصه حساب‌ کتاب دنگ گروه: *${group?.name ?: ""}*\n")
    sb.append("📅 تاریخ: ${formatDate(System.currentTimeMillis())}\n")
    sb.append("─────────────────────\n")
    sb.append("💵 کل مخارج گروه: *${formatToman(summaryState.totalExpenses)}*\n")
    sb.append("👤 سهم هر نفر: *${formatToman(summaryState.sharePerPerson)}*\n\n")

    sb.append("📌 وضعیت تراز اعضا:\n")
    for (bal in summaryState.memberBalances) {
        val st = if (bal.netBalance > 0) "طلـبکار: +${formatToman(bal.netBalance)}"
        else if (bal.netBalance < 0) "بدهـکار: -${formatToman(abs(bal.netBalance))}"
        else "بی‌حساب و تسویه"
        sb.append("• ${bal.member.name} ➔ $st\n")
    }

    sb.append("\n✅ پیشنهاد پرداخت‌های بهینه تسویه:\n")
    if (summaryState.suggestions.isEmpty()) {
        sb.append("همه حساب‌ها تراز هستند. عالی!\n")
    } else {
        for (sg in summaryState.suggestions) {
            sb.append("💸 ${sg.fromMember.name} باید مبلغ *${formatToman(sg.amount)}* به ${sg.toMember.name} بدهد.\n")
        }
    }
    sb.append("\n_توسط اپلیکیشن دنگینو_ 📱")
    return sb.toString()
}

// --- 3. EXPENSES SCREEN ---
@Composable
fun ExpensesScreen(viewModel: DongViewModel) {
    val activeExpenses by viewModel.activeExpenses.collectAsStateWithLifecycle()
    val activeMembers by viewModel.activeMembers.collectAsStateWithLifecycle()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("همه") }

    val categoriesList = listOf("همه", "غذا", "حمل‌ونقل", "اقامت", "متفرقه")

    val filteredExpenses = activeExpenses.filter {
        val matchesSearch = it.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "همه" || it.category == selectedCategoryFilter
        matchesSearch && matchesCategory
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "هزینه‌های ثبت‌شده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${filteredExpenses.size} فاکتور",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجو در بین جزئیات مخارج...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category filter pills
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoriesList) { cat ->
                    val isSelected = cat == selectedCategoryFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هیچ هزینه‌ای پیدا نشد!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای افزودن اولین خرج، روی دکمه + پایین ضربه بزنید.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            members = activeMembers,
                            onEdit = { editingExpense = expense },
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }
        }

        // Add Expense FAB
        FloatingActionButton(
            onClick = {
                if (activeMembers.isEmpty()) {
                    Toast.makeText(viewModel.getApplication(), "ابتدا عضو اضافه کنید", Toast.LENGTH_SHORT).show()
                } else {
                    showAddExpenseDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .testTag("add_expense_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "افزودن خرج")
        }

        if (showAddExpenseDialog) {
            AddEditExpenseDialog(
                members = activeMembers,
                onSave = { amount, desc, payerId, beneficiaryIds, category, date, uri ->
                    viewModel.saveExpense(
                        amount = amount,
                        description = desc,
                        paidById = payerId,
                        beneficiaryIds = beneficiaryIds,
                        category = category,
                        date = date,
                        imageUri = uri
                    )
                    showAddExpenseDialog = false
                },
                onDismiss = { showAddExpenseDialog = false }
            )
        }

        if (editingExpense != null) {
            AddEditExpenseDialog(
                editingExpense = editingExpense,
                members = activeMembers,
                onSave = { amount, desc, payerId, beneficiaryIds, category, date, uri ->
                    viewModel.saveExpense(
                        id = editingExpense!!.id,
                        amount = amount,
                        description = desc,
                        paidById = payerId,
                        beneficiaryIds = beneficiaryIds,
                        category = category,
                        date = date,
                        imageUri = uri,
                        existingPhotoPath = editingExpense!!.invoicePhotoPath
                    )
                    editingExpense = null
                },
                onDismiss = { editingExpense = null }
            )
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    members: List<GroupMember>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val payer = members.find { it.id == expense.paidByMemberId }
    val payerName = payer?.name ?: "نامعلوم"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetailsDialog = true }
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag pill & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(expense.category, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = expense.description,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatToman(expense.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "پرداخت‌کننده: $payerName",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDate(expense.date),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    if (expense.invoicePhotoPath != null) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "دارای عکس فاکتور",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش هزینه",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف هزینه",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف هزینه دنگ", fontWeight = FontWeight.Bold) },
            text = { Text("آیا می‌خواهید هزینه «${expense.description}» به مبلغ ${formatToman(expense.amount)} را حذف کنید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("بله حذف کن", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Invoice Preview & Details Overlay sheet dialog
    if (showDetailsDialog) {
        Dialog(onDismissRequest = { showDetailsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "جزئیات سهم هزینه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DetailItemRow("توضیح:", expense.description)
                    DetailItemRow("مبلغ کل:", formatToman(expense.amount))
                    DetailItemRow("پرداخت‌کننده:", payerName)
                    DetailItemRow("دسته‌بندی:", expense.category)
                    DetailItemRow("تاریخ:", formatDate(expense.date))

                    // Show list of beneficiaries
                    val beneficiariesList = remember(expense, members) {
                        val ids = expense.beneficiaryMemberIds.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
                        if (ids.isEmpty()) "همه اعضا"
                        else members.filter { ids.contains(it.id) }.joinToString("، ") { it.name }
                    }
                    DetailItemRow("سهم‌برندگان:", beneficiariesList)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (expense.invoicePhotoPath != null) {
                        Text(
                            text = "تصویر فیش/فاکتور ضمیمهشده:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                        )
                        val imageFile = File(expense.invoicePhotoPath)
                        if (imageFile.exists()) {
                            AsyncImage(
                                model = imageFile,
                                contentDescription = "فیش فاکتور",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Render template fallback visually styled if physical device reference failed
                            MockVisualInvoiceView(expense)
                        }
                    } else {
                        // Empty photo placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "تصویری پیوست نشده است.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showDetailsDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("بستن صفحه")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// In case the physical file cannot be rendered or wasn't taken on the emulator, render a beautiful mock bill
@Composable
fun MockVisualInvoiceView(expense: Expense) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("فاکتور رسمی پیوست‌شده", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(expense.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                text = "${formatToman(expense.amount)} - ${formatDate(expense.date)}",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// --- 4. SETTLEMENTS SCREEN (پرداخت‌های واقعی) ---
@Composable
fun SettlementsScreen(viewModel: DongViewModel) {
    val activeSettlements by viewModel.activeSettlements.collectAsStateWithLifecycle()
    val activeMembers by viewModel.activeMembers.collectAsStateWithLifecycle()

    var showAddSettlementDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تاریخچه تراکنش‌های تسویه‌شده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${activeSettlements.size} تراکنش",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeSettlements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تراکنشی ثبت نشده است",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "وقتی فردی دنگ خود را واگذار یا نقداً تسویه کرد، از پایین وارد کنید تا تراز تصفیه شود.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeSettlements) { settlement ->
                        SettlementHistoryCard(
                            settlement = settlement,
                            members = activeMembers,
                            onDelete = { viewModel.deleteSettlement(settlement) }
                        )
                    }
                }
            }
        }

        // Add manual settlement Button FAB
        FloatingActionButton(
            onClick = {
                if (activeMembers.size < 2) {
                    Toast.makeText(viewModel.getApplication(), "برای تسویه به بیش از ۱ عضو در گروه نیاز است.", Toast.LENGTH_SHORT).show()
                } else {
                    showAddSettlementDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .testTag("add_settlement_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.AddCard, contentDescription = "ثبت تسویه دستی")
        }

        if (showAddSettlementDialog) {
            AddSettlementDialog(
                members = activeMembers,
                onSave = { from, to, amount ->
                    viewModel.addSettlement(from, to, amount)
                    showAddSettlementDialog = false
                },
                onDismiss = { showAddSettlementDialog = false }
            )
        }
    }
}

@Composable
fun SettlementHistoryCard(
    settlement: Settlement,
    members: List<GroupMember>,
    onDelete: () -> Unit
) {
    val sender = members.find { it.id == settlement.fromMemberId }?.name ?: "نامعلوم"
    val receiver = members.find { it.id == settlement.toMemberId }?.name ?: "نامعلوم"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settlement_card_${settlement.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sender, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(" ➔ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(receiver, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "مبلغ تسویه: ${formatToman(settlement.amount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDate(settlement.date),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف تراکنش",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// --- FLOATING POPUP OVERLAYS / DIALOGS ---

@Composable
fun CreateGroupDialog(
    onCreate: (String, String?, String?, String?, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    var currentMemberInput by remember { mutableStateOf("") }
    val membersList = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "ایجاد گروه دنگ حساب جدید",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام گروه (مثلاً: سفر شمال)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات کوتاه سفر/مهمانی") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("تاریخ شروع (اختیاری)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("تاریخ پایان (اختیاری)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // DYNAMIC MEMBERS ADD SECTION
                Text(
                    text = "اعضای گروه دنگ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentMemberInput,
                        onValueChange = { currentMemberInput = it },
                        placeholder = { Text("نام عضو را وارد کنید", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_member_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (currentMemberInput.isNotBlank()) {
                                membersList.add(currentMemberInput.trim())
                                currentMemberInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("append_member_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن عضو")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of added names
                if (membersList.isEmpty()) {
                    Text(
                        text = "هیچ عضوی اضافه نشده است. (اعضا را در فیلد بالا با دکمه + ثبت کنید)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 16.sp
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        membersList.forEachIndexed { idx, name ->
                            AssistChip(
                                onClick = {},
                                label = { Text(name, fontWeight = FontWeight.Bold) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { membersList.removeAt(idx) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(10.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                // Basic validation
                                return@Button
                            }
                            onCreate(name, description.ifBlank { null }, startDate.ifBlank { null }, endDate.ifBlank { null }, membersList.toList())
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_group_button")
                    ) {
                        Text("ثبت گروه")
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "افزودن عضو جدید به گروه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام و نام خانوادگی") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onAdd(name)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ذخیره عضو")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditExpenseDialog(
    editingExpense: Expense? = null,
    members: List<GroupMember>,
    onSave: (Long, String, Int, List<Int>, String, Long, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(editingExpense?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(editingExpense?.description ?: "") }
    var category by remember { mutableStateOf(editingExpense?.category ?: "غذا") }
    var paidByMemberId by remember { mutableStateOf(editingExpense?.paidByMemberId ?: (if (members.isNotEmpty()) members.first().id else -1)) }

    // Beneficiaries selections (initially all members checked)
    val checkedBeneficiaryIds = remember {
        mutableStateListOf<Int>().apply {
            if (editingExpense != null) {
                val ids = editingExpense.beneficiaryMemberIds.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
                addAll(ids)
            } else {
                addAll(members.map { it.id })
            }
        }
    }

    // Photo Attach Support
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (editingExpense == null) "ثبت هزینه جدید" else "ویرایش اطلاعات هزینه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("مبلغ هزینه به تومان (عدد)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("بابت چه چیزی؟ (مثال: هزینه نانوایی)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CATEGORIES
                Text("دسته‌بندی خرج:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val categories = listOf("غذا", "حمل‌ونقل", "اقامت", "متفرقه")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PAYER SELECTOR
                Text("پرداخت‌کننده فاکتور:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        val isSelected = member.id == paidByMemberId
                        ElevatedAssistChip(
                            onClick = { paidByMemberId = member.id },
                            label = { Text(member.name, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.elevatedAssistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // BENEFICIARIES SELECTOR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سهم‌برندگان این هزینه (ذینفعان):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    // Toggle select all
                    TextButton(
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            if (checkedBeneficiaryIds.size == members.size) {
                                checkedBeneficiaryIds.clear()
                            } else {
                                checkedBeneficiaryIds.clear()
                                checkedBeneficiaryIds.addAll(members.map { it.id })
                            }
                        }
                    ) {
                        Text(
                            text = if (checkedBeneficiaryIds.size == members.size) "حذف همه" else "انتخاب همه اعضا",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        members.forEach { member ->
                            val isChecked = checkedBeneficiaryIds.contains(member.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) checkedBeneficiaryIds.remove(member.id)
                                        else checkedBeneficiaryIds.add(member.id)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { check ->
                                        if (check == true) checkedBeneficiaryIds.add(member.id)
                                        else checkedBeneficiaryIds.remove(member.id)
                                    },
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(member.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ATTACH PHOTO SECTION (Optional)
                Text("ضمیمه تصویر فاکتور/فیش (اختیار):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("انتخاب عکس")
                }

                if (selectedPhotoUri != null || editingExpense?.invoicePhotoPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✔️ تصویر با موفقیت ضمیمه شد.",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { selectedPhotoUri = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "حذف عکس", tint = Color.Red, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            val amt = amount.toLongOrNull() ?: 0L
                            if (amt <= 0 || description.isBlank() || paidByMemberId == -1) {
                                return@Button
                            }
                            onSave(
                                amt,
                                description,
                                paidByMemberId,
                                checkedBeneficiaryIds.toList(),
                                category,
                                editingExpense?.date ?: System.currentTimeMillis(),
                                selectedPhotoUri
                            )
                        },
                        enabled = amount.isNotBlank() && description.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_expense_button")
                    ) {
                        Text("ذخیره خرج")
                    }
                }
            }
        }
    }
}

@Composable
fun AddSettlementDialog(
    members: List<GroupMember>,
    onSave: (Int, Int, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var fromMemberId by remember { mutableStateOf(if (members.isNotEmpty()) members.first().id else -1) }
    var toMemberId by remember { mutableStateOf(if (members.size > 1) members[1].id else -1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "ثبت تراکنش تسویه‌حساب واقعی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("مبلغ تسویه شده به تومان") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settlement_amount_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("پرداخت کننده دنگ (بدهکار):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        val isSelected = member.id == fromMemberId
                        ElevatedAssistChip(
                            onClick = { fromMemberId = member.id },
                            label = { Text(member.name, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.elevatedAssistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("دریافت کننده مبلغ (طلبکار):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        val isSelected = member.id == toMemberId
                        ElevatedAssistChip(
                            onClick = { toMemberId = member.id },
                            label = { Text(member.name, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.elevatedAssistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            val amt = amount.toLongOrNull() ?: 0L
                            if (amt <= 0 || fromMemberId == -1 || toMemberId == -1 || fromMemberId == toMemberId) {
                                return@Button
                            }
                            onSave(fromMemberId, toMemberId, amt)
                        },
                        enabled = amount.isNotBlank() && fromMemberId != toMemberId,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_settlement_button")
                    ) {
                        Text("ثبت تسویه")
                    }
                }
            }
        }
    }
}

@Composable
fun GroupSelectorDialog(
    allGroups: List<DongGroup>,
    selectedGroupId: Int,
    onSelectGroup: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "انتخاب گروه فعال",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (allGroups.isEmpty()) {
                    Text(
                        text = "هنوز هیچ گروهی ساخته نشده است. به خانه بروید و اولین گروه را ایجاد کنید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allGroups) { group ->
                            val isSelected = group.id == selectedGroupId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectGroup(group.id) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "انتخاب شده",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بستن")
                }
            }
        }
    }
}
