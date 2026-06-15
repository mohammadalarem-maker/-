package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MarketRepository

    // POS Cart
    val cart = mutableStateListOf<Pair<Product, Int>>()

    // Active Cashier
    private val _currentCashier = MutableStateFlow<CashierUser?>(null)
    val currentCashier: StateFlow<CashierUser?> = _currentCashier.asStateFlow()

    // Search and Filters
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MarketRepository(database)
        
        // Auto-select the first prepopulated cashier for demo convenience if none is active
        viewModelScope.launch {
            repository.cashiers.firstOrNull()?.firstOrNull()?.let {
                _currentCashier.value = it
            }
        }
    }

    // Expose flows from repository
    val products: StateFlow<List<Product>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierOrAuthor>> = repository.suppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashiers: StateFlow<List<CashierUser>> = repository.cashiers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = repository.sales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<AccountTransaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial calculations
    val totalSalesFromSales: StateFlow<Double> = repository.totalSalesFromSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalProfitFromSales: StateFlow<Double> = repository.totalProfitFromSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpenses: StateFlow<Double> = repository.totalExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalOtherRevenues: StateFlow<Double> = repository.totalOtherRevenues
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalance: StateFlow<Double> = repository.netBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Filtered Products for POS Cashier Screen
    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        _productSearchQuery,
        _selectedCategory
    ) { prodList, query, cat ->
        prodList.filter { prod ->
            val matchesQuery = (prod.nameAr.contains(query, ignoreCase = true) || 
                                prod.barcode?.contains(query) == true || 
                                prod.categoryAr.contains(query, ignoreCase = true))
            val matchesCategory = (cat == "الكل" || prod.categoryAr == cat)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Management
    fun addToCart(product: Product) {
        val existingIndex = cart.indexOfFirst { it.first.id == product.id }
        if (existingIndex != -1) {
            val currentQty = cart[existingIndex].second
            if (currentQty < product.quantity) {
                cart[existingIndex] = Pair(product, currentQty + 1)
            }
        } else {
            if (product.quantity > 0) {
                cart.add(Pair(product, 1))
            }
        }
    }

    fun updateCartQuantity(product: Product, newQty: Int) {
        val existingIndex = cart.indexOfFirst { it.first.id == product.id }
        if (existingIndex != -1) {
            if (newQty <= 0) {
                cart.removeAt(existingIndex)
            } else if (newQty <= product.quantity) {
                cart[existingIndex] = Pair(product, newQty)
            }
        }
    }

    fun removeFromCart(product: Product) {
        val existingIndex = cart.indexOfFirst { it.first.id == product.id }
        if (existingIndex != -1) {
            cart.removeAt(existingIndex)
        }
    }

    fun clearCart() {
        cart.clear()
    }

    // POS Checkout
    fun checkout(paymentMethod: String, onComplete: (Boolean, String) -> Unit) {
        val cashier = _currentCashier.value
        if (cashier == null) {
            onComplete(false, "فضلاً اختر الكاشير الحالي أولاً")
            return
        }
        if (cart.isEmpty()) {
            onComplete(false, "السلة فارغة")
            return
        }

        viewModelScope.launch {
            val success = repository.checkout(cashier.fullName, cart.toList(), paymentMethod)
            if (success) {
                cart.clear()
                onComplete(true, "تمت عملية البيع وحفظ الفاتورة بنجاح")
            } else {
                onComplete(false, "حدث خطأ أثناء إتمام عملية البيع")
            }
        }
    }

    // Search query updates
    fun updateSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // Login / Cashier unlock
    fun switchCashierWithPin(username: String, pin: String, onResult: (Boolean) -> Unit) {
        val cleanUser = username.trim()
        val cleanPin = pin.trim()
        val email = if (cleanUser.contains("@")) cleanUser else "$cleanUser@alibisi.com"

        viewModelScope.launch {
            try {
                // Try Firebase Auth first
                com.google.firebase.auth.FirebaseAuth.getInstance().signInWithEmailAndPassword(email, cleanPin)
                    .addOnCompleteListener { task ->
                        viewModelScope.launch {
                            if (task.isSuccessful) {
                                // Once Firebase auth succeeds, fallback to locate the role locally
                                val cashier = repository.cashierDao.verifyCashier(cleanUser, cleanPin) 
                                    ?: repository.cashiers.firstOrNull()?.find { it.username == cleanUser }
                                    ?: CashierUser(0, cleanUser, cleanUser, cleanPin, "كاشير") // Default to cashier if missing locally
                                _currentCashier.value = cashier
                                onResult(true)
                            } else {
                                // If Firebase fails, try local fallback
                                val cashier = repository.cashierDao.verifyCashier(cleanUser, cleanPin)
                                if (cashier != null) {
                                    _currentCashier.value = cashier
                                    onResult(true)
                                } else {
                                    onResult(false)
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                // If Firebase is not initialized, fallback to purely local DB
                val cashier = repository.cashierDao.verifyCashier(cleanUser, cleanPin)
                if (cashier != null) {
                    _currentCashier.value = cashier
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
        }
    }

    // Add Direct Cashier Session (Switching cashier directly without pin for easy access as well)
    fun selectCashierDirectly(cashier: CashierUser) {
        _currentCashier.value = cashier
    }

    // Product Functions
    fun saveProduct(
        id: Int,
        nameAr: String,
        barcode: String?,
        quantity: Int,
        costPrice: Double,
        sellingPrice: Double,
        categoryAr: String,
        supplierId: Int?,
        imagePath: String? = null
    ) {
        viewModelScope.launch {
            val product = Product(
                id = if (id == 0) 0 else id,
                nameAr = nameAr,
                barcode = barcode,
                quantity = quantity,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                categoryAr = categoryAr,
                supplierId = supplierId,
                imagePath = imagePath
            )
            if (id == 0) {
                repository.addProduct(product)
            } else {
                repository.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Suppliers Functions
    fun saveSupplier(
        id: Int,
        name: String,
        phone: String?,
        type: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val supplier = SupplierOrAuthor(
                id = if (id == 0) 0 else id,
                name = name,
                phone = phone,
                type = type,
                notes = notes
            )
            if (id == 0) {
                repository.addSupplier(supplier)
            } else {
                repository.updateSupplier(supplier)
            }
        }
    }

    fun deleteSupplier(supplier: SupplierOrAuthor) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }

    // Cashiers Management Functions
    fun saveCashier(
        id: Int,
        username: String,
        fullName: String,
        pinCode: String,
        role: String
    ) {
        viewModelScope.launch {
            val email = if (username.contains("@")) username else "$username@alibisi.com"
            try {
                // Try creating Firebase user
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, pinCode)
            } catch (e: Exception) {
                // Ignore if already exists or Firebase is not initialized
            }

            val cashier = CashierUser(
                id = if (id == 0) 0 else id,
                username = username,
                fullName = fullName,
                pinCode = pinCode,
                role = role
            )
            if (id == 0) {
                repository.addCashier(cashier)
            } else {
                repository.updateCashier(cashier)
            }
        }
    }

    fun deleteCashier(cashier: CashierUser) {
        viewModelScope.launch {
            repository.deleteCashier(cashier)
        }
    }

    // Accounting Transaction Operations
    fun saveTransaction(
        type: String,
        category: String,
        amount: Double,
        description: String?
    ) {
        viewModelScope.launch {
            val transaction = AccountTransaction(
                type = type,
                category = category,
                amount = amount,
                description = description
            )
            repository.addTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: AccountTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
