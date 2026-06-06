package com.wordsearch.data.model

import com.google.gson.annotations.SerializedName

data class StoreThemeDto(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("coinCost")
    val coinCost: Int = 0,
    @SerializedName("owned")
    val owned: Boolean = false
)

data class StoreCategoryDto(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("coinUnlockCost")
    val coinUnlockCost: Int = 0,
    @SerializedName("unlocked")
    val unlocked: Boolean = false
)

data class StoreResponse(
    @SerializedName("coinBalance")
    val coinBalance: Long = 0L,
    @SerializedName("hintCost")
    val hintCost: Int = 0,
    @SerializedName("themes")
    val themes: List<StoreThemeDto> = emptyList(),
    @SerializedName("lockableCategories")
    val lockableCategories: List<StoreCategoryDto> = emptyList()
)

data class EconomyPurchaseRequest(
    @SerializedName("itemType")
    val itemType: String,
    @SerializedName("itemId")
    val itemId: String
)

data class EconomyPurchaseResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("coinBalance")
    val coinBalance: Long = 0L,
    @SerializedName("message")
    val message: String = ""
)

data class WalletResponse(
    @SerializedName("coinBalance")
    val coinBalance: Long = 0L
)

data class HintResponse(
    @SerializedName("word")
    val word: String = "",
    @SerializedName("cells")
    val cells: List<CellDto> = emptyList(),
    @SerializedName("coinsSpent")
    val coinsSpent: Long = 0L,
    @SerializedName("coinBalance")
    val coinBalance: Long = 0L
)
