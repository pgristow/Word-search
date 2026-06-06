package com.wordsearch.data.repository

import com.wordsearch.data.api.EconomyApi
import com.wordsearch.data.model.EconomyPurchaseRequest
import com.wordsearch.data.model.EconomyPurchaseResponse
import com.wordsearch.data.model.StoreResponse
import com.wordsearch.data.model.WalletResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EconomyRepository @Inject constructor(
    private val economyApi: EconomyApi
) {

    suspend fun getStore(): Result<StoreResponse> = withContext(Dispatchers.IO) {
        try {
            val response = economyApi.getStore()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch store data"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching store data")
            Result.failure(e)
        }
    }

    suspend fun purchase(itemType: String, itemId: String): Result<EconomyPurchaseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = economyApi.purchase(EconomyPurchaseRequest(itemType, itemId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Purchase failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing purchase")
            Result.failure(e)
        }
    }

    suspend fun getWallet(): Result<WalletResponse> = withContext(Dispatchers.IO) {
        try {
            val response = economyApi.getWallet()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch wallet"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching wallet")
            Result.failure(e)
        }
    }
}
