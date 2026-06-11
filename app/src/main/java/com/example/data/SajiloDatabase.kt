package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. User Profile Entity
@Entity(tableName = "user_profile")
data class DbUserProfile(
    @PrimaryKey val id: String = "rider_01",
    val name: String,
    val phoneNumber: String,
    val email: String,
    val balanceEseva: Double,
    val balanceKhalti: Double,
    val balanceImePay: Double,
    val raterCount: Int,
    val rating: Double,
    val selectedLanguage: String, // "en" or "ne"
    val isDataSavingsMode: Boolean = false,
    val verifiedRiderStatus: String = "VERIFIED" // "VERIFIED", "PENDING", "UNVERIFIED"
)

// 2. Scheduled or Active Ride Records
@Entity(tableName = "rides")
data class DbRide(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pickup: String,
    val destination: String,
    val fareNpr: Double,
    val distanceKm: Double,
    val durationMinutes: Int,
    val category: String, // "BIKE", "CAR", "SUV", "SHARED"
    val paymentMethod: String, // "CASH", "ESEWA", "KHALTI", "IMEPAY"
    val isHillyTerrain: Boolean,
    val hasFuelSurge: Boolean,
    val driverName: String,
    val driverPhone: String,
    val driverRating: Double,
    val driverVehiclePlate: String,
    val status: String, // "REQUESTED", "ACCEPTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    val timestamp: Long = System.currentTimeMillis(),
    val isPreScheduled: Boolean = false,
    val scheduledTime: String = "" // e.g. "06:30 AM"
)

// 3. Simulated Active Chat Messaging
@Entity(tableName = "chat_messages")
data class DbChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: Long,
    val sender: String, // "RIDER", "DRIVER"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 4. Customer Support Ticket
@Entity(tableName = "support_tickets")
data class DbSupportTicket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val message: String,
    val status: String = "OPEN", // "OPEN", "RESOLVED"
    val timestamp: Long = System.currentTimeMillis()
)

// 5. Driver Verification / Onboarding Documents
@Entity(tableName = "driver_onboarding")
data class DbDriverOnboarding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val licenseNumber: String,
    val citizenshipId: String,
    val vehicleModel: String,
    val vehiclePlate: String,
    val documentStatus: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis()
)

// DAOs
@Dao
interface SajiloDao {
    // User Profile
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<DbUserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: DbUserProfile)

    // Rides
    @Query("SELECT * FROM rides ORDER BY timestamp DESC")
    fun getAllRides(): Flow<List<DbRide>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: DbRide): Long

    @Update
    suspend fun updateRide(ride: DbRide)

    @Query("UPDATE rides SET status = :status WHERE id = :rideId")
    suspend fun updateRideStatus(rideId: Long, status: String)

    // Chats
    @Query("SELECT * FROM chat_messages WHERE rideId = :rideId ORDER BY timestamp ASC")
    fun getChatMessagesForRide(rideId: Long): Flow<List<DbChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(msg: DbChatMessage)

    // Support
    @Query("SELECT * FROM support_tickets ORDER BY timestamp DESC")
    fun getAllTickets(): Flow<List<DbSupportTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: DbSupportTicket)

    // Driver Onboarding
    @Query("SELECT * FROM driver_onboarding ORDER BY timestamp DESC")
    fun getDriversOnboarding(): Flow<List<DbDriverOnboarding>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriverOnboarding(driver: DbDriverOnboarding)

    @Query("UPDATE driver_onboarding SET documentStatus = :status WHERE id = :id")
    suspend fun updateDriverStatus(id: Long, status: String)
}

// Database Setup
@Database(
    entities = [
        DbUserProfile::class,
        DbRide::class,
        DbChatMessage::class,
        DbSupportTicket::class,
        DbDriverOnboarding::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SajiloDatabase : RoomDatabase() {
    abstract fun dao(): SajiloDao

    companion object {
        @Volatile
        private var INSTANCE: SajiloDatabase? = null

        fun getDatabase(context: Context): SajiloDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SajiloDatabase::class.java,
                    "sajilo_ride_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
