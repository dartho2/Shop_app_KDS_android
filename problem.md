przychodzi zamówienie:
2026-04-04 18:29:46.717 25968-26413 SOCKET_EVENT            com.itsorderchat                     D  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2026-04-04 18:29:46.717 25968-26413 SOCKET_EVENT            com.itsorderchat                     D  📥 ORDER_CREATED/PROCESSING received
2026-04-04 18:29:46.717 25968-26413 SOCKET_EVENT            com.itsorderchat                     D     ├─ orderId: 69d13c7bfdf8f3f49c4c5927
2026-04-04 18:29:46.717 25968-26413 SOCKET_EVENT            com.itsorderchat                     D     ├─ orderNumber: 00421660
2026-04-04 18:29:46.717 25968-26413 SOCKET_EVENT            com.itsorderchat                     D     ├─ status: ACCEPTED
2026-04-04 18:29:46.717 25968-26413 SOCKET_EVENT            com.itsorderchat                     D     ├─ createdAt: 2026-04-04T16:29:47.453Z
2026-04-04 18:29:46.717 25968-26413 SOCKET_EVENT            com.itsorderchat                     D     ├─ age: 0s (0min)
2026-04-04 18:29:46.717 25968-26413 SOCKET_EVENT            com.itsorderchat                     D     ├─ isProcessing: false
2026-04-04 18:29:46.718 25968-26413 SOCKET_EVENT            com.itsorderchat                     D     ├─ deliveryType: dine_in
2026-04-04 18:29:46.718 25968-26413 SOCKET_EVENT            com.itsorderchat                     D     └─ Will emit to Flow: false
2026-04-04 18:29:46.718 25968-26413 SocketStaf...ntsHandler com.itsorderchat                     D  Handling new order: 69d13c7bfdf8f3f49c4c5927 (Status: ACCEPTED) -> SILENTLY SAVING
2026-04-04 18:29:46.732 25968-26026 ContentValues           com.itsorderchat                     D  [emit] Order: Order(orderId=69d13c7bfdf8f3f49c4c5927, status=true, total=16.0, consumer=Consumer(name=DINE_IN, email=dine-in@local.pos, phone=, countryCode=+48), orderNumber=00421660, orderStatus=OrderStatus(name=accepted, sequence=4, slug=ACCEPTED), orderStatusActivities=null, paymentMethod=cash, paymentStatus=pending, paymentStatusRank=null, amount=16.0, taxTotal=1.19, shippingTotal=0.0, walletBalance=0.0, additionalFeeTotal=0.0, additionalFees=[], couponTotalDiscount=0.0, currency=null, isGuest=true, pointsAmount=0.0, usedPoint=null, createdAt=2026-04-04T16:29:47.453Z, updatedAt=null, shippingAddress=ShippingAddress(street=null, city=null, numberHome=null, numberFlat=null, coordinates=Coordinates(lat=0.0, lng=0.0)), deliveryInterval=2026-04-04T18:29:33.000Z, deliveryTime=2026-04-04T18:29:33.000Z, isAsap=true, products=[OrderProduct(discount=0.0, price=16.0, comment=null, note=[], salePrice=16.0, name=Hosomaki awokado, quantity=1, addonsGroup=[])], note=null, deliveryType=dine_in, courier=null, source=SourceOrder(sourceId=3c0537ba-ffc6-4071-b3b4-7ee08c0a320c, number=00421660, name=gopos), orderKey=3c0537ba-ffc6-4071-b3b4-7ee08c0a320c, ip=null, externalDelivery=ExternalDelivery(courier=null, status=null, trackingUrl=null, deliveryId=null, orderReference=null, pickupEta=null, dropoffEta=null, acceptedByCourier=null, acceptedAt=null, completedAt=null, rejected=null, rejectedReason=null, rejectedAt=null, handshakePin=null, courierLocation=null), type=asap, isScheduled=false)
2026-04-04 18:29:46.870 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
2026-04-04 18:29:46.874 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  VM: Filtruję listę 37 zamówień. Pokazuję tylko nowsze niż 2026-04-03T22:00:00Z (lub aktywne/zaplanowane).
2026-04-04 18:29:46.877 25968-25993 om.itsordercha          com.itsorderchat                     I  Waiting for a blocking GC ProfileSaver
2026-04-04 18:29:46.890 25968-25968 QUEUE_FILTER            com.itsorderchat                     D  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2026-04-04 18:29:46.891 25968-25968 QUEUE_FILTER            com.itsorderchat                     D  📋 QUEUE UPDATE - Total orders: 37
2026-04-04 18:29:46.891 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421660: status=ACCEPTED, orderId=69d13c7bfdf8f3f49c4c5927, inQueue=false
2026-04-04 18:29:46.892 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421659: status=ACCEPTED, orderId=69d13c14fdf8f3f49c4c5907, inQueue=false
2026-04-04 18:29:46.892 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421658: status=ACCEPTED, orderId=69d13abbfdf8f3f49c4c5879, inQueue=false
2026-04-04 18:29:46.892 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421657: status=ACCEPTED, orderId=69d13962fdf8f3f49c4c57f2, inQueue=false
2026-04-04 18:29:46.893 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 28477: status=ACCEPTED, orderId=69d13892fdf8f3f49c4c57a9, inQueue=false
2026-04-04 18:29:46.893 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421656: status=ACCEPTED, orderId=69d137f0fdf8f3f49c4c5769, inQueue=false
2026-04-04 18:29:46.893 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order F6C8D7: status=ACCEPTED, orderId=69d134c6fdf8f3f49c4c561c, inQueue=false
2026-04-04 18:29:46.894 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421655: status=ACCEPTED, orderId=69d13453fdf8f3f49c4c55f7, inQueue=false
2026-04-04 18:29:46.894 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421654: status=ACCEPTED, orderId=69d133bffdf8f3f49c4c55bc, inQueue=false
2026-04-04 18:29:46.894 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order N12RB: status=ACCEPTED, orderId=69d130b7fdf8f3f49c4c548f, inQueue=false
2026-04-04 18:29:46.894 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421653: status=ACCEPTED, orderId=69d12a8bfdf8f3f49c4c523b, inQueue=false
2026-04-04 18:29:46.895 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order PBR36H: status=ACCEPTED, orderId=69d12642fdf8f3f49c4c507d, inQueue=false
2026-04-04 18:29:46.895 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 5737E: status=ACCEPTED, orderId=69d124a1fdf8f3f49c4c4fe2, inQueue=false
2026-04-04 18:29:46.895 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TGDPYB: status=ACCEPTED, orderId=69d12326fdf8f3f49c4c4f64, inQueue=false
2026-04-04 18:29:46.895 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421652: status=ACCEPTED, orderId=69d121fefdf8f3f49c4c4ef6, inQueue=false
2026-04-04 18:29:46.896 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TQ3HJ3: status=ACCEPTED, orderId=69d12078fdf8f3f49c4c4e5c, inQueue=false
2026-04-04 18:29:46.896 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TD8Q3C: status=ACCEPTED, orderId=69d12013fdf8f3f49c4c4e2f, inQueue=false
2026-04-04 18:29:46.896 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order KW967C: status=ACCEPTED, orderId=69d11fb1fdf8f3f49c4c4e03, inQueue=false
2026-04-04 18:29:46.896 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order D6YRCF: status=ACCEPTED, orderId=69d11f06fdf8f3f49c4c4dd0, inQueue=false
2026-04-04 18:29:46.897 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order OSK3B: status=ACCEPTED, orderId=69d11d33fdf8f3f49c4c4d27, inQueue=false
2026-04-04 18:29:46.897 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421651: status=ACCEPTED, orderId=69d11866fdf8f3f49c4c4b64, inQueue=false
2026-04-04 18:29:46.897 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421650: status=ACCEPTED, orderId=69d111d8fdf8f3f49c4c48fe, inQueue=false
2026-04-04 18:29:46.898 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421649: status=ACCEPTED, orderId=69d10cdafdf8f3f49c4c472c, inQueue=false
2026-04-04 18:29:46.898 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order WGB7MT: status=ACCEPTED, orderId=69d109c0fdf8f3f49c4c459f, inQueue=false
2026-04-04 18:29:46.898 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 28476: status=ACCEPTED, orderId=69d1063dfdf8f3f49c4c4473, inQueue=false
2026-04-04 18:29:46.898 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421648: status=ACCEPTED, orderId=69d0fcacfdf8f3f49c4c36af, inQueue=false
2026-04-04 18:29:46.898 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order FVTMQR: status=ACCEPTED, orderId=69d0fb2cfdf8f3f49c4c3609, inQueue=false
2026-04-04 18:29:46.898 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order DWCKMR: status=ACCEPTED, orderId=69d0fb25fdf8f3f49c4c3606, inQueue=false
2026-04-04 18:29:46.898 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order WJFPF7: status=ACCEPTED, orderId=69d0f652fdf8f3f49c4c345a, inQueue=false
2026-04-04 18:29:46.898 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 101614264259-580: status=ACCEPTED, orderId=69d0f627fdf8f3f49c4c3440, inQueue=false
2026-04-04 18:29:46.899 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 101614258909-006: status=ACCEPTED, orderId=69d0f501fdf8f3f49c4c33e3, inQueue=false
2026-04-04 18:29:46.899 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 4PCDCG: status=ACCEPTED, orderId=69d0f45dfdf8f3f49c4c33b0, inQueue=false
2026-04-04 18:29:46.899 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order QPCCXV: status=ACCEPTED, orderId=69d0ee76fdf8f3f49c4c31a3, inQueue=false
2026-04-04 18:29:46.899 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 68MMI: status=ACCEPTED, orderId=69d0ed6cfdf8f3f49c4c314e, inQueue=false
2026-04-04 18:29:46.899 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order PDB9XV: status=ACCEPTED, orderId=69d0ece2fdf8f3f49c4c311b, inQueue=false
2026-04-04 18:29:46.899 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421647: status=ACCEPTED, orderId=69d0ec4bfdf8f3f49c4c30ea, inQueue=false
2026-04-04 18:29:46.899 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order QBHFDW: status=ACCEPTED, orderId=69d0e4f4fdf8f3f49c4c2e66, inQueue=false
2026-04-04 18:29:46.899 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     └─ Filtered PROCESSING: 0
2026-04-04 18:29:46.902 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
2026-04-04 18:29:46.905 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  VM: Filtruję listę 37 zamówień. Pokazuję tylko nowsze niż 2026-04-03T22:00:00Z (lub aktywne/zaplanowane).
2026-04-04 18:29:46.918 25968-25968 QUEUE_FILTER            com.itsorderchat                     D  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2026-04-04 18:29:46.919 25968-25968 QUEUE_FILTER            com.itsorderchat                     D  📋 QUEUE UPDATE - Total orders: 37
2026-04-04 18:29:46.919 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421660: status=ACCEPTED, orderId=69d13c7bfdf8f3f49c4c5927, inQueue=false
2026-04-04 18:29:46.919 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421659: status=ACCEPTED, orderId=69d13c14fdf8f3f49c4c5907, inQueue=false
2026-04-04 18:29:46.919 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421658: status=ACCEPTED, orderId=69d13abbfdf8f3f49c4c5879, inQueue=false
2026-04-04 18:29:46.920 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421657: status=ACCEPTED, orderId=69d13962fdf8f3f49c4c57f2, inQueue=false
2026-04-04 18:29:46.920 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 28477: status=ACCEPTED, orderId=69d13892fdf8f3f49c4c57a9, inQueue=false
2026-04-04 18:29:46.920 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421656: status=ACCEPTED, orderId=69d137f0fdf8f3f49c4c5769, inQueue=false
2026-04-04 18:29:46.921 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order F6C8D7: status=ACCEPTED, orderId=69d134c6fdf8f3f49c4c561c, inQueue=false
2026-04-04 18:29:46.921 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421655: status=ACCEPTED, orderId=69d13453fdf8f3f49c4c55f7, inQueue=false
2026-04-04 18:29:46.921 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421654: status=ACCEPTED, orderId=69d133bffdf8f3f49c4c55bc, inQueue=false
2026-04-04 18:29:46.921 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order N12RB: status=ACCEPTED, orderId=69d130b7fdf8f3f49c4c548f, inQueue=false
2026-04-04 18:29:46.921 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421653: status=ACCEPTED, orderId=69d12a8bfdf8f3f49c4c523b, inQueue=false
2026-04-04 18:29:46.921 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order PBR36H: status=ACCEPTED, orderId=69d12642fdf8f3f49c4c507d, inQueue=false
2026-04-04 18:29:46.921 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 5737E: status=ACCEPTED, orderId=69d124a1fdf8f3f49c4c4fe2, inQueue=false
2026-04-04 18:29:46.921 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TGDPYB: status=ACCEPTED, orderId=69d12326fdf8f3f49c4c4f64, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421652: status=ACCEPTED, orderId=69d121fefdf8f3f49c4c4ef6, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TQ3HJ3: status=ACCEPTED, orderId=69d12078fdf8f3f49c4c4e5c, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TD8Q3C: status=ACCEPTED, orderId=69d12013fdf8f3f49c4c4e2f, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order KW967C: status=ACCEPTED, orderId=69d11fb1fdf8f3f49c4c4e03, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order D6YRCF: status=ACCEPTED, orderId=69d11f06fdf8f3f49c4c4dd0, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order OSK3B: status=ACCEPTED, orderId=69d11d33fdf8f3f49c4c4d27, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421651: status=ACCEPTED, orderId=69d11866fdf8f3f49c4c4b64, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421650: status=ACCEPTED, orderId=69d111d8fdf8f3f49c4c48fe, inQueue=false
2026-04-04 18:29:46.922 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421649: status=ACCEPTED, orderId=69d10cdafdf8f3f49c4c472c, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order WGB7MT: status=ACCEPTED, orderId=69d109c0fdf8f3f49c4c459f, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 28476: status=ACCEPTED, orderId=69d1063dfdf8f3f49c4c4473, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421648: status=ACCEPTED, orderId=69d0fcacfdf8f3f49c4c36af, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order FVTMQR: status=ACCEPTED, orderId=69d0fb2cfdf8f3f49c4c3609, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order DWCKMR: status=ACCEPTED, orderId=69d0fb25fdf8f3f49c4c3606, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order WJFPF7: status=ACCEPTED, orderId=69d0f652fdf8f3f49c4c345a, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 101614264259-580: status=ACCEPTED, orderId=69d0f627fdf8f3f49c4c3440, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 101614258909-006: status=ACCEPTED, orderId=69d0f501fdf8f3f49c4c33e3, inQueue=false
2026-04-04 18:29:46.923 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 4PCDCG: status=ACCEPTED, orderId=69d0f45dfdf8f3f49c4c33b0, inQueue=false
2026-04-04 18:29:46.924 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order QPCCXV: status=ACCEPTED, orderId=69d0ee76fdf8f3f49c4c31a3, inQueue=false
2026-04-04 18:29:46.924 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 68MMI: status=ACCEPTED, orderId=69d0ed6cfdf8f3f49c4c314e, inQueue=false
2026-04-04 18:29:46.924 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order PDB9XV: status=ACCEPTED, orderId=69d0ece2fdf8f3f49c4c311b, inQueue=false
2026-04-04 18:29:46.924 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421647: status=ACCEPTED, orderId=69d0ec4bfdf8f3f49c4c30ea, inQueue=false
2026-04-04 18:29:46.924 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order QBHFDW: status=ACCEPTED, orderId=69d0e4f4fdf8f3f49c4c2e66, inQueue=false
2026-04-04 18:29:46.924 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     └─ Filtered PROCESSING: 0
2026-04-04 18:29:46.928 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
2026-04-04 18:29:46.930 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  VM: Filtruję listę 37 zamówień. Pokazuję tylko nowsze niż 2026-04-03T22:00:00Z (lub aktywne/zaplanowane).
2026-04-04 18:29:46.942 25968-25968 QUEUE_FILTER            com.itsorderchat                     D  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2026-04-04 18:29:46.942 25968-25968 QUEUE_FILTER            com.itsorderchat                     D  📋 QUEUE UPDATE - Total orders: 37
2026-04-04 18:29:46.942 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421660: status=ACCEPTED, orderId=69d13c7bfdf8f3f49c4c5927, inQueue=false
2026-04-04 18:29:46.943 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421659: status=ACCEPTED, orderId=69d13c14fdf8f3f49c4c5907, inQueue=false
2026-04-04 18:29:46.943 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421658: status=ACCEPTED, orderId=69d13abbfdf8f3f49c4c5879, inQueue=false
2026-04-04 18:29:46.943 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421657: status=ACCEPTED, orderId=69d13962fdf8f3f49c4c57f2, inQueue=false
2026-04-04 18:29:46.943 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 28477: status=ACCEPTED, orderId=69d13892fdf8f3f49c4c57a9, inQueue=false
2026-04-04 18:29:46.943 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421656: status=ACCEPTED, orderId=69d137f0fdf8f3f49c4c5769, inQueue=false
2026-04-04 18:29:46.943 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order F6C8D7: status=ACCEPTED, orderId=69d134c6fdf8f3f49c4c561c, inQueue=false
2026-04-04 18:29:46.943 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421655: status=ACCEPTED, orderId=69d13453fdf8f3f49c4c55f7, inQueue=false
2026-04-04 18:29:46.944 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421654: status=ACCEPTED, orderId=69d133bffdf8f3f49c4c55bc, inQueue=false
2026-04-04 18:29:46.944 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order N12RB: status=ACCEPTED, orderId=69d130b7fdf8f3f49c4c548f, inQueue=false
2026-04-04 18:29:46.944 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421653: status=ACCEPTED, orderId=69d12a8bfdf8f3f49c4c523b, inQueue=false
2026-04-04 18:29:46.944 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order PBR36H: status=ACCEPTED, orderId=69d12642fdf8f3f49c4c507d, inQueue=false
2026-04-04 18:29:46.944 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 5737E: status=ACCEPTED, orderId=69d124a1fdf8f3f49c4c4fe2, inQueue=false
2026-04-04 18:29:46.944 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TGDPYB: status=ACCEPTED, orderId=69d12326fdf8f3f49c4c4f64, inQueue=false
2026-04-04 18:29:46.944 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421652: status=ACCEPTED, orderId=69d121fefdf8f3f49c4c4ef6, inQueue=false
2026-04-04 18:29:46.945 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TQ3HJ3: status=ACCEPTED, orderId=69d12078fdf8f3f49c4c4e5c, inQueue=false
2026-04-04 18:29:46.945 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order TD8Q3C: status=ACCEPTED, orderId=69d12013fdf8f3f49c4c4e2f, inQueue=false
2026-04-04 18:29:46.945 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order KW967C: status=ACCEPTED, orderId=69d11fb1fdf8f3f49c4c4e03, inQueue=false
2026-04-04 18:29:46.945 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order D6YRCF: status=ACCEPTED, orderId=69d11f06fdf8f3f49c4c4dd0, inQueue=false
2026-04-04 18:29:46.945 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order OSK3B: status=ACCEPTED, orderId=69d11d33fdf8f3f49c4c4d27, inQueue=false
2026-04-04 18:29:46.945 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421651: status=ACCEPTED, orderId=69d11866fdf8f3f49c4c4b64, inQueue=false
2026-04-04 18:29:46.946 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421650: status=ACCEPTED, orderId=69d111d8fdf8f3f49c4c48fe, inQueue=false
2026-04-04 18:29:46.946 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421649: status=ACCEPTED, orderId=69d10cdafdf8f3f49c4c472c, inQueue=false
2026-04-04 18:29:46.946 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order WGB7MT: status=ACCEPTED, orderId=69d109c0fdf8f3f49c4c459f, inQueue=false
2026-04-04 18:29:46.946 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 28476: status=ACCEPTED, orderId=69d1063dfdf8f3f49c4c4473, inQueue=false
2026-04-04 18:29:46.946 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421648: status=ACCEPTED, orderId=69d0fcacfdf8f3f49c4c36af, inQueue=false
2026-04-04 18:29:46.946 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order FVTMQR: status=ACCEPTED, orderId=69d0fb2cfdf8f3f49c4c3609, inQueue=false
2026-04-04 18:29:46.946 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order DWCKMR: status=ACCEPTED, orderId=69d0fb25fdf8f3f49c4c3606, inQueue=false
2026-04-04 18:29:46.947 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order WJFPF7: status=ACCEPTED, orderId=69d0f652fdf8f3f49c4c345a, inQueue=false
2026-04-04 18:29:46.947 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 101614264259-580: status=ACCEPTED, orderId=69d0f627fdf8f3f49c4c3440, inQueue=false
2026-04-04 18:29:46.947 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 101614258909-006: status=ACCEPTED, orderId=69d0f501fdf8f3f49c4c33e3, inQueue=false
2026-04-04 18:29:46.947 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 4PCDCG: status=ACCEPTED, orderId=69d0f45dfdf8f3f49c4c33b0, inQueue=false
2026-04-04 18:29:46.947 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order QPCCXV: status=ACCEPTED, orderId=69d0ee76fdf8f3f49c4c31a3, inQueue=false
2026-04-04 18:29:46.947 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 68MMI: status=ACCEPTED, orderId=69d0ed6cfdf8f3f49c4c314e, inQueue=false
2026-04-04 18:29:46.947 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order PDB9XV: status=ACCEPTED, orderId=69d0ece2fdf8f3f49c4c311b, inQueue=false
2026-04-04 18:29:46.948 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order 00421647: status=ACCEPTED, orderId=69d0ec4bfdf8f3f49c4c30ea, inQueue=false
2026-04-04 18:29:46.948 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     Order QBHFDW: status=ACCEPTED, orderId=69d0e4f4fdf8f3f49c4c2e66, inQueue=false
2026-04-04 18:29:46.948 25968-25968 QUEUE_FILTER            com.itsorderchat                     D     └─ Filtered PROCESSING: 0
2026-04-04 18:29:46.961 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order TGDPYB with delivery 2026-04-04T16:30:00.000Z, isScheduled=false
2026-04-04 18:29:46.967 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 5737E with delivery 2026-04-04T16:18:15.000Z, isScheduled=false
2026-04-04 18:29:46.973 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order PBR36H with delivery 2026-04-04T16:24:58.000Z, isScheduled=false
2026-04-04 18:29:46.978 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 00421653 with delivery 2026-04-04T17:12:56.000Z, isScheduled=false
2026-04-04 18:29:46.983 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order N12RB with delivery 2026-04-04T17:40:28.000Z, isScheduled=false
2026-04-04 18:29:46.989 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 00421654 with delivery 2026-04-04T17:51:19.000Z, isScheduled=false
2026-04-04 18:29:46.994 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 00421655 with delivery 2026-04-04T17:54:43.000Z, isScheduled=false
2026-04-04 18:29:47.000 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order F6C8D7 with delivery 2026-04-04T17:26:54.000Z, isScheduled=false
2026-04-04 18:29:47.006 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 00421656 with delivery 2026-04-04T18:10:13.000Z, isScheduled=false
2026-04-04 18:29:47.011 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 28477 with delivery 2026-04-04T17:00:00.000Z, isScheduled=false
2026-04-04 18:29:47.015 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 00421657 with delivery 2026-04-04T18:16:26.000Z, isScheduled=false
2026-04-04 18:29:47.020 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 00421658 with delivery 2026-04-04T18:22:01.000Z, isScheduled=false
2026-04-04 18:29:47.026 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 00421659 with delivery 2026-04-04T18:27:51.000Z, isScheduled=false
2026-04-04 18:29:47.042 25968-25968 OrderListItem           com.itsorderchat                     D  Rendering order 00421660 with delivery 2026-04-04T18:29:33.000Z, isScheduled=false
2026-04-04 18:29:47.111 25968-25983 om.itsordercha          com.itsorderchat                     I  Background concurrent copying GC freed 286324(10MB) AllocSpace objects, 16(320KB) LOS objects, 26% free, 16MB/22MB, paused 150us total 273.034ms
2026-04-04 18:29:47.111 25968-25993 om.itsordercha          com.itsorderchat                     I  WaitForGcToComplete blocked ProfileSaver on HeapTrim for 234.495ms
2026-04-04 18:29:47.119   544-879   BufferQueueProducer     surfaceflinger                       I  [com.itsorderchat/com.itsorderchat.ui.theme.home.HomeActivity#0](this:0xb40000792964b1f8,id:381,api:1,p:25968,c:544) queueBuffer: fps=0.03 dur=33677.78 max=33677.78 min=33677.78
2026-04-04 18:29:47.139   544-544   SurfaceFlinger          surfaceflinger                       I  operator()(), mtkRenderCntDebug 7565, screenshot (com.itsorderchat/com.itsorderchat.ui.theme.home.HomeActivity#0)
2026-04-04 18:29:47.238 25968-25968 OrdersView...cketEvents com.itsorderchat                     D  📥 Received order from socket: orderId=69d13c7bfdf8f3f49c4c5927, externalDelivery.status=null
2026-04-04 18:29:47.241 25968-25968 chatty                  com.itsorderchat                     I  uid=10203(com.itsorderchat) identical 1 line
2026-04-04 18:29:47.243 25968-25968 OrdersView...cketEvents com.itsorderchat                     D  📥 Received order from socket: orderId=69d13c7bfdf8f3f49c4c5927, externalDelivery.status=null
2026-04-04 18:29:47.245 25968-25968 OrdersView...cketEvents com.itsorderchat                     D  💾 Order saved to database: orderId=69d13c7bfdf8f3f49c4c5927
2026-04-04 18:29:47.252 25968-25968 AUTO_PRINT_DINE_IN      com.itsorderchat                     D  ⏭️ Auto-druk DINE_IN wyłączony w ustawieniach
2026-04-04 18:29:47.254 25968-25968 OrdersView...cketEvents com.itsorderchat                     D  💾 Order saved to database: orderId=69d13c7bfdf8f3f49c4c5927
2026-04-04 18:29:47.256 25968-25968 AUTO_PRINT_DINE_IN      com.itsorderchat                     D  ⏭️ Auto-druk DINE_IN wyłączony w ustawieniach
2026-04-04 18:29:47.263 25968-25968 OrdersView...cketEvents com.itsorderchat                     D  💾 Order saved to database: orderId=69d13c7bfdf8f3f49c4c5927
2026-04-04 18:29:47.268 25968-25968 AUTO_PRINT_DINE_IN      com.itsorderchat                     D  ⏭️ Auto-druk DINE_IN wyłączony w ustawieniach
2026-04-04 18:29:47.346 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
2026-04-04 18:29:47.351 25968-25968 chatty                  com.itsorderchat                     I  uid=10203(com.itsorderchat) identical 1 line
2026-04-04 18:29:47.364 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
2026-04-04 18:29:47.484 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
2026-04-04 18:29:47.491 25968-25968 chatty                  com.itsorderchat                     I  uid=10203(com.itsorderchat) identical 1 line
2026-04-04 18:29:47.504 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
2026-04-04 18:29:51.187   544-544   SurfaceFlinger          surfaceflinger                       I  operator()(), mtkRenderCntDebug 7566, screenshot (com.itsorderchat/com.itsorderchat.ui.theme.home.HomeActivity#0)
2026-04-04 18:29:54.219   544-544   SurfaceFlinger          surfaceflinger                       I  operator()(), mtkRenderCntDebug 7567, screenshot (com.itsorderchat/com.itsorderchat.ui.theme.home.HomeActivity#0)
2026-04-04 18:30:00.067   544-544   SurfaceFlinger          surfaceflinger                       I  operator()(), mtkRenderCntDebug 7568, screenshot (com.itsorderchat/com.itsorderchat.ui.theme.home.HomeActivity#0)
2026-04-04 18:30:03.301   544-544   SurfaceFlinger          surfaceflinger                       I  operator()(), mtkRenderCntDebug 7569, screenshot (com.itsorderchat/com.itsorderchat.ui.theme.home.HomeActivity#0)
2026-04-04 18:30:06.324   544-544   SurfaceFlinger          surfaceflinger                       I  operator()(), mtkRenderCntDebug 7570, screenshot (com.itsorderchat/com.itsorderchat.ui.theme.home.HomeActivity#0)



tutaj dostaje aktualizacje ktora moze sie tyczyc również produktów i kwoty
2026-04-04 18:30:53.459 25968-26444 SOCKET_EVENT            com.itsorderchat                     D  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2026-04-04 18:30:53.460 25968-26444 SOCKET_EVENT            com.itsorderchat                     D  📊 STATUS UPDATE received
2026-04-04 18:30:53.460 25968-26444 SOCKET_EVENT            com.itsorderchat                     D     ├─ orderId: 69d13c7bfdf8f3f49c4c5927
2026-04-04 18:30:53.460 25968-26444 SOCKET_EVENT            com.itsorderchat                     D     ├─ action: ACTION_ORDER_UPDATE
2026-04-04 18:30:53.460 25968-26444 SOCKET_EVENT            com.itsorderchat                     D     ├─ newStatus: ACCEPTED
2026-04-04 18:30:53.460 25968-26444 SOCKET_EVENT            com.itsorderchat                     D     ├─ Source: WebSocket
2026-04-04 18:30:53.460 25968-26444 SOCKET_EVENT            com.itsorderchat                     D     └─ Timestamp: 1775320253460
2026-04-04 18:30:53.461 25968-26444 EVENT                   com.itsorderchat                     I  ORDER: 69d13c7bfdf8f3f49c4c5927, Action: ACTION_ORDER_UPDATE
2026-04-04 18:30:53.584 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
2026-04-04 18:30:53.590 25968-25968 chatty                  com.itsorderchat                     I  uid=10203(com.itsorderchat) identical 1 line
2026-04-04 18:30:53.608 25968-25968 OrdersView...nlined$map com.itsorderchat                     D  📂 Baza danych (getAllOrdersFlow) wyemitowała 37 encji.
