package com.itsorderkds.data.model

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.itsorderkds.data.util.FlexibleDoubleAdapter
import com.itsorderkds.ui.product.detail.ProductDetailUiState

data class Product(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,                   // String lub mapa tłumaczeń
    @SerializedName("short_description") val shortDescription: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("type") val type: ProductTypeEnum?,
    @SerializedName("unit") val unit: String?,
    @SerializedName("quantity") val quantity: Int?,
    @SerializedName("price") val price: Double?,
    @SerializedName("sale_price") val salePrice: Double?,
    @field:com.google.gson.annotations.JsonAdapter(FlexibleDoubleAdapter::class)
    @SerializedName("discount") val discount: Double?,
    @SerializedName("is_featured") val isFeatured: Boolean?,
    @SerializedName("product_type") val productType: ProductProductTypeEnum?,
    @SerializedName("is_sale_enable") val isSaleEnable: Boolean,
    @SerializedName("sale_starts_at") val saleStartsAt: String?,
    @SerializedName("sale_expired_at") val saleExpiredAt: String?,
    @SerializedName("is_wishlist") val isWishlist: Boolean?,
    @SerializedName("sku") val sku: String?,
    @SerializedName("stock_status") val stockStatus: StockStatusEnum,
    @SerializedName("slug") val slug: String?,
    @SerializedName("tax_id") val taxId: String?,
    @SerializedName("product_thumbnail_id") val productThumbnailId: String?,
    @SerializedName("tax") val tax: TaxAdminDto?,
    @SerializedName("product_thumbnail") val productThumbnail: AttachmentAdminDto?,
    @SerializedName("product_galleries") val productGalleries: List<AttachmentAdminDto> = emptyList(),
    @field:JsonAdapter(FlexibleBooleanAdapter::class)
    @SerializedName("status") val status: Boolean,
    @SerializedName("allergens") val allergens: List<AllergenAdminDto> = emptyList(),
    @SerializedName("categories") val categories: List<CategoryAdminDto> = emptyList(),
    @SerializedName("addons_group") val addonsGroup: List<AddonGroupAdminDto> = emptyList(),
    @SerializedName("availability") val availability: Availability? = null,
    @SerializedName("related_products") val relatedProducts: List<String>? = emptyList(),
    @SerializedName("cross_sell_products") val crossSellProducts: List<String>? = emptyList(),
    @SerializedName("attributes_ids") val attributesIds: List<String>? = emptyList(),
    @SerializedName("digital_file_ids") val digitalFileIds: List<String>? = emptyList(),
)

enum class ProductTypeEnum {
    SIMPLE,
    VARIABLE
}
enum class ProductProductTypeEnum {
    PHYSICAL,
    DIGITAL,
    EXTERNAL
}
enum class ProductFilterStatus {
    ALL,
    ACTIVE,
    INACTIVE
}
data class ProductsResponse(
    val data: List<Product>
)

data class ProductsCategoriesResponse(
    val data: List<CategoryProduct>
)
// ─────────────────────────── POMOCNICZE DTO ────────────────────────────────────

/** Informacja o stawce podatkowej */
data class TaxAdminDto(
    val id: String,
    val name: String,
    val rate: Double
)

/** Załączniki (miniatury, galerie) */
data class AttachmentAdminDto(
    val id: String,
    val name: String?,
    @SerializedName("file_name") val fileName: String?,
    val size: Int?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("original_url") val originalUrl: String
)

/** Alergen występujący w produkcie */
data class AllergenAdminDto(
    val id: String,
    val name: String
)

/** Pojedynczy addon (dodatek) */
data class AddonAdminDto(
    val id: String,
    val name: String,
    val price: Double?,
    val status: Boolean,
    @SerializedName("stock_status") val stockStatus: StockStatusEnum,
    val sku: String?,
    @SerializedName("pos_id") val posId: String?
)

/** Grupa addonów */
data class AddonGroupAdminDto(
    val id: String,
    val name: String,
    @SerializedName("addons") val addons: List<AddonAdminDto> = emptyList()
)
data class UpdateStockStatusRequest(
    val status: StockStatusEnum
)
/** Kategoria produktu */
data class CategoryAdminDto(
    val id: String?,  // nullable - API może zwracać null
    val name: String?,  // nullable - API może zwracać null
    val slug: String?,  // nullable - API może zwracać null
    val status: Boolean?,  // nullable - API może zwracać null
    val description: String?,  // nullable - API może zwracać null
    val products: List<Product>? = null,
    @SerializedName("category_icon") val categoryIcon: AttachmentAdminDto? = null
)

// Alias dla wygody użycia w UI
typealias CategoryProduct = CategoryAdminDto
/* ───────── Time slot ───────── */
data class TimeRange(
    val start: String,   // w formacie „HH:mm”
    val end:   String    // w formacie „HH:mm”
)

/* ───────── Wyjątki (święta, urlopy) ───────── */
data class AvailabilityException(
    val date : String,   // „RRRR-MM-DD”
    val start: String,
    val end  : String
)

/* ───────── Dostępność tygodniowa ───────── */
data class WeeklyAvailability(
    val mon: List<TimeRange> = emptyList(),
    val tue: List<TimeRange> = emptyList(),
    val wed: List<TimeRange> = emptyList(),
    val thu: List<TimeRange> = emptyList(),
    val fri: List<TimeRange> = emptyList(),
    val sat: List<TimeRange> = emptyList(),
    val sun: List<TimeRange> = emptyList()
)

/* ───────── Główne DTO ───────── */
data class Availability(
    val weekly    : WeeklyAvailability = WeeklyAvailability(),
    val exceptions: List<AvailabilityException> = emptyList()
)

enum class StockStatusEnum {
    @SerializedName(value = "IN_STOCK", alternate = ["in_stock"])
    IN_STOCK,

    @SerializedName(value = "OUT_OF_STOCK", alternate = ["out_of_stock"])
    OUT_OF_STOCK
}
data class UpdateAddonStatusRequest(
    val status: Boolean,
    val stock_status: StockStatusEnum
)
data class UpdateProductRequest(
    val name: String,
    val short_description: String,
    val description: String?,
    val sku: String,
    val unit: String,
    val price: Double,
    val sale_price: Double?,
    val discount: Double,
    val product_type: String,
    val type: String,
    val quantity: Int,
    val is_sale_enable: Int,
    val stock_status: String,
    val status: Boolean,
    val categories: List<String>,
    val attributes_ids: List<String>? = null,   // ★ było wymagane – teraz opcjonalne
    val availability: Availability,
    val digital_file_ids: List<String>? = null,
    val tax_id: String? = null,
    val product_thumbnail_id: String? = null,
    val related_products: List<String>? = null,
    val cross_sell_products: List<String>? = null
)
data class UpdateProductStatusRequest(
    val stock_status: StockStatusEnum,
    val status: Boolean? = null,
)
fun Product.toUpdateRequest(ui: ProductDetailUiState): UpdateProductRequest {
    val pt = productType?.name ?: "PHYSICAL"
    val tp = type?.name ?: "SIMPLE"

    // discount — jeśli w modelu masz Number? albo String?, łykamy oba warianty:
    val discountDouble =
        (discount as? Number)?.toDouble()
            ?: (discount as? String)?.toDoubleOrNull()
            ?: throw IllegalArgumentException("discount required")

    val shortDesc = shortDescription?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("short_description required")

    val unitValue = unit?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("unit required")

    val digitalIds = if (pt == "DIGITAL") {
        ui.digitalFileIds.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("digital_file_ids required for DIGITAL")
    } else null

    return UpdateProductRequest(
        name = name,
        short_description = shortDesc,
        description = description,
        sku = requireNotNull(sku?.takeIf { it.isNotBlank() }) { "sku required" },
        unit = unitValue,
        price = requireNotNull(price) { "price required" },
        sale_price = salePrice,
        discount = discountDouble,
        product_type = pt,
        type = tp,
        quantity = requireNotNull(quantity) { "quantity required" },
        is_sale_enable = if (isSaleEnable == true) 1 else 0, // number
        stock_status = stockStatus.name,
        status = ui.statusEnabled,                            // boolean
        categories = ui.categoryIds,                          // wymagane
        attributes_ids = ui.attributeIds.takeIf { it.isNotEmpty() }, // ★ null gdy pusto
        availability = availability ?: Availability(),
        digital_file_ids = digitalIds,
        tax_id = taxId,
        product_thumbnail_id = productThumbnailId,
        related_products = relatedProducts,
        cross_sell_products = crossSellProducts
    )
}
