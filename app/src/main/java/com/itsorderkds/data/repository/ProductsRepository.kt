package com.itsorderkds.data.repository

import com.itsorderkds.data.model.Addon
import com.itsorderkds.data.model.Product
import com.itsorderkds.data.model.StockStatusEnum
import com.itsorderkds.data.model.UpdateAddonStatusRequest
import com.itsorderkds.data.model.UpdateProductRequest
import com.itsorderkds.data.model.UpdateProductStatusRequest
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.network.ProductApi
import com.itsorderkds.ui.theme.GlobalMessageManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium do zarządzania produktami.
 * Zależność 'ProductsApi' jest teraz wstrzykiwana przez Hilt, a nie tworzona ręcznie.
 * Adnotacja @Singleton zapewnia, że w całej aplikacji będzie tylko jedna instancja tego repozytorium.
 */
@Singleton // Repozytoria zazwyczaj powinny być Singletonami
class ProductsRepository @Inject constructor(
    private val api: ProductApi,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    // ───── API CALLS ───────────────────────────────────────────────────────
    suspend fun updateStockStatus(
        productId: String,
        newStatus: StockStatusEnum
    ): Resource<Product> = safeApiCall {
        val isInStock = newStatus == StockStatusEnum.IN_STOCK
        val req = UpdateProductStatusRequest( // było: UpdateProdctStatusRequest
            stock_status = newStatus,         // jeśli backend oczekuje "stock_status"
            status = isInStock                // boolean wynikający z enumu
        )
        api.updateStockStatus(productId, req)
    }


    suspend fun getProducts(
        limit: Int?,
        search: String?,
        status: Boolean?
    ): Resource<List<Product>> =
        safeApiCall { api.getProducts(limit, search, status) }
            .mapSuccess { it.data ?: emptyList() }

    suspend fun getProductsByCategories(): Resource<List<com.itsorderkds.data.model.CategoryProduct>> =
        safeApiCall { api.getProductsByCategories() }
            .mapSuccess { it.data ?: emptyList() }

    suspend fun getProductDetails(productId: String): Resource<Product> =
        safeApiCall { api.getProductDetails(productId) }

    suspend fun updateProduct(
        productId: String,
        productData: UpdateProductRequest
    ): Resource<Product> =
        safeApiCall { api.updateProduct(productId, productData) }

    //    suspend fun updateAddonStatus(addonId: String, status: Boolean) {
//        safeApiCall { api.updateAddonStatus(addonId, UpdateAddonStatusRequest(status)) }
//    }
    suspend fun updateAddonStatus(
        addonId: String,
        newStatus: StockStatusEnum
    ): Resource<Addon> = safeApiCall {
        val isInStock = newStatus == StockStatusEnum.IN_STOCK
        val req = UpdateAddonStatusRequest( // było: UpdateProdctStatusRequest
            stock_status = newStatus,         // jeśli backend oczekuje "stock_status"
            status = isInStock                // boolean wynikający z enumu
        )
        api.updateAddonStatus(addonId, req)
    }
}

