import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getCustomerOrders } from "../lib/api";
import { useAuth } from "../context/AuthContext";

function money(value) {
  return `₹${Number(value ?? 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatDate(value) {
  if (!value) return "—";
  return new Date(value).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

export default function OrdersPage() {
  const { customer, status } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (status === "loading") return;
    getCustomerOrders()
      .then(setOrders)
      .catch((err) => setError(err.message || "Unable to load your orders."))
      .finally(() => setLoading(false));
  }, [customer, status]);

  if (status === "loading" || loading) return <section className="page-container state-page"><p>Loading your orders…</p></section>;
  if (error) return <section className="page-container state-page"><div className="state-box error-state"><strong>Unable to load orders</strong><span>{error}</span><button className="secondary-button" onClick={() => window.location.reload()}>Try again</button></div></section>;

  return (
    <section className="orders-page page-container">
      <div className="orders-heading">
        <div>
          <p className="eyebrow">MY ACCOUNT</p>
          <h1>Your orders</h1>
          <p>View your purchases, payment status and delivery progress.</p>
        </div>
        <Link className="secondary-button" to="/account">Back to account</Link>
      </div>

      {orders.length === 0 ? (
        <div className="state-box orders-empty">
          <strong>No orders yet.</strong>
          <span>Your completed purchases will appear here.</span>
          <Link className="primary-button" to="/shop">Start shopping</Link>
        </div>
      ) : (
        <div className="orders-list">
          {orders.map((order) => (
            <article className="order-card" key={order.id}>
              <div className="order-card-top">
                <div>
                  <p className="eyebrow">ORDER</p>
                  <h2>{order.orderNumber}</h2>
                  <span>{formatDate(order.createdAt)}</span>
                </div>
                <div className="order-status-group">
                  <span className="status-pill">{String(order.orderStatus || "PLACED").replaceAll("_", " ")}</span>
                  <strong>{money(order.total)}</strong>
                </div>
              </div>
              <div className="order-card-meta">
                <div><span>Items</span><strong>{order.items?.reduce((sum, item) => sum + Number(item.quantity || 0), 0) || 0}</strong></div>
                <div><span>Payment</span><strong>{order.paymentMethod === "COD" ? "Cash on delivery" : order.paymentStatus || "—"}</strong></div>
                <div><span>Deliver to</span><strong>{order.addressCity}, {order.addressPincode}</strong></div>
              </div>
              <div className="order-card-actions">
                <Link className="primary-button" to={`/account/orders/${order.id}`}>View order</Link>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
