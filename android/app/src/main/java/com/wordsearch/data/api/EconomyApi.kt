package com.wordsearch.data.api

import com.wordsearch.data.model.EconomyPurchaseRequest
import com.wordsearch.data.model.EconomyPurchaseResponse
import com.wordsearch.data.model.StoreResponse
import com.wordsearch.data.model.WalletResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EconomyApi {

    @GET("api/economy/store")
    suspend fun getStore(): Response<StoreResponse>

    @POST("api/economy/purchase")
    suspend fun purchase(@Body req: EconomyPurchaseRequest): Response<EconomyPurchaseResponse>

    @GET("api/economy/wallet")
    suspend fun getWallet(): Response<WalletResponse>
}
