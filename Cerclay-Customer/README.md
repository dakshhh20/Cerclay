# Cerclay Customer Website

Customer-facing React/Vite foundation for Cerclay.

## Run

1. Install Node.js.
2. Copy `.env.example` to `.env`.
3. Keep `VITE_API_BASE_URL=http://localhost:8080/api` for the current local backend.
4. Run `npm install`.
5. Run `npm run dev`.

The catalogue requests products from the existing Spring Boot backend. Product information is not hardcoded.

## Current scope

- Cerclay visual foundation
- Responsive header
- Homepage
- Shop/catalogue route
- Real `/api/products` connection
- Loading/error/empty states
- Product-card foundation
- React Router foundation

Next build stages: product detail, cart, customer authentication/account, addresses, checkout, payments, orders, tracking, returns/refunds.
