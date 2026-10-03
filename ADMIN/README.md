# Cerclay Admin

Production-oriented admin frontend for Cerclay.

## Current modules
- Secure admin session login/logout
- Dashboard and orders
- Products and product images
- Inventory adjustments and inventory history
- Customers
- Discounts & promotions
- Distance/zone shipping rules and pincode overrides
- Payments
- Customer communications: editable email templates and delivery history
- Store settings

## Backend
Set `VITE_API_BASE_URL` when the API is not `http://localhost:8080`.

The frontend never owns business truth for prices, stock, discounts, shipping or notifications. Admin changes go through the backend and database.
