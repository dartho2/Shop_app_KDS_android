package com.itsorderkds.data.network

import com.itsorderkds.data.model.BatchUpdateStatusRequest
import com.itsorderkds.data.model.DispatchCancelCourier
import com.itsorderkds.data.model.DispatchCourier
import com.itsorderkds.data.model.DispatchResponse
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.OrderTras
import com.itsorderkds.data.model.PaginatedOrders
import com.itsorderkds.data.model.ShiftCheckIn
import com.itsorderkds.data.model.ShiftCheckOut
import com.itsorderkds.data.model.ShiftResponse
import com.itsorderkds.data.model.UpdateCourierOrder
import com.itsorderkds.data.model.UpdateOrderData
import com.itsorderkds.ui.order.OrderStatusEnum
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApi {
    @GET("client/v3/api/admin/order?paginate=200")
    suspend fun getOrders(

        @Query("createdAt") startDate: String? = null
    ): Response<PaginatedOrders>

    @GET("client/v3/api/admin/order/{orderId}/show")
    suspend fun getOrderById(@Path("orderId") orderId: String): Response<Order>

    @POST("client/v3/api/admin/orders/{orderId}/commands/dispatch")
    suspend fun sendToExternalCourier(
        @Path("orderId") orderId: String,
        @Body body: DispatchCourier
    ): Response<DispatchResponse>

    @POST("client/v3/api/admin/orders/{orderId}/commands/cancel-dispatch")
    suspend fun cancledToExternalCourier(
        @Path("orderId") orderId: String,
        @Body body: DispatchCancelCourier
    ): Response<DispatchResponse>

    @PUT("client/v3/api/admin/order/{orderId}/status/{status}")
    suspend fun updateOrder(
        @Path("orderId") orderId: String,
        @Path("status") status: String,
        @Body order: UpdateOrderData
    ): Response<Order>

    @PUT("client/v3/api/courier/order/{orderId}/assign-courier")
    suspend fun assignCourier(
        @Path("orderId") orderId: String,
        @Body courier: UpdateCourierOrder
    ): Response<Order>

    @GET("client/v3/api/courier/shift/status")
    suspend fun shiftStatus(@Query("date") date: String): Response<ShiftResponse>

    @GET("client/v3/api/courier/order/tras")
    suspend fun getOrderTras(): Response<List<OrderTras>>

    @POST("client/v3/api/courier/shift/checkin")
    suspend fun checkIn(@Body body: ShiftCheckIn): Response<ShiftResponse>

    @POST("client/v3/api/courier/shift/checkout")
    suspend fun checkOut(@Body body: ShiftCheckOut): Response<ShiftResponse>

    @PUT("client/v3/api/v2/admin/order/statuses")
    suspend fun batchUpdateOrderStatus(@Body body: BatchUpdateStatusRequest): Response<List<OrderTras>>
}
