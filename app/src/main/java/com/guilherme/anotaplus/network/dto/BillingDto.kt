package com.guilherme.anotaplus.network.dto

data class BillingSyncRequest(
    val productId: String,
    val purchaseToken: String,
    val active: Boolean
)

data class BillingSyncResponse(val proAtivo: Boolean)
