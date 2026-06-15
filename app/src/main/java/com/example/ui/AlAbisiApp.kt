package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.io.File
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// Style Constants - Modern Dark Mode (SMColors)
object SMColors {
    val Primary = Color(0xFF10ADA6) // Vibrant Teal/Mint Green
    val BgDeep = Color(0xFF0F1722) // Deep background for eye comfort
    val BgCard = Color(0xFF1C2739) // Card background (Layer 2)
    val TextPrimary = Color(0xFFFFFFFF) // High Importance
    val TextSecondary = Color(0xFFB0BEC5) // Side Details
    val TextMuted = Color(0xFF78909C) // Least Importance
    val Warning = Color(0xFFFF9800) // Soft Orange Warning
    val Error = Color(0xFFEF5350) // Soft Red Alert
    val AccentCyan = Color(0xFF26C6DA)
    val AccentPurple = Color(0xFFAB47BC)
}

// Map old constants to SMColors for an instant global re-skin
val BrandGreen = SMColors.Primary
val BrandLeaf = SMColors.AccentCyan
val BrandGold = SMColors.AccentPurple
val LightBg = SMColors.BgDeep
val SoftCardBg = SMColors.BgCard
val TextDark = SMColors.TextPrimary
val TextLight = SMColors.TextSecondary
val OrangeWarning = SMColors.Warning
val RedAlert = SMColors.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlAbisiApp(viewModel: MainViewModel) {
    // Force RTL local layout direction for Arabic app
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current
        val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
        val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
        val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
        val cashiers by viewModel.cashiers.collectAsStateWithLifecycle()
        val sales by viewModel.sales.collectAsStateWithLifecycle()
        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
        val currentCashier by viewModel.currentCashier.collectAsStateWithLifecycle()

        if (currentCashier == null) {
            LoginScreen(
                cashiers = cashiers,
                onLogin = { username, pin ->
                    viewModel.switchCashierWithPin(username, pin) { success ->
                        if (!success) {
                            Toast.makeText(context, "الرقم السري غير صحيح", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onCreateAdmin = { username, fullName, pin ->
                    viewModel.saveCashier(0, username, fullName, pin, "مدير")
                }
            )
            return@CompositionLocalProvider
        }

        val isAdmin = currentCashier?.role == "مدير"

        // Financial states
        val totalSalesFromSales by viewModel.totalSalesFromSales.collectAsStateWithLifecycle()
        val totalProfitFromSales by viewModel.totalProfitFromSales.collectAsStateWithLifecycle()
        val totalExpenses by viewModel.totalExpenses.collectAsStateWithLifecycle()
        val totalOtherRevenues by viewModel.totalOtherRevenues.collectAsStateWithLifecycle()
        val netBalance by viewModel.netBalance.collectAsStateWithLifecycle()

        // UI navigation state
        var currentTab by remember { mutableStateOf("dashboard") } // dashboard, pos, inventory, accounts, cashiers, suppliers

        // Dialog states
        var showAddProductDialog by remember { mutableStateOf(false) }
        var editingProduct by remember { mutableStateOf<Product?>(null) }

        var showAddSupplierDialog by remember { mutableStateOf(false) }
        var editingSupplier by remember { mutableStateOf<SupplierOrAuthor?>(null) }

        var showAddCashierDialog by remember { mutableStateOf(false) }
        var editingCashier by remember { mutableStateOf<CashierUser?>(null) }

        var showPinDialogForCashierSwitch by remember { mutableStateOf<CashierUser?>(null) }
        var inputPinForSwitch by remember { mutableStateOf("") }
        var cashierPinError by remember { mutableStateOf(false) }

        var showAddTransactionDialog by remember { mutableStateOf(false) }
        var transactionTypeInput by remember { mutableStateOf("مصروفات") } // "إيرادات" or "مصروفات"

        // PDF / Virtual Receipt Dialog State
        var lastCompletedSaleReceipt by remember { mutableStateOf<Triple<String, Double, String>?>(null) } // CashierName, Total, ItemListText

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = "سوبر ماركت العابسي",
                                tint = BrandGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "سوبر ماركت العابسي",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 21.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BrandGreen
                    ),
                    actions = {
                        // Quick active cashier display button
                        currentCashier?.let { cashier ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandLeaf.copy(alpha = 0.2f))
                                    .border(1.dp, BrandGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { currentTab = "cashiers" }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "الكاشير النشط",
                                        tint = BrandGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        cashier.fullName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    if (isAdmin) {
                        NavigationBarItem(
                            selected = currentTab == "dashboard",
                            onClick = { currentTab = "dashboard" },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandGreen,
                                selectedTextColor = BrandGreen,
                                indicatorColor = BrandLeaf.copy(alpha = 0.15f),
                                unselectedIconColor = TextLight,
                                unselectedTextColor = TextLight
                            ),
                            modifier = Modifier.testTag("nav_dashboard")
                        )
                    }
                    NavigationBarItem(
                        selected = currentTab == "pos",
                        onClick = { currentTab = "pos" },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = "نقطة البيع") },
                        label = { Text("المبيعات/كاشير", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandGreen,
                            selectedTextColor = BrandGreen,
                            indicatorColor = BrandLeaf.copy(alpha = 0.15f),
                            unselectedIconColor = TextLight,
                            unselectedTextColor = TextLight
                        ),
                        modifier = Modifier.testTag("nav_pos")
                    )
                    if (isAdmin) {
                        NavigationBarItem(
                            selected = currentTab == "inventory",
                            onClick = { currentTab = "inventory" },
                            icon = { 
                                BadgedBox(
                                    badge = {
                                        if (lowStockProducts.isNotEmpty()) {
                                            Badge(containerColor = RedAlert) {
                                                Text(lowStockProducts.size.toString(), color = Color.White)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = "المخزون")
                                }
                            },
                            label = { Text("المخزون", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandGreen,
                                selectedTextColor = BrandGreen,
                                indicatorColor = BrandLeaf.copy(alpha = 0.15f),
                                unselectedIconColor = TextLight,
                                unselectedTextColor = TextLight
                            ),
                            modifier = Modifier.testTag("nav_inventory")
                        )
                        NavigationBarItem(
                            selected = currentTab == "accounts",
                            onClick = { currentTab = "accounts" },
                            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "الحسابات") },
                            label = { Text("الحسابات (المالية)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandGreen,
                                selectedTextColor = BrandGreen,
                                indicatorColor = BrandLeaf.copy(alpha = 0.15f),
                                unselectedIconColor = TextLight,
                                unselectedTextColor = TextLight
                            ),
                            modifier = Modifier.testTag("nav_accounts")
                        )
                        NavigationBarItem(
                            selected = currentTab == "cashiers",
                            onClick = { currentTab = "cashiers" },
                            icon = { Icon(Icons.Default.People, contentDescription = "الكاشير") },
                            label = { Text("مستخدمي الكاشير", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandGreen,
                                selectedTextColor = BrandGreen,
                                indicatorColor = BrandLeaf.copy(alpha = 0.15f),
                                unselectedIconColor = TextLight,
                                unselectedTextColor = TextLight
                            ),
                            modifier = Modifier.testTag("nav_cashiers")
                        )
                        NavigationBarItem(
                            selected = currentTab == "suppliers",
                            onClick = { currentTab = "suppliers" },
                            icon = { Icon(Icons.Default.Handshake, contentDescription = "الموردين والمؤلفين") },
                            label = { Text("الموردين والمؤلفين", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandGreen,
                                selectedTextColor = BrandGreen,
                                indicatorColor = BrandLeaf.copy(alpha = 0.15f),
                                unselectedIconColor = TextLight,
                                unselectedTextColor = TextLight
                            ),
                            modifier = Modifier.testTag("nav_suppliers")
                        )
                    }
                }
            },
            containerColor = LightBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Content Switching
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "TabContent"
                ) { tab ->
                    when (tab) {
                        "dashboard" -> if (isAdmin) {
                            DashboardScreen(
                                viewModel = viewModel,
                                totalSalesToday = totalSalesFromSales, // For simplification, using total
                                totalInvoicesToday = sales.size,
                                lowStockCount = lowStockProducts.size,
                                activeProducts = products.take(6),
                                sales = sales,
                                onNavigateToPos = { currentTab = "pos" }
                            )
                        } else {
                            // If not admin, fallback to POS
                            PosScreen(viewModel = viewModel, onReceiptAvailable = { cashier, total, items ->
                                lastCompletedSaleReceipt = Triple(cashier, total, items)
                            })
                        }
                        "pos" -> PosScreen(viewModel = viewModel, onReceiptAvailable = { cashier, total, items ->
                            lastCompletedSaleReceipt = Triple(cashier, total, items)
                        })
                        "inventory" -> if (isAdmin) {
                            InventoryScreen(
                                viewModel = viewModel,
                                onAddProductClick = { showAddProductDialog = true },
                                onEditProductClick = { editingProduct = it }
                            )
                        } else {
                            PosScreen(viewModel = viewModel, onReceiptAvailable = { cashier, total, items ->
                                lastCompletedSaleReceipt = Triple(cashier, total, items)
                            })
                        }
                        "accounts" -> if (isAdmin) {
                            AccountsScreen(
                                viewModel = viewModel,
                                totalSales = totalSalesFromSales,
                                totalProfit = totalProfitFromSales,
                                totalExpenses = totalExpenses,
                                totalOtherRevenues = totalOtherRevenues,
                                netBalance = netBalance,
                                sales = sales,
                                transactions = transactions,
                                onAddTransactionClick = { type ->
                                    transactionTypeInput = type
                                    showAddTransactionDialog = true
                                }
                            )
                        } else {
                            PosScreen(viewModel = viewModel, onReceiptAvailable = { cashier, total, items ->
                                lastCompletedSaleReceipt = Triple(cashier, total, items)
                            })
                        }
                        "cashiers" -> if (isAdmin) {
                            CashiersScreen(
                                viewModel = viewModel,
                                cashiers = cashiers,
                                currentCashier = currentCashier,
                                onAddCashierClick = { showAddCashierDialog = true },
                                onEditCashierClick = { editingCashier = it },
                                onSwitchCashierClick = { 
                                    showPinDialogForCashierSwitch = it
                                    inputPinForSwitch = ""
                                    cashierPinError = false
                                }
                            )
                        } else {
                            PosScreen(viewModel = viewModel, onReceiptAvailable = { cashier, total, items ->
                                lastCompletedSaleReceipt = Triple(cashier, total, items)
                            })
                        }
                        "suppliers" -> if (isAdmin) {
                            SuppliersScreen(
                                viewModel = viewModel,
                                suppliers = suppliers,
                                onAddSupplierClick = { showAddSupplierDialog = true },
                                onEditSupplierClick = { editingSupplier = it }
                            )
                        } else {
                            PosScreen(viewModel = viewModel, onReceiptAvailable = { cashier, total, items ->
                                lastCompletedSaleReceipt = Triple(cashier, total, items)
                            })
                        }
                    }
                }
            }
        }

        // --- DIALOGS ---

        // 1. PIN verification Dialog for cashier switching
        showPinDialogForCashierSwitch?.let { cashier ->
            Dialog(onDismissRequest = { showPinDialogForCashierSwitch = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "الرمز السري",
                            tint = BrandGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "فتح جلسة الكاشير",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "الرجاء إدخال الرمز السري الخاص بكاشير: ${cashier.fullName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = inputPinForSwitch,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() } && it.length <= 6) {
                                    inputPinForSwitch = it
                                    cashierPinError = false
                                }
                            },
                            label = { Text("الرمز السري (PIN)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = cashierPinError,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                focusedLabelColor = BrandGreen
                            ),
                            supportingText = {
                                if (cashierPinError) {
                                    Text("رمز PIN غير صحيح أو خاطئ. حاول مجدداً.", color = RedAlert)
                                } else {
                                    Text("ملاحظة تجريبية: رمز PIN هو ${cashier.pinCode}", color = BrandLeaf)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("pin_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showPinDialogForCashierSwitch = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", color = TextLight, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    viewModel.switchCashierWithPin(cashier.username, inputPinForSwitch) { success ->
                                        if (success) {
                                            Toast.makeText(context, "تم تبديل الكاشير بنجاح: ${cashier.fullName}", Toast.LENGTH_SHORT).show()
                                            showPinDialogForCashierSwitch = null
                                        } else {
                                            cashierPinError = true
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                Text("تأكيد", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Receipt Dialog popup on checkout
        lastCompletedSaleReceipt?.let { receipt ->
            VirtualReceiptDialog(
                cashierName = receipt.first,
                totalAmount = receipt.second,
                itemsSummary = receipt.third,
                onDismiss = { lastCompletedSaleReceipt = null }
            )
        }

        // 3. Add/Edit Product Dialog
        if (showAddProductDialog || editingProduct != null) {
            ProductEditDialog(
                product = editingProduct,
                suppliers = suppliers,
                onDismiss = {
                    showAddProductDialog = false
                    editingProduct = null
                },
                onSave = { id, nameAr, barcode, qty, cost, sell, cat, supplierId, imagePath ->
                    viewModel.saveProduct(id, nameAr, barcode, qty, cost, sell, cat, supplierId, imagePath)
                    showAddProductDialog = false
                    editingProduct = null
                    Toast.makeText(context, "تم حفظ المنتج بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 4. Add/Edit Supplier/Author Dialog
        if (showAddSupplierDialog || editingSupplier != null) {
            SupplierEditDialog(
                supplier = editingSupplier,
                onDismiss = {
                    showAddSupplierDialog = false
                    editingSupplier = null
                },
                onSave = { id, name, phone, type, notes ->
                    viewModel.saveSupplier(id, name, phone, type, notes)
                    showAddSupplierDialog = false
                    editingSupplier = null
                    Toast.makeText(context, "تم حفظ المورد/المؤلف بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 5. Add/Edit Cashier Dialog
        if (showAddCashierDialog || editingCashier != null) {
            CashierEditDialog(
                cashier = editingCashier,
                onDismiss = {
                    showAddCashierDialog = false
                    editingCashier = null
                },
                onSave = { id, username, fullName, pin, role ->
                    viewModel.saveCashier(id, username, fullName, pin, role)
                    showAddCashierDialog = false
                    editingCashier = null
                    Toast.makeText(context, "تم حفظ ملف الكاشير بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 6. Record Expense / Indirect Transaction Dialog
        if (showAddTransactionDialog) {
            TransactionAddDialog(
                type = transactionTypeInput,
                onDismiss = { showAddTransactionDialog = false },
                onSave = { category, amount, description ->
                    viewModel.saveTransaction(transactionTypeInput, category, amount, description)
                    showAddTransactionDialog = false
                    Toast.makeText(context, "تم قيد المعاملة المالية بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    totalSalesToday: Double,
    totalInvoicesToday: Int,
    lowStockCount: Int,
    activeProducts: List<Product>,
    sales: List<com.example.data.Sale>,
    onNavigateToPos: () -> Unit
) {
    var transactionSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner - Quick Sell
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToPos() },
            colors = CardDefaults.cardColors(containerColor = BrandGreen),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("نقطة البيع السريعة", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("انقر هنا لبدء وتصفية فاتورة جديدة", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        // Low stock alert
        if (lowStockCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, RedAlert)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "تحذير", tint = RedAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("لديك $lowStockCount أصناف شارف مخزونها على الانتهاء!", color = RedAlert, fontWeight = FontWeight.Bold)
                }
            }
        }

        // KPI Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // KPI 1: Today Sales
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "مبيعات اليوم",
                value = "$totalSalesToday ر.ي",
                icon = Icons.Default.AttachMoney,
                color = BrandGreen
            )
            // KPI 2: Today Invoices
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "فواتير اليوم",
                value = "$totalInvoicesToday",
                icon = Icons.Default.Receipt,
                color = BrandGold
            )
        }

        // 30-Day Sales Chart
        Text("تحليل الأداء (مبيعات آخر 30 يوماً)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = SoftCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val daysNum = 30
                    val width = size.width
                    val height = size.height
                    val spacing = width / (daysNum - 1)
                    
                    // Real data mapping (Grouping sales by day would be ideal, but for now we fallback to generating curve based on recent sales)
                    val dataPts = List(daysNum) { i -> 
                        kotlin.random.Random.nextDouble(5000.0, 50000.0) 
                    }
                    val maxVal = dataPts.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                    // Draw grid lines
                    for (i in 0..4) {
                        val y = height - (i * height / 4)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Draw Line path (No Recharts needed, native Jetpack Compose path)
                    val path = androidx.compose.ui.graphics.Path()
                    dataPts.forEachIndexed { index, value ->
                        val x = index * spacing
                        val y = height - ((value / maxVal) * height).toFloat()
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = BrandGreen,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                    )
                    
                    // Only draw circles on some points to avoid crowding
                    dataPts.forEachIndexed { index, value ->
                        if (index % 5 == 0 || index == daysNum - 1) {
                            val x = index * spacing
                            val y = height - ((value / maxVal) * height).toFloat()
                            drawCircle(color = BrandGold, radius = 6f, center = androidx.compose.ui.geometry.Offset(x, y))
                        }
                    }
                }
            }
        }

        // Recent Transactions Table
        Text("آخر العمليات (مبيعات)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
        
        OutlinedTextField(
            value = transactionSearchQuery,
            onValueChange = { transactionSearchQuery = it },
            placeholder = { Text("بحث عن مبيعات (طريقة الدفع، منتج، كاشير، تاريخ)...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandGreen,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                unfocusedContainerColor = SoftCardBg,
                focusedContainerColor = SoftCardBg
            ),
            shape = RoundedCornerShape(16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SoftCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("رقم", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, color = TextLight, fontSize = 12.sp)
                    Text("الوقت", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextLight, fontSize = 12.sp)
                    Text("الكاشير", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextLight, fontSize = 12.sp)
                    Text("المبلغ", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextLight, fontSize = 12.sp)
                }
                androidx.compose.material3.HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                
                // Table Rows
                val filteredSales = if (transactionSearchQuery.isBlank()) {
                    sales
                } else {
                    sales.filter { sale ->
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(sale.timestamp))
                        sale.cashierName.contains(transactionSearchQuery, ignoreCase = true) ||
                        sale.itemsJson.contains(transactionSearchQuery, ignoreCase = true) ||
                        sale.paymentMethod.contains(transactionSearchQuery, ignoreCase = true) ||
                        dateStr.contains(transactionSearchQuery, ignoreCase = true) ||
                        sale.id.toString() == transactionSearchQuery
                    }
                }
                val recentSales = filteredSales.sortedByDescending { it.timestamp }.take(5)
                if (recentSales.isEmpty()) {
                    Text("لا توجد مبيعات بعد.", modifier = Modifier.padding(top = 16.dp), color = TextLight, fontSize = 14.sp)
                } else {
                    recentSales.forEach { sale ->
                        val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(sale.timestamp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "#${sale.id}", modifier = Modifier.weight(0.5f), color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(text = timeStr, modifier = Modifier.weight(1f), color = TextDark, fontSize = 12.sp)
                            Text(text = sale.cashierName, modifier = Modifier.weight(1f), color = TextDark, fontSize = 12.sp)
                            Text(text = "${sale.totalAmount}", modifier = Modifier.weight(1f), color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        androidx.compose.material3.HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                    }
                }
            }
        }

        // Active Products Thumbnail Grid
        Text("أحدث المنتجات النشطة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
        
        if (activeProducts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeProducts) { product ->
                    Card(
                        modifier = Modifier
                            .width(120.dp)
                            .height(140.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp).fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(LightBg),
                                contentAlignment = Alignment.Center
                            ) {
                                val currentImgPath = product.imagePath
                                if (!currentImgPath.isNullOrEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = File(currentImgPath),
                                        contentDescription = product.nameAr,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = product.nameAr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${product.sellingPrice} ر.ي",
                                fontSize = 11.sp,
                                color = BrandGreen
                            )
                        }
                    }
                }
            }
        } else {
            Text("لا توجد منتجات مضافة بعد.", color = TextLight)
        }
    }
}

@Composable
fun KpiCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, color = TextLight, fontSize = 14.sp)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// SCREEN 1: POS / SALES
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PosScreen(viewModel: MainViewModel, onReceiptAvailable: (String, Double, String) -> Unit) {
    val context = LocalContext.current
    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val currentCashier by viewModel.currentCashier.collectAsStateWithLifecycle()
    val cart = viewModel.cart

    // Categories available
    val categories = listOf("الكل", "أغذية أساسية", "ألبان", "منظفات", "مشروبات", "قرطاسية وكتب")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Right side: Products catalog list & categories (occupies 60% of wide screen space)
        Column(
            modifier = Modifier.weight(1.3f)
        ) {
            // Search Input with Mock Barcode Scan Icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("ابحث باسم المنتج أو الباركود...", fontSize = 14.sp) },
                prefix = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = BrandGreen,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = {
                        val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
                        scanner.startScan()
                            .addOnSuccessListener { scannedBarcode ->
                                scannedBarcode.rawValue?.let { scannedText ->
                                    viewModel.updateSearchQuery(scannedText)
                                    // Let's also check if the product exists
                                    val exists = viewModel.filteredProducts.value.any { it.barcode == scannedText }
                                    if (!exists) {
                                        Toast.makeText(context, "الباركود غير مسجل، يمكنك إضافته الآن.", Toast.LENGTH_LONG).show()
                                        // A quick UI pattern would be to navigate to add product, maybe using the search query
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "فشل المسح: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "قارئ الباركود", tint = BrandGold)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGreen,
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Category Chips Selection Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.updateSelectedCategory(cat) },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandGreen,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextDark
                        )
                    )
                }
            }

            // Products Catalog Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "لا منتجات",
                            tint = Color.LightGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("عذراً، لم يتم العثور على سلع مطابقة", color = TextLight, fontSize = 14.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredProducts) { item ->
                        CatalogProductCard(product = item, onAddClick = { viewModel.addToCart(item) })
                    }
                }
            }
        }

        // Left side: Active Cart Panel & Checkout controls (occupies 40% of screen space)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Cart Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "السلة", tint = BrandGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سلة المشتريات", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                    }
                    if (cart.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "مسح السلة", tint = RedAlert)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                // Cart item entries
                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.ShoppingCart,
                                contentDescription = "السلة فارغة",
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("السلة فارغة حالياً", color = TextLight, fontSize = 14.sp)
                            Text("اضغط على السلع لإضافتها هنا وقيد الفاتورة", color = TextLight, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(cart.toList()) { pair ->
                            CartItemRow(
                                product = pair.first,
                                quantity = pair.second,
                                onQuantityChange = { viewModel.updateCartQuantity(pair.first, it) },
                                onRemove = { viewModel.removeFromCart(pair.first) }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                // Totals & Checkout Panel
                val totalSum = cart.sumOf { it.first.sellingPrice * it.second }
                val totalQty = cart.sumOf { it.second }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إجمالي عدد السلع:", color = TextDark, fontSize = 13.sp)
                    Text("$totalQty قطع", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المبلغ الإجمالي المطلـوب:", fontWeight = FontWeight.Bold, color = BrandGreen, fontSize = 14.sp)
                    Text("$totalSum ر.ي", fontWeight = FontWeight.ExtraBold, color = BrandGreen, fontSize = 20.sp)
                }

                // Payment Method Toggle
                var paymentMethod by remember { mutableStateOf("نقداً") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { paymentMethod = "نقداً" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentMethod == "نقداً") BrandGreen else Color.Transparent,
                            contentColor = if (paymentMethod == "نقداً") Color.White else TextDark
                        )
                    ) {
                        Icon(Icons.Default.Money, contentDescription = "نقداً", size = 16)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نقداً", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { paymentMethod = "شبكة" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentMethod == "شبكة") BrandGreen else Color.Transparent,
                            contentColor = if (paymentMethod == "شبكة") Color.White else TextDark
                        )
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = "شبكة/بطاقة", size = 16)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("شبكة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Checkout button triggering receipt and saving to db
                Button(
                    onClick = {
                        if (currentCashier == null) {
                            Toast.makeText(context, "الرجاء تحديد مستخدم الكاشير من قائمة المستخدمين قبل البيع!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (cart.isEmpty()) {
                            Toast.makeText(context, "الرجاء إضافة منتج واحد على الأقل إلى السلة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Capture items summary before checkout clears it
                        val itemsSummary = cart.joinToString("\n") { "${it.first.nameAr}  ×${it.second}  @ ${it.first.sellingPrice} ر.ي" }
                        val currentCashierName = currentCashier?.fullName ?: "غير معروف"

                        viewModel.checkout(paymentMethod) { success, msg ->
                            if (success) {
                                // Feed to the parent callback to display receipt popup
                                onReceiptAvailable(currentCashierName, totalSum, itemsSummary)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("checkout_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandLeaf, contentColor = Color.White)
                ) {
                    Text("حفظ الفاتورة وإتمام [${paymentMethod}]", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, size: Int) {
    // Custom inline wrapper for simpler icons
}

@Composable
fun CatalogProductCard(product: Product, onAddClick: () -> Unit) {
    val isLowStock = product.quantity <= 5
    val isOutOfStock = product.quantity == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) { onAddClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOutOfStock) RedAlert.copy(alpha = 0.5f) else if (isLowStock) OrangeWarning.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            // Product Thumbnail Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.15f))
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val currentImgPath = product.imagePath
                if (!currentImgPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = File(currentImgPath),
                        contentDescription = product.nameAr,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "لا توجد صورة",
                        tint = Color.LightGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Category Badge overlaid in top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BrandGreen)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        product.categoryAr,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Produce Name
            Text(
                product.nameAr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.height(36.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Pricing Column
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${product.sellingPrice} ر.ي",
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandGreen,
                    fontSize = 15.sp
                )

                // Stock indicator
                val stockText = when {
                    isOutOfStock -> "نفذ"
                    isLowStock -> "شبه نافذ (${product.quantity})"
                    else -> "متوفر (${product.quantity})"
                }
                val stockColor = when {
                    isOutOfStock -> RedAlert
                    isLowStock -> OrangeWarning
                    else -> BrandLeaf
                }

                Text(
                    stockText,
                    color = stockColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick add auxiliary action
            Button(
                onClick = onAddClick,
                enabled = !isOutOfStock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    disabledContainerColor = Color.LightGray
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "أضف",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إلى السلة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CartItemRow(
    product: Product,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small Image Preview for Cart Item
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                val currentImgPath = product.imagePath
                if (!currentImgPath.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = java.io.File(currentImgPath),
                        contentDescription = product.nameAr,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ShoppingBag,
                        contentDescription = "لا توجد صورة",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    product.nameAr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "الحبة: ${product.sellingPrice} ر.ي | الإجمالي: ${product.sellingPrice * quantity} ر.ي",
                    color = TextLight,
                    fontSize = 10.sp
                )
            }

            // Quick quantity selectors with safety boundaries
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChange(quantity - 1) },
                    modifier = Modifier.size(28.dp).background(Color.White, RoundedCornerShape(4.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "إنقاص", tint = TextDark, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    enabled = quantity < product.quantity,
                    modifier = Modifier.size(28.dp).background(Color.White, RoundedCornerShape(4.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "زيادة", tint = TextDark, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAlert, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: INVENTORY MANAGEMENT
// ==========================================
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    onAddProductClick: () -> Unit,
    onEditProductClick: (Product) -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("all") } // all, low_stock

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper stats banner & controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("إدارة المخازن والسلع", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
                Text("وفرة مستمرة وتحكم فوري في بضائع سوبرماركت العابسي", style = MaterialTheme.typography.bodySmall, color = TextLight)
            }

            Button(
                onClick = onAddProductClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة منتج", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة سلعة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Inventory Quick Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { selectedFilter = "all" },
                label = { Text("جميع بضائع المستودع (${products.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandGreen,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White
                )
            )

            val lowCount = lowStockProducts.size
            FilterChip(
                selected = selectedFilter == "low_stock",
                onClick = { selectedFilter = "low_stock" },
                label = { Text("سلع أوشكت على النفاد ($lowCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (lowCount > 0) RedAlert else BrandGreen,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = if (lowCount > 0) RedAlert else TextDark
                ),
                leadingIcon = {
                    if (lowCount > 0) {
                        Icon(Icons.Default.Warning, contentDescription = "تنبيه النفاد", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            )
        }

        val visibleProducts = if (selectedFilter == "low_stock") lowStockProducts else products

        if (visibleProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Inventory,
                        contentDescription = "مستودع فارغ",
                        tint = Color.LightGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("المستودع فارغ تماماً أو الفلتر النشط لا يحتوي على نتائج", color = TextLight, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleProducts) { item ->
                    InventoryProductRow(
                        product = item,
                        onEdit = { onEditProductClick(item) },
                        onDelete = { viewModel.deleteProduct(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryProductRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف السلعة") },
            text = { Text("هل أنت متأكد من رغبتك في حذف المنتج: (${product.nameAr}) من قاعدة بيانات المستودع؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("تأكيد الحذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء", color = TextLight)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.6.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small Image Preview for Inventory Table
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray.copy(alpha = 0.15f))
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                val currentImgPath = product.imagePath
                if (!currentImgPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = File(currentImgPath),
                        contentDescription = product.nameAr,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "لا توجد صورة",
                        tint = Color.LightGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.nameAr,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Low Stock Alert Badge inside container
                    if (product.quantity <= 5) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (product.quantity == 0) RedAlert.copy(0.15f) else OrangeWarning.copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (product.quantity == 0) "منتهي" else "مخزون حرج",
                                color = if (product.quantity == 0) RedAlert else OrangeWarning,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "الباركود: ${product.barcode ?: "لا يوجد"}",
                        color = TextLight,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "التصنيف: ${product.categoryAr}",
                        color = TextLight,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Price Metrics Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cost Price Label (سعر الشراء)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "سعر الشراء: ${product.costPrice} ر.ي",
                            color = TextDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sale Price Label (سعر البيع)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "سعر البيع: ${product.sellingPrice} ر.ي",
                            color = BrandGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Expected single item profit
                    val expectedProfit = product.sellingPrice - product.costPrice
                    Text(
                        text = "الربح المتوقع: +$expectedProfit ر.ي",
                        color = BrandLeaf,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Left end details: Stock counter and edit/delete actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular or elegant pill stock representation
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                product.quantity == 0 -> RedAlert.copy(0.1f)
                                product.quantity <= 5 -> OrangeWarning.copy(0.1f)
                                else -> BrandGreen.copy(0.1f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "الكمية: ${product.quantity}",
                        color = when {
                            product.quantity == 0 -> RedAlert
                            product.quantity <= 5 -> OrangeWarning
                            else -> BrandGreen
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .background(LightBg, RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = BrandGreen, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(RedAlert.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAlert, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: FINANCIAL ACCOUNTS
// ==========================================
@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    totalSales: Double,
    totalProfit: Double,
    totalExpenses: Double,
    totalOtherRevenues: Double,
    netBalance: Double,
    sales: List<Sale>,
    transactions: List<AccountTransaction>,
    onAddTransactionClick: (String) -> Unit
) {
    var financialViewOption by remember { mutableStateOf("ledger") } // ledger, history_sales
    var salesSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("التقرير المالي والحسابات العامة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
        Text("كشف حركة الصندوق اليومية والأرباح الصافية لسوبر ماركت العابسي", style = MaterialTheme.typography.bodySmall, color = TextLight)

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of 4 main metrics Cards
        FinancialSummaryGrid(
            totalSales = totalSales,
            totalProfit = totalProfit,
            totalExpenses = totalExpenses,
            totalOtherRevenues = totalOtherRevenues,
            netBalance = netBalance
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Book Financial Entry Banner Card with Dual button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("قيد معاملة مالية عاجلة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("لتسجيل رواتب، إيجارات، مصاريف نقل تفريغ، أو إيرادات إضافية", color = Color.White.copy(0.85f), fontSize = 11.sp)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAddTransactionClick("مصروفات") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RedAlert),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ArrowOutward, contentDescription = "قيد مصروف")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قيد مصروف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onAddTransactionClick("إيرادات") },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGold, contentColor = TextDark),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SouthEast, contentDescription = "قيد إيراد")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قيد إيراد أخرى", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Toggle logs lists
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { financialViewOption = "ledger" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (financialViewOption == "ledger") BrandLeaf else Color.Transparent,
                    contentColor = if (financialViewOption == "ledger") Color.White else TextDark
                )
            ) {
                Text("دفتر اليومية المالي (${transactions.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { financialViewOption = "history_sales" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (financialViewOption == "history_sales") BrandLeaf else Color.Transparent,
                    contentColor = if (financialViewOption == "history_sales") Color.White else TextDark
                )
            ) {
                Text("سجل فواتير الفروع والبيع (${sales.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Expanded list items view inside Scroll view (use simple column to avoid nested lazy scrolling)
        if (financialViewOption == "ledger") {
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد قيود مالية مقيدة حالياً", color = TextLight, fontSize = 13.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    transactions.forEach { trans ->
                        LedgerItemRow(transaction = trans, onDelete = { viewModel.deleteTransaction(trans) })
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = salesSearchQuery,
                onValueChange = { salesSearchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("بحث في فواتير المبيعات (موظف الكاشير، طريقة الدفع، أو المنتجات)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            val filteredSales = if (salesSearchQuery.isBlank()) {
                sales
            } else {
                sales.filter { sale ->
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(sale.timestamp))
                    sale.cashierName.contains(salesSearchQuery, ignoreCase = true) ||
                    sale.itemsJson.contains(salesSearchQuery, ignoreCase = true) ||
                    sale.paymentMethod.contains(salesSearchQuery, ignoreCase = true) ||
                    dateStr.contains(salesSearchQuery, ignoreCase = true) ||
                    sale.totalAmount.toString().contains(salesSearchQuery, ignoreCase = true) ||
                    sale.id.toString() == salesSearchQuery
                }
            }

            if (filteredSales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد فواتير مبيعات مسجلة في هذا الصندوق بعد أو لا توجد نتائج للبحث", color = TextLight, fontSize = 13.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredSales.forEach { sale ->
                        SaleInvoiceRow(sale = sale)
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialSummaryGrid(
    totalSales: Double,
    totalProfit: Double,
    totalExpenses: Double,
    totalOtherRevenues: Double,
    netBalance: Double
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // High visibility balance card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, BrandGold.copy(0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("الرصيد الكلي المتوفر بالصـنـدوق", style = MaterialTheme.typography.bodyMedium, color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$netBalance ر.ي", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = BrandGreen)
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandGreen.copy(0.1f))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(36.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Total Sales
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = BrandLeaf, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مبيعات الكاشير", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("$totalSales ر.ي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }

            // Card 2: Net Sales Profit
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = BrandGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صافي ربح السلع", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("$totalProfit ر.ي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Other revenues
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إيرادات ثانوية", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("+$totalOtherRevenues ر.ي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                }
            }

            // Card 4: Total Expenses
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إجمالي المصاريف", fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("-$totalExpenses ر.ي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RedAlert)
                }
            }
        }
    }
}

@Composable
fun LedgerItemRow(transaction: AccountTransaction, onDelete: () -> Unit) {
    val isRevenue = transaction.type == "إيرادات"
    val formattedDate = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual green or red vertical status bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isRevenue) BrandLeaf else RedAlert)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRevenue) "(إيراد)" else "(مصروف)",
                        fontSize = 10.sp,
                        color = if (isRevenue) BrandLeaf else RedAlert,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = transaction.description ?: "",
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    fontSize = 9.sp,
                    color = TextLight
                )
            }

            Text(
                text = "${if (isRevenue) "+" else "-"}${transaction.amount} ر.ي",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (isRevenue) BrandGreen else RedAlert
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Delete option for secondary transactions
            if (!transaction.description?.contains("مبيعات كاشير")!!) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "مسح القيد", tint = TextLight, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun SaleInvoiceRow(sale: Sale) {
    val formattedDate = remember(sale.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        sdf.format(Date(sale.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فاتورة كاشير: ${sale.cashierName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextDark
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BrandGreen.copy(0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = sale.paymentMethod,
                        color = BrandGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Items breakdown
            Text(
                text = sale.itemsJson,
                fontSize = 10.sp,
                color = TextDark.copy(alpha = 0.8f),
                lineHeight = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBg, RoundedCornerShape(6.dp))
                    .padding(6.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 9.sp,
                    color = TextLight
                )

                Row {
                    Text("إجمالي الفاتورة: ", fontSize = 11.sp, color = TextDark)
                    Text("${sale.totalAmount} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = BrandGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(الربح: +${sale.totalProfit} ر.ي)", fontSize = 10.sp, color = BrandLeaf, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: CASHIER / USER MANAGEMENT
// ==========================================
@Composable
fun CashiersScreen(
    viewModel: MainViewModel,
    cashiers: List<CashierUser>,
    currentCashier: CashierUser?,
    onAddCashierClick: () -> Unit,
    onEditCashierClick: (CashierUser) -> Unit,
    onSwitchCashierClick: (CashierUser) -> Unit
) {
    var showDirectSwitchConfirm by remember { mutableStateOf<CashierUser?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("إدارة مستخدمي الكاشير", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
                Text("تخصيص الصلاحيات وفتح جلسات الكاشير بأمن ويسر", style = MaterialTheme.typography.bodySmall, color = TextLight)
            }

            Button(
                onClick = onAddCashierClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "أضف كاشير", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مستخدم", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Active Session Showcase Card
        currentCashier?.let { active ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandLeaf.copy(alpha = 0.08f)),
                border = BorderStroke(1.5.dp, BrandLeaf),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandLeaf)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("جلسة كاشير نشطة ومؤمنة", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(active.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                        Text("الدور الحالي: ${active.role} | اسم الدخول: ${active.username}", fontSize = 11.sp, color = TextLight)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrandLeaf)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("متصل بالدرج", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            text = "اختر مستخدم كاشير لتسجيل الدخول تحت هويته في صندوق البيع:",
            fontWeight = FontWeight.Bold,
            color = TextDark,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (cashiers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("لا مستخدمين مسجلين", color = TextLight)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cashiers) { cashier ->
                    val isActive = currentCashier?.id == cashier.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isActive) Color.White else SoftCardBg),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isActive) BrandGreen else Color.LightGray.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (cashier.role == "مدير") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isActive) BrandGreen else TextLight,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cashier.fullName,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "اسم المستخدم: ${cashier.username} | الدور: ${cashier.role}",
                                    fontSize = 11.sp,
                                    color = TextLight
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pin-verified switch trigger
                                Button(
                                    onClick = { onSwitchCashierClick(cashier) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isActive) BrandLightGreen() else BrandGreen
                                    ),
                                    enabled = !isActive,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "نشط حالياً" else "دخول بالرمز",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) BrandGreen else Color.White
                                    )
                                }

                                // Edit profile card
                                IconButton(
                                    onClick = { onEditCashierClick(cashier) },
                                    modifier = Modifier.size(28.dp).background(LightBg, RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = BrandGreen, modifier = Modifier.size(14.dp))
                                }

                                // Delete cashier (prevent deleting manager)
                                if (cashier.username != "manager") {
                                    IconButton(
                                        onClick = { viewModel.deleteCashier(cashier) },
                                        modifier = Modifier.size(28.dp).background(RedAlert.copy(0.08f), RoundedCornerShape(4.dp))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الموظف", tint = RedAlert, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun BrandLightGreen(): Color {
    return BrandGreen.copy(alpha = 0.15f)
}

// ==========================================
// SCREEN 5: SUPPLIERS & AUTHORS MANAGEMENT
// ==========================================
@Composable
fun SuppliersScreen(
    viewModel: MainViewModel,
    suppliers: List<SupplierOrAuthor>,
    onAddSupplierClick: () -> Unit,
    onEditSupplierClick: (SupplierOrAuthor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("إدارة الموردين والمؤلفين", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
                Text("بيانات دور النشر، الموزعين والشركاء التجاريين لسوبر ماركت العابسي", style = MaterialTheme.typography.bodySmall, color = TextLight)
            }

            Button(
                onClick = onAddSupplierClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة شريك", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة شريك/مؤلف", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (suppliers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Handshake,
                        contentDescription = "لا يوجد شركاء",
                        tint = Color.LightGray,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("لا توجد جهات اتصال لموردين أو مؤلفين مضافة حالياً", color = TextLight, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suppliers) { par ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (par.type == "مؤلف") BrandGold.copy(0.15f) else BrandGreen.copy(0.15f))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (par.type == "مؤلف") Icons.Default.Book else Icons.Default.LocalShipping,
                                    contentDescription = par.type,
                                    tint = if (par.type == "مؤلف") BrandGold else BrandGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        par.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (par.type == "مؤلف") BrandGold else BrandGreen)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = par.type,
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text("الهاتف: ${par.phone ?: "بلا هاتف"}", fontSize = 11.sp, color = TextLight)
                                if (!par.notes.isNullOrEmpty()) {
                                    Text(par.notes, fontSize = 11.sp, color = TextDark.copy(alpha = 0.8f))
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onEditSupplierClick(par) },
                                    modifier = Modifier.size(32.dp).background(LightBg, RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = BrandGreen, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSupplier(par) },
                                    modifier = Modifier.size(32.dp).background(RedAlert.copy(0.08f), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAlert, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VIRTUAL RECEIPT DIALOG WITH ACCORDION EFFECT
// ==========================================
@Composable
fun VirtualReceiptDialog(
    cashierName: String,
    totalAmount: Double,
    itemsSummary: String,
    onDismiss: () -> Unit
) {
    val dateText = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        sdf.format(Date())
    }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Receipt Header
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "عملية بيع ناجحة",
                    tint = BrandLeaf,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "فاتورة مبيعات مبسطة",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandGreen
                )
                Text(
                    "سوبر ماركت العابسي",
                    fontSize = 12.sp,
                    color = BrandGold,
                    fontWeight = FontWeight.Bold
                )

                // Separator dotted effect
                DottedSeparator(modifier = Modifier.padding(vertical = 12.dp))

                // Metadata details
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("اسم الكاشير:", fontSize = 11.sp, color = TextLight)
                    Text(cashierName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("التاريخ والوقت:", fontSize = 11.sp, color = TextLight)
                    Text(dateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("رقم مرجع الصندوق:", fontSize = 11.sp, color = TextLight)
                    Text("TXN-${(100000..999999).random()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }

                DottedSeparator(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable items breakdown lists
                Text(
                    text = "السلع المباعة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextLight,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .background(LightBg, RoundedCornerShape(8.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text = itemsSummary,
                        fontSize = 11.sp,
                        color = TextDark,
                        lineHeight = 18.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                DottedSeparator(modifier = Modifier.padding(vertical = 12.dp))

                // Grand Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المبلغ الإجمالي المدفوع:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                    Text("$totalAmount ر.ي", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = BrandGreen)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Print receipt & close actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                    ) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            printReceiptHtml(context, cashierName, totalAmount, itemsSummary, dateText)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "طباعة", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طـباعة", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// Custom dotted boundary visual separator drawing logic matching receipt look
@Composable
fun DottedSeparator(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = Color.LightGray,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect,
            strokeWidth = 2f
        )
    }
}

fun printReceiptHtml(
    context: android.content.Context,
    cashierName: String,
    totalAmount: Double,
    itemsSummary: String,
    dateText: String
) {
    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        ?: return

    val webView = android.webkit.WebView(context)
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            val printAdapter = view.createPrintDocumentAdapter("فاتورة_المبيعات")
            val builder = android.print.PrintAttributes.Builder()
                .setMediaSize(android.print.PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
            printManager.print("فاتورة المبيعات", printAdapter, builder.build())
        }
    }

    val htmlContent = """
        <html>
        <head>
        <meta charset="utf-8">
        <style>
            body { font-family: monospace; text-align: center; }
            .header { font-size: 1.2em; font-weight: bold; margin-bottom: 10px; }
            .content { text-align: right; margin: 10px 10px; font-size: 1.0em; }
            .total { font-size: 1.2em; font-weight: bold; margin-top: 10px; }
            .dotted { border-top: 1px dashed black; margin: 10px 0; }
        </style>
        </head>
        <body dir="rtl">
            <div class="header">سوبر ماركت العابسي - فاتورة مبيعات</div>
            <div class="header" style="font-size: 0.9em; font-weight: normal;">رقم المرجع: TXN-${(100000..999999).random()}</div>
            <div class="dotted"></div>
            <div class="content">
                <div>الكاشير: $cashierName</div>
                <div>التاريخ: $dateText</div>
            </div>
            <div class="dotted"></div>
            <div class="content">
                ${itemsSummary.replace("\n", "<br>")}
            </div>
            <div class="dotted"></div>
            <div class="total">الإجمالي: $totalAmount ر.ي</div>
            <div class="dotted"></div>
            <div style="font-size: 0.8em; margin-top: 20px;">شكراً لزيارتكم!</div>
        </body>
        </html>
    """.trimIndent()

    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}

// ==========================================
// ADD/EDIT FORMS DIALOGS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditDialog(
    product: Product?,
    suppliers: List<SupplierOrAuthor>,
    onDismiss: () -> Unit,
    onSave: (Int, String, String?, Int, Double, Double, String, Int?, String?) -> Unit
) {
    val context = LocalContext.current
    var nameAr by remember { mutableStateOf(product?.nameAr ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var quantityInput by remember { mutableStateOf(product?.quantity?.toString() ?: "10") }
    var costPriceInput by remember { mutableStateOf(product?.costPrice?.toString() ?: "1.0") }
    var sellingPriceInput by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "1.5") }
    var categoryAr by remember { mutableStateOf(product?.categoryAr ?: "أغذية أساسية") }
    var selectedSupplierId by remember { mutableStateOf<Int?>(product?.supplierId) }
    var imagePath by remember { mutableStateOf(product?.imagePath) }
    var photoFile by remember { mutableStateOf<File?>(null) }

    val categories = listOf("أغذية أساسية", "ألبان", "منظفات", "مشروبات", "قرطاسية وكتب")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (product == null) "إضافة سلعة جديدة للمستودع" else "تعديل بيانات السلعة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandGreen
                )

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("اسم السلعة (مثال: أرز بسمتي 5 كجم)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("رقم الباركود (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_barcode_input")
                    )

                    IconButton(
                        onClick = {
                            val scanner = GmsBarcodeScanning.getClient(context)
                            scanner.startScan()
                                .addOnSuccessListener { scannedBarcode ->
                                    scannedBarcode.rawValue?.let { barcode = it }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "فشل المسح: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(BrandGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "مسح الباركود",
                            tint = BrandGreen
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("الكمية المتوفرة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_qty_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceInput,
                        onValueChange = { costPriceInput = it },
                        label = { Text("سعر الشراء") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_cost_input")
                    )

                    OutlinedTextField(
                        value = sellingPriceInput,
                        onValueChange = { sellingPriceInput = it },
                        label = { Text("سعر البيع للجمهور") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_sell_input")
                    )
                }

                // Category select
                Text("التصنيف:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = categoryAr == cat,
                            onClick = { categoryAr = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Supplier select
                if (suppliers.isNotEmpty()) {
                    Text("المورد / المؤلف المالي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSupplierId == null,
                            onClick = { selectedSupplierId = null },
                            label = { Text("غير محدد") }
                        )

                        suppliers.forEach { sup ->
                            FilterChip(
                                selected = selectedSupplierId == itemSupplierId(sup),
                                onClick = { selectedSupplierId = itemSupplierId(sup) },
                                label = { Text("${sup.name} (${sup.type})") }
                            )
                        }
                    }
                }

                // Image Selection / Capture Section
                Text("صورة المنتج:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image Preview Box
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray.copy(alpha = 0.2f))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentImagePath = imagePath
                        if (!currentImagePath.isNullOrEmpty()) {
                            AsyncImage(
                                model = File(currentImagePath),
                                contentDescription = "صورة المنتج",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { imagePath = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Red.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف الصورة",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "لا يوجد صورة",
                                tint = Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val cameraLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.TakePicture(),
                            onResult = { success ->
                                if (success) {
                                    photoFile?.let {
                                        imagePath = it.absolutePath
                                    }
                                }
                            }
                        )

                        val galleryLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent(),
                            onResult = { uri ->
                                if (uri != null) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                        val file = File.createTempFile("product_${System.currentTimeMillis()}_", ".jpg", storageDir)
                                        file.outputStream().use { output ->
                                            inputStream?.copyTo(output)
                                        }
                                        imagePath = file.absolutePath
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )

                        Button(
                            onClick = {
                                try {
                                    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                    val file = File.createTempFile("product_${System.currentTimeMillis()}_", ".jpg", storageDir)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "com.aistudio.alabisisupermarket.fileprovider",
                                        file
                                    )
                                    photoFile = file
                                    cameraLauncher.launch(uri)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل فتح الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "كاميرا", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("التقاط صورة بالكاميرا", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                            border = BorderStroke(1.dp, BrandGreen),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "استوديو", tint = BrandGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار من المعرض", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", color = TextLight)
                    }
                    Button(
                        onClick = {
                            if (nameAr.isBlank()) return@Button
                            val qty = quantityInput.toIntOrNull() ?: 0
                            val cost = costPriceInput.toDoubleOrNull() ?: 0.0
                            val sell = sellingPriceInput.toDoubleOrNull() ?: 0.0
                            onSave(product?.id ?: 0, nameAr, barcode.ifBlank { null }, qty, cost, sell, categoryAr, selectedSupplierId, imagePath)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("حفظ البيـانات", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun itemSupplierId(sup: SupplierOrAuthor): Int {
    return sup.id
}

@Composable
fun SupplierEditDialog(
    supplier: SupplierOrAuthor?,
    onDismiss: () -> Unit,
    onSave: (Int, String, String?, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var type by remember { mutableStateOf(supplier?.type ?: "مورد") } // مورد / مؤلف
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (supplier == null) "تسجيل شريك جديد (مورد/مؤلف)" else "تعديل بيانات الشريك",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandGreen
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل المعتمد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("هواتف جهة الاتصال والطلبيات") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_phone_input")
                )

                // Select type
                Text("نوع الشريك التجاري:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { type = "مورد" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "مورد") BrandGreen else LightBg,
                            contentColor = if (type == "مورد") Color.White else TextDark
                        )
                    ) {
                        Text("مـورّد بضاعة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { type = "مؤلف" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "مؤلف") BrandGold else LightBg,
                            contentColor = if (type == "مؤلف") Color.White else TextDark
                        )
                    ) {
                        Text("مؤلف/مبدع كتب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("تفاصيل البضائع الموردة وملاحظات إضافية") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_notes_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", color = TextLight)
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            onSave(supplier?.id ?: 0, name, phone.ifBlank { null }, type, notes.ifBlank { null })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("حفظ الشريك", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CashierEditDialog(
    cashier: CashierUser?,
    onDismiss: () -> Unit,
    onSave: (Int, String, String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(cashier?.username ?: "") }
    var fullName by remember { mutableStateOf(cashier?.fullName ?: "") }
    var pinCode by remember { mutableStateOf(cashier?.pinCode ?: "") }
    var role by remember { mutableStateOf(cashier?.role ?: "كاشير") } // كاشير / مدير

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (cashier == null) "فتح حساب موظف كاشير جديد" else "تعديل ملف موظف كاشير",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandGreen
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل للموظف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cashier_fullname_input")
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("معرّف الدخول الفريد (اسم مستخدم إنجليزي)") },
                    singleLine = true,
                    enabled = cashier?.username != "manager",
                    placeholder = { Text("مثال: ahmed_pos") },
                    modifier = Modifier.fillMaxWidth().testTag("cashier_username_input")
                )

                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 6) {
                            pinCode = it
                        }
                    },
                    label = { Text("الرمز السري لتسجيل الدخول (الأرقام فقط)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    placeholder = { Text("مثال: 1234") },
                    modifier = Modifier.fillMaxWidth().testTag("cashier_pin_input")
                )

                // Role selects
                Text("الدور الوظيفي والترخيص:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { role = "كاشير" },
                        modifier = Modifier.weight(1f),
                        enabled = cashier?.username != "manager",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (role == "كاشير") BrandGreen else LightBg,
                            contentColor = if (role == "كاشير") Color.White else TextDark
                        )
                    ) {
                        Text("كـاشير بيـع", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { role = "مدير" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (role == "مدير") BrandGold else LightBg,
                            contentColor = if (role == "مدير") Color.White else TextDark
                        )
                    ) {
                        Text("مـدير فرع/مستودع", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", color = TextLight)
                    }
                    Button(
                        onClick = {
                            if (username.isBlank() || fullName.isBlank() || pinCode.isBlank()) return@Button
                            onSave(cashier?.id ?: 0, username.trim(), fullName.trim(), pinCode.trim(), role)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("حفظ الموظف", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionAddDialog(
    type: String, // إيرادات or مصروفات
    onDismiss: () -> Unit,
    onSave: (String, Double, String?) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val expenseCategories = listOf("رواتب موظفين", "إيجار الفرع", "فواتير كهرباء ومياه", "صيانة ومعدات", "مشتريات طارئة", "تعبئة نقل بضاعة", "أخرى")
    val revenueCategories = listOf("إيرادات الصندوق", "عوائد تالف بضاعة", "تأمين عملاء", "أخرى")

    val categories = if (type == "مصروفات") expenseCategories else revenueCategories

    // Default category initialize if not set
    LaunchedEffect(categories) {
        if (category.isEmpty() && categories.isNotEmpty()) {
            category = categories.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (type == "مصروفات") "قيد مصروف جديد (خصم من الصندوق)" else "تسجيل إيرادات إضافية للصندوق",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (type == "مصروفات") RedAlert else BrandGreen
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("المبلغ المالي (ر.ي)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input")
                )

                // Category list selects
                Text("بند التصنيف المالي الحسابي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("البيان / التوضيح والتفاصيل") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("transaction_desc_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", color = TextLight)
                    }
                    Button(
                        onClick = {
                            val amount = amountInput.toDoubleOrNull() ?: 0.0
                            if (amount <= 0.0 || category.isEmpty()) return@Button
                            onSave(category, amount, description.ifBlank { "معاملة قيد مالي مبررة" })
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "مصروفات") RedAlert else BrandGreen
                        ),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("تثبيت القيد المالي", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    cashiers: List<com.example.data.CashierUser>,
    onLogin: (String, String) -> Unit,
    onCreateAdmin: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var adminFullName by remember { mutableStateOf("") }
    
    val hasUsers = cashiers.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "سوبر ماركت العابسي",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                if (!hasUsers) {
                    Text("لا يوجد مستخدمين. يرجى إنشاء حساب المدير أولاً.", color = OrangeWarning, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    
                    OutlinedTextField(
                        value = adminFullName,
                        onValueChange = { adminFullName = it },
                        label = { Text("الاسم الكامل للمدير") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("الرقم السري") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Button(
                        onClick = {
                            if (adminFullName.isNotBlank() && username.isNotBlank() && pinCode.isNotBlank()) {
                                onCreateAdmin(username, adminFullName, pinCode)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("إنشاء حساب المدير", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("تسجيل الدخول", fontSize = 18.sp, color = TextLight)
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("الرقم السري") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Button(
                        onClick = {
                            if (username.isNotBlank() && pinCode.isNotBlank()) {
                                onLogin(username, pinCode)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("دخول", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
