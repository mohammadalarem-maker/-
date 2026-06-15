package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String,
    val barcode: String?,
    val quantity: Int,
    val costPrice: Double, // سعر الشراء
    val sellingPrice: Double, // سعر البيع
    val categoryAr: String,
    val supplierId: Int? = null,
    val imagePath: String? = null
)

@Entity(tableName = "suppliers")
data class SupplierOrAuthor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String?,
    val type: String, // "مورد" (Supplier) or "مؤلف" (Author)
    val notes: String?
)

@Entity(tableName = "cashier_users")
data class CashierUser(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val fullName: String,
    val pinCode: String,
    val role: String // "كاشير" (Cashier) or "مدير" (Manager)
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierName: String,
    val itemsJson: String, // قائمة المواد المباعة بصيغة JSON
    val totalAmount: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val paymentMethod: String // "نقداً" or "شبكة"
)

@Entity(tableName = "transactions")
data class AccountTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "إيرادات" or "مصروفات"
    val category: String, // "مبيعات" / "إيجار" / "رواتب" / "مشتريات" / "صيانة" / "أخرى"
    val amount: Double,
    val description: String?
)

// Data class representation for parsing itemsJson
data class SoldItem(
    val productId: Int,
    val nameAr: String,
    val quantity: Int,
    val costPrice: Double,
    val sellingPrice: Double
)
