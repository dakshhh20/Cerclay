import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getOrder } from "../lib/api";

function money(value) {
  return `₹${Number(value ?? 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export default function OrderSuccessPage() {
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getOrder(orderId).then(setOrder).catch((err) => setError(err.message || "Unable to load your order."));
  }, [orderId]);

  if (error) {
    return <section className="order-success page-container"><div className="state-box error-state"><strong>Your order was submitted.</strong><span>{error}</span><Link className="primary-button" to="/account">Go to account</Link></div></section>;
  }

  if (!order) return <section className="order-success page-container"><div className="state-box">Loading your order…</div></section>;

  return (
    <section className="order-success page-container">
      <div className="success-card">
        <p className="eyebrow">ORDER CONFIRMED</p>
        <div className="success-mark">✓</div>
        <h1>Thank you for your order.</h1>
        <p className="success-copy">Your order <strong>{order.orderNumber}</strong> has been placed. We’ll keep the order status and delivery updates connected to your account.</p>
        <div className="success-details">
          <div><span>Total</span><strong>{money(order.total)}</strong></div>
          <div><span>Payment</span><strong>{order.paymentMethod === "COD" ? "Cash on delivery" : order.paymentStatus}</strong></div>
          <div><span>Deliver to</span><strong>{order.addressCity}, {order.addressPincode}</strong></div>
        </div>
        <div className="success-actions">
          <Link className="primary-button" to="/account">Go to account</Link>
          <Link className="secondary-button" to="/shop">Continue shopping</Link>
        </div>
      </div>
    </section>
  );
}
