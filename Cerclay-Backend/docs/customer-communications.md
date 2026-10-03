# Customer Communications

Cerclay customer communication uses a database-backed email notification queue.

## Events
- ORDER_PLACED
- ORDER_SHIPPED
- ORDER_OUT_FOR_DELIVERY
- ORDER_DELIVERED
- ORDER_CANCELLED

## Flow
1. An order/shipment event occurs in the backend.
2. The backend renders the current database template and creates a notification row.
3. A scheduled worker claims pending notifications.
4. Spring Mail sends the email through the configured SMTP provider.
5. Delivery status is recorded as SENT, FAILED, or FAILED_PERMANENT with retry information.

Business events do not depend on the frontend being open.

## Configuration
SMTP credentials must be supplied through environment variables/secrets:
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `CERCLAY_MAIL_ENABLED=true`
- `CERCLAY_MAIL_FROM`

No SMTP password is stored in the frontend or database.

## Admin
`/api/admin/notifications/templates` manages customer email templates.
`/api/admin/notifications/history` returns recent notification delivery records.

Supported template placeholders:
`{{customerName}}`, `{{orderNumber}}`, `{{total}}`, `{{paymentMethod}}`, `{{status}}`, `{{trackingNumber}}`, `{{trackingUrl}}`.
