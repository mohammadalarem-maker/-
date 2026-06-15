package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY nameAr ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE quantity <= 5 ORDER BY quantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierOrAuthor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierOrAuthor)

    @Update
    suspend fun updateSupplier(supplier: SupplierOrAuthor)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierOrAuthor)
}

@Dao
interface CashierUserDao {
    @Query("SELECT * FROM cashier_users ORDER BY username ASC")
    fun getAllCashiers(): Flow<List<CashierUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashier(cashier: CashierUser)

    @Update
    suspend fun updateCashier(cashier: CashierUser)

    @Delete
    suspend fun deleteCashier(cashier: CashierUser)

    @Query("SELECT * FROM cashier_users WHERE username = :username AND pinCode = :pin LIMIT 1")
    suspend fun verifyCashier(username: String, pin: String): CashierUser?
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Query("SELECT SUM(totalAmount) FROM sales")
    fun getTotalSalesAmountFlow(): Flow<Double?>

    @Query("SELECT SUM(totalProfit) FROM sales")
    fun getTotalProfitFlow(): Flow<Double?>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<AccountTransaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getSumAmountByTypeFlow(type: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AccountTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: AccountTransaction)
}
