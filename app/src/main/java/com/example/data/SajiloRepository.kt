package com.example.data

import kotlinx.coroutines.flow.Flow

class SajiloRepository(private val dao: SajiloDao) {

    val userProfile: Flow<DbUserProfile?> = dao.getUserProfile()
    val allRides: Flow<List<DbRide>> = dao.getAllRides()
    val allTickets: Flow<List<DbSupportTicket>> = dao.getAllTickets()
    val driverOnboardings: Flow<List<DbDriverOnboarding>> = dao.getDriversOnboarding()

    suspend fun saveProfile(profile: DbUserProfile) {
        dao.insertOrUpdateProfile(profile)
    }

    suspend fun insertRide(ride: DbRide): Long {
        return dao.insertRide(ride)
    }

    suspend fun updateRide(ride: DbRide) {
        dao.updateRide(ride)
    }

    suspend fun updateRideStatus(rideId: Long, status: String) {
        dao.updateRideStatus(rideId, status)
    }

    fun getChatsForRide(rideId: Long): Flow<List<DbChatMessage>> {
        return dao.getChatMessagesForRide(rideId)
    }

    suspend fun sendChatMessage(msg: DbChatMessage) {
        dao.insertChatMessage(msg)
    }

    suspend fun createTicket(ticket: DbSupportTicket) {
        dao.insertTicket(ticket)
    }

    suspend fun submitDriverOnboarding(onboarding: DbDriverOnboarding) {
        dao.insertDriverOnboarding(onboarding)
    }

    suspend fun updateDriverOnboardingStatus(id: Long, status: String) {
        dao.updateDriverStatus(id, status)
    }
}
