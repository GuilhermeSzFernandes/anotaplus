package com.guilherme.anotaplus.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.guilherme.anotaplus.BuildConfig
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.BillingSyncRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Fina camada em cima do Play Billing Library pra assinatura PRO. Cada tela
 * que precisa mexer com assinatura cria a própria instância (não é
 * singleton — só PlansActivity efetivamente compra; Settings/Login só
 * reconsultam) e chama encerrar() no onDestroy.
 *
 * IMPORTANTE: só funciona de verdade com o app instalado a partir da Play
 * Store (mesmo que em teste interno/fechado) — instalado via APK avulso
 * (como o app-debug.apk do GitHub Actions), o BillingClient conecta mas
 * não encontra produto nenhum, porque a Play Store não sabe que esse
 * "aplicativo" tem uma assinatura associada. Também precisa que
 * "anotaplus_pro_mensal" (SUBSCRIPTION_PRODUCT_ID) exista de verdade no
 * Play Console com esse id exato.
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
        .build()

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        // Callback do próprio launchBillingFlow — o resultado "de verdade"
        // (sucesso/cancelamento) chega aqui, de forma assíncrona.
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        CoroutineScope(Dispatchers.IO).launch {
            purchases.forEach { processarCompra(it) }
        }
    }

    private suspend fun conectar() {
        if (billingClient.isReady) return
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    // Sem retry automático aqui de propósito: cada chamada
                    // pública desta classe já chama conectar() antes de
                    // usar o client, então a próxima operação reconecta.
                }
            })
        }
    }

    /** Detalhes (preço, offer token) do produto de assinatura PRO, direto da Play Store. Null se ainda não existir no Play Console ou o app não estiver instalado via Play Store. */
    suspend fun consultarProductDetails(): ProductDetails? {
        conectar()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()
        return runCatching { billingClient.queryProductDetails(params).productDetailsList?.firstOrNull() }
            .getOrNull()
    }

    /** Abre o fluxo de compra da Play Store. Retorna false se não deu nem pra iniciar (ex: sem offer configurada). */
    fun iniciarCompra(activity: Activity, productDetails: ProductDetails): Boolean {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        return billingClient.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Reconsulta o Play Billing (fonte da verdade) e sincroniza o
     * resultado com o backend e com o cache local (SubscriptionPrefs).
     * Chamar no login, ao abrir telas que dependem do status (Planos,
     * Configurações) e no onResume de PlansActivity (volta do fluxo de
     * compra).
     */
    suspend fun atualizarStatusAssinatura() {
        conectar()
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        val ativa = runCatching { billingClient.queryPurchasesAsync(params).purchasesList }
            .getOrNull()
            ?.firstOrNull {
                it.products.contains(BuildConfig.SUBSCRIPTION_PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }

        if (ativa != null) {
            processarCompra(ativa)
        } else {
            SubscriptionPrefs.setPro(context, false)
            sincronizarBackend(BuildConfig.SUBSCRIPTION_PRODUCT_ID, "", active = false)
        }
    }

    private suspend fun processarCompra(purchase: Purchase) {
        if (!purchase.products.contains(BuildConfig.SUBSCRIPTION_PRODUCT_ID)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        // Toda compra precisa ser confirmada (acknowledge) em até 3 dias,
        // senão o Google reembolsa automaticamente.
        if (!purchase.isAcknowledged) {
            runCatching {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                )
            }
        }

        SubscriptionPrefs.setPro(context, true)
        sincronizarBackend(BuildConfig.SUBSCRIPTION_PRODUCT_ID, purchase.purchaseToken, active = true)
    }

    private suspend fun sincronizarBackend(productId: String, purchaseToken: String, active: Boolean) {
        if (!SessionPrefs.estaLogado(context)) return
        runCatching {
            ApiClient.api.sincronizarAssinatura(
                "Bearer ${SessionPrefs.getAccessToken(context)}",
                BillingSyncRequest(productId, purchaseToken, active)
            )
        }
    }

    fun encerrar() {
        billingClient.endConnection()
    }
}
