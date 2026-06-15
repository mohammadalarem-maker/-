package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class MarketRepository(private val db: AppDatabase) {

    // DAOs
    val productDao = db.productDao()
    val supplierDao = db.supplierDao()
    val cashierDao = db.cashierUserDao()
    val saleDao = db.saleDao()
    val transactionDao = db.transactionDao()

    // Flows
    val products: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val suppliers: Flow<List<SupplierOrAuthor>> = supplierDao.getAllSuppliers()
    val cashiers: Flow<List<CashierUser>> = cashierDao.getAllCashiers()
    val sales: Flow<List<Sale>> = saleDao.getAllSales()
    val transactions: Flow<List<AccountTransaction>> = transactionDao.getAllTransactions()

    // Financial calculations
    val totalSalesFromSales: Flow<Double> = saleDao.getTotalSalesAmountFlow().map { it ?: 0.0 }
    val totalProfitFromSales: Flow<Double> = saleDao.getTotalProfitFlow().map { it ?: 0.0 }
    val totalExpenses: Flow<Double> = transactionDao.getSumAmountByTypeFlow("مصروفات").map { it ?: 0.0 }
    val totalOtherRevenues: Flow<Double> = transactionDao.getSumAmountByTypeFlow("إيرادات").map { it ?: 0.0 }

    // Combined net financial results
    val netBalance: Flow<Double> = combine(
        totalSalesFromSales,
        totalOtherRevenues,
        totalExpenses
    ) { sales, otherRevenues, expenses ->
        (sales + otherRevenues) - expenses
    }

    // Operations
    suspend fun addProduct(product: Product) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    suspend fun addSupplier(supplier: SupplierOrAuthor) = supplierDao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: SupplierOrAuthor) = supplierDao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: SupplierOrAuthor) = supplierDao.deleteSupplier(supplier)

    suspend fun addCashier(cashier: CashierUser) = cashierDao.insertCashier(cashier)
    suspend fun updateCashier(cashier: CashierUser) = cashierDao.updateCashier(cashier)
    suspend fun deleteCashier(cashier: CashierUser) = cashierDao.deleteCashier(cashier)

    suspend fun addTransaction(transaction: AccountTransaction) = transactionDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: AccountTransaction) = transactionDao.deleteTransaction(transaction)

    // Complete POS Sale Flow
    suspend fun checkout(
        cashierName: String,
        cartItems: List<Pair<Product, Int>>,
        paymentMethod: String
    ): Boolean {
        if (cartItems.isEmpty()) return false

        var totalAmount = 0.0
        var totalCost = 0.0
        val serializedList = mutableListOf<String>()

        // Update quantities and calculate values
        for ((product, qty) in cartItems) {
            val dbProduct = productDao.getProductById(product.id) ?: continue
            val newQty = (dbProduct.quantity - qty).coerceAtLeast(0)
            productDao.updateProduct(dbProduct.copy(quantity = newQty))

            totalAmount += product.sellingPrice * qty
            totalCost += product.costPrice * qty
            
            // Format: "Name x Qty @ Price"
            serializedList.add("${product.nameAr} × $qty @ ${product.sellingPrice}")
        }

        val totalProfit = totalAmount - totalCost
        val jsonSummary = serializedList.joinToString("\n")

        val sale = Sale(
            cashierName = cashierName,
            itemsJson = jsonSummary,
            totalAmount = totalAmount,
            totalCost = totalCost,
            totalProfit = totalProfit,
            paymentMethod = paymentMethod
        )
        saleDao.insertSale(sale)

        // Also record this as a Sales Transaction automatically in accounts
        transactionDao.insertTransaction(
            AccountTransaction(
                type = "إيرادات",
                category = "مبيعات",
                amount = totalAmount,
                description = "مبيعات كاشير: $cashierName (رقم الفاتورة تلقائي)"
            )
        )
        return true
    }
}
