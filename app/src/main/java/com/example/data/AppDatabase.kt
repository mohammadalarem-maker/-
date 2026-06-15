package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        SupplierOrAuthor::class,
        CashierUser::class,
        Sale::class,
        AccountTransaction::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun cashierUserDao(): CashierUserDao
    abstract fun saleDao(): SaleDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "al_abisi_supermarket_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default manager and cashiers so app is ready to use
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            
                            // Insert default users
                            database.cashierUserDao().insertCashier(
                                CashierUser(
                                    username = "manager",
                                    fullName = "المدير العام (العابسي)",
                                    pinCode = "1234",
                                    role = "مدير"
                                )
                            )
                            database.cashierUserDao().insertCashier(
                                CashierUser(
                                    username = "cashier1",
                                    fullName = "كاشير 1 - أحمد",
                                    pinCode = "1122",
                                    role = "كاشير"
                                )
                            )
                            database.cashierUserDao().insertCashier(
                                CashierUser(
                                    username = "cashier2",
                                    fullName = "كاشير 2 - محمد",
                                    pinCode = "3344",
                                    role = "كاشير"
                                )
                            )

                            // Prepopulate some default products
                            val defaultProducts = listOf(
                                Product(nameAr = "أرز بسمتي 5 كجم", barcode = "62810011", quantity = 25, costPrice = 35.0, sellingPrice = 45.0, categoryAr = "أغذية أساسية"),
                                Product(nameAr = "حليب سائل كامل الدسم", barcode = "62820012", quantity = 100, costPrice = 5.0, sellingPrice = 7.0, categoryAr = "ألبان"),
                                Product(nameAr = "زيت طبخ 1.5 لتر", barcode = "62830013", quantity = 40, costPrice = 12.0, sellingPrice = 16.0, categoryAr = "أغذية أساسية"),
                                Product(nameAr = "سكر ناعم 1 كجم", barcode = "62840014", quantity = 4, costPrice = 4.0, sellingPrice = 5.5, categoryAr = "أغذية أساسية"), // Low stock
                                Product(nameAr = "صابون غسيل الصحون", barcode = "62850015", quantity = 30, costPrice = 8.0, sellingPrice = 11.0, categoryAr = "منظفات"),
                                Product(nameAr = "شاي أحمر خشن 100 كيس", barcode = "62860016", quantity = 2, costPrice = 9.0, sellingPrice = 12.0, categoryAr = "مشروبات") // Low stock
                            )
                            for (product in defaultProducts) {
                                database.productDao().insertProduct(product)
                            }

                            // Prepopulate some suppliers
                            database.supplierDao().insertSupplier(
                                SupplierOrAuthor(
                                    name = "مجموعة هائل سعيد أنعم",
                                    phone = "777112233",
                                    type = "مورد",
                                    notes = "مورد المواد الأساسية والأغذية"
                                )
                            )
                            database.supplierDao().insertSupplier(
                                SupplierOrAuthor(
                                    name = "دار المعرفة للنشر والتأليف",
                                    phone = "771112222",
                                    type = "مؤلف",
                                    notes = "مؤلف ومورد الكتب والدفاتر التعليمية للسوبرماركت"
                                )
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
