package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// 1. Product Entity
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val arabicName: String,
    val category: String,
    val price: Double,
    val oldPrice: Double? = null,
    val imageUrl: String,
    val brand: String,
    val model: String,
    val year: Int,
    val description: String,
    val stock: Int,
    val isB2B: Boolean = false,
    val isOffer: Boolean = false
)

// 2. Cart Item Entity
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Int,
    val quantity: Int
)

// 3. Bike Profile Entity (Garage)
@Entity(tableName = "bike_profiles")
data class BikeProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val model: String,
    val year: Int,
    val odometer: Int,
    val lastServiceDate: String,
    val notes: String = ""
)

// 4. Maintenance Event Entity
@Entity(tableName = "maintenance_events")
data class MaintenanceEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikeId: Int,
    val title: String,
    val dueDate: String,
    val isCompleted: Boolean = false
)

// 5. Community Post Entity
@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val author: String,
    val avatar: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)

// 6. Order Entity
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderDate: String,
    val itemsCount: Int,
    val totalPrice: Double,
    val status: String
)

// DAOs
@Dao
interface TroosDao {
    // Products
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE brand = :brand AND model = :model AND year = :year")
    fun getCompatibleProducts(brand: String, model: String, year: Int): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    // Cart
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Bike Profiles
    @Query("SELECT * FROM bike_profiles ORDER BY id DESC")
    fun getMyBikes(): Flow<List<BikeProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBike(bike: BikeProfileEntity): Long

    @Query("DELETE FROM bike_profiles WHERE id = :id")
    suspend fun deleteBike(id: Int)

    // Maintenance Events
    @Query("SELECT * FROM maintenance_events WHERE bikeId = :bikeId ORDER BY id DESC")
    fun getMaintenanceEvents(bikeId: Int): Flow<List<MaintenanceEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceEvent(event: MaintenanceEventEntity)

    // Community Posts
    @Query("SELECT * FROM community_posts ORDER BY timestamp DESC")
    fun getCommunityPosts(): Flow<List<CommunityPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CommunityPostEntity)

    // Orders
    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)
}

@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        BikeProfileEntity::class,
        MaintenanceEventEntity::class,
        CommunityPostEntity::class,
        OrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TroosDatabase : RoomDatabase() {
    abstract fun dao(): TroosDao

    companion object {
        @Volatile
        private var INSTANCE: TroosDatabase? = null

        fun getDatabase(context: Context): TroosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TroosDatabase::class.java,
                    "troos_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
