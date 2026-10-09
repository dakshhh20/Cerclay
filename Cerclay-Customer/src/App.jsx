import React from "react";
import { Routes, Route } from "react-router-dom";
import SiteHeader from "./components/SiteHeader";
import HomePage from "./pages/HomePage";
import CataloguePage from "./pages/CataloguePage";
import NotFoundPage from "./pages/NotFoundPage";
import ProductDetailPage from "./pages/ProductDetailPage";
import CartPage from "./pages/CartPage";
import AuthPage from "./pages/AuthPage";
import AccountPage from "./pages/AccountPage";
import AddressesPage from "./pages/AddressesPage";
import CheckoutPage from "./pages/CheckoutPage";
import OrderSuccessPage from "./pages/OrderSuccessPage";
import OrdersPage from "./pages/OrdersPage";
import OrderDetailPage from "./pages/OrderDetailPage";
import ReturnRequestPage from "./pages/ReturnRequestPage";
import ReturnsPage from "./pages/ReturnsPage";
import ReturnDetailPage from "./pages/ReturnDetailPage";
import WishlistPage from "./pages/WishlistPage";
import ReviewsPage from "./pages/ReviewsPage";
import ProtectedRoute from "./routes/ProtectedRoute";

function Protected({ children }) {
  return <ProtectedRoute>{children}</ProtectedRoute>;
}

export default function App() {
  return (
    <div className="app-shell">
      <SiteHeader />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/shop" element={<CataloguePage />} />
          <Route path="/products/:identifier" element={<ProductDetailPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/order-success/:orderId" element={<OrderSuccessPage />} />
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route path="/forgot-password" element={<AuthPage mode="forgot" />} />
          <Route
            path="/account"
            element={
              <Protected>
                <AccountPage />
              </Protected>
            }
          />
          <Route
            path="/account/addresses"
            element={
              <Protected>
                <AddressesPage />
              </Protected>
            }
          />
          <Route
            path="/account/wishlist"
            element={
              <Protected>
                <WishlistPage />
              </Protected>
            }
          />
          <Route
            path="/account/reviews"
            element={
              <Protected>
                <ReviewsPage />
              </Protected>
            }
          />
          <Route
            path="/account/orders"
            element={
              <Protected>
                <OrdersPage />
              </Protected>
            }
          />
          <Route
            path="/account/orders/:orderId"
            element={
              <Protected>
                <OrderDetailPage />
              </Protected>
            }
          />
          <Route
            path="/account/orders/:orderId/return"
            element={
              <Protected>
                <ReturnRequestPage />
              </Protected>
            }
          />
          <Route
            path="/account/returns"
            element={
              <Protected>
                <ReturnsPage />
              </Protected>
            }
          />
          <Route
            path="/account/returns/:returnId"
            element={
              <Protected>
                <ReturnDetailPage />
              </Protected>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </div>
  );
}
