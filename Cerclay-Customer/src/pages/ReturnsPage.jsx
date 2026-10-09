import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getCustomerReturns } from "../lib/api";
function pretty(v) {
  return String(v || "—")
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}
function money(v) {
  return `₹${Number(v || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}
function date(v) {
  return v
    ? new Date(v).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })
    : "—";
}
export default function ReturnsPage() {
  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    getCustomerReturns()
      .then(setReturns)
      .catch((e) => setError(e.message || "Unable to load returns."))
      .finally(() => setLoading(false));
  }, []);
  if (loading)
    return (
      <section className="page-container state-page">
        <p>Loading returns…</p>
      </section>
    );
  if (error)
    return (
      <section className="page-container state-page">
        <div className="state-box error-state">
          <strong>Unable to load returns</strong>
          <span>{error}</span>
        </div>
      </section>
    );
  return (
    <section className="returns-page page-container">
      <div className="orders-heading">
        <div>
          <p className="eyebrow">MY ACCOUNT</p>
          <h1>Returns & refunds</h1>
          <p>Track your return requests and refund progress.</p>
        </div>
        <Link className="secondary-button" to="/account/orders">
          Back to orders
        </Link>
      </div>
      {returns.length === 0 ? (
        <div className="state-box orders-empty">
          <strong>No return requests yet.</strong>
          <span>Eligible returns can be started from an order within 2 days of delivery.</span>
          <Link className="primary-button" to="/account/orders">
            View orders
          </Link>
        </div>
      ) : (
        <div className="orders-list">
          {returns.map((r) => (
            <article className="order-card" key={r.id}>
              <div className="order-card-top">
                <div>
                  <p className="eyebrow">RETURN · ORDER {r.orderNumber}</p>
                  <h2>{pretty(r.reason)}</h2>
                  <span>Requested {date(r.requestedAt)}</span>
                </div>
                <div className="order-status-group">
                  <span className="status-pill">{pretty(r.status)}</span>
                  <strong>{money(r.estimatedRefundAmount)}</strong>
                </div>
              </div>
              <div className="order-card-meta">
                <div>
                  <span>Items</span>
                  <strong>{r.items?.reduce((s, i) => s + Number(i.quantity || 0), 0) || 0}</strong>
                </div>
                <div>
                  <span>Refund</span>
                  <strong>
                    {r.refunds?.[0]?.status ? pretty(r.refunds[0].status) : "Pending"}
                  </strong>
                </div>
                <div>
                  <span>Photos</span>
                  <strong>{r.photos?.length || 0}</strong>
                </div>
              </div>
              <div className="order-card-actions">
                <Link className="primary-button" to={`/account/returns/${r.id}`}>
                  View return
                </Link>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
