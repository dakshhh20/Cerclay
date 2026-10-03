# Shadowfax manual AWB workflow

When a shipment is created manually in the Shadowfax dashboard, the admin can link its AWB to an existing Cerclay order with:

`POST /api/admin/shadowfax/orders/{orderId}/awb`

JSON body:

```json
{
  "awb": "SF123456789"
}
```

The endpoint:

1. Creates the local shipment record if the order does not already have one.
2. Assigns courier `Shadowfax` and provider code `SHADOWFAX`.
3. Saves the supplied AWB.
4. Records a local shipment-created tracking event.
5. Immediately attempts a Shadowfax tracking sync.
6. If Shadowfax has not exposed the AWB to tracking yet, the AWB remains saved and the normal polling/webhook flow can update it later.

The endpoint is protected by the existing `/api/admin/**` admin security rule.

The customer order page refreshes its order/tracking data every 30 seconds while open, so an admin-created shipment can appear without a manual browser refresh.
