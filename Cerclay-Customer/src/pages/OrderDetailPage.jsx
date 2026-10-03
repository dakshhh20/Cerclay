import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { cancelCustomerOrder, getOrder, getOrderShipment, getOrderShipmentTimeline } from "../lib/api";
import { useAuth } from "../context/AuthContext";

function money(value) {
  return `₹${Number(value ?? 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function pretty(value) {
  return String(value || "—").replaceAll("_", " ");
}

function formatDate(value) {
  if (!value) return "—";
  return new Date(value).toLocaleString("en-IN", { day: "numeric", month: "short", year: "numeric", hour: "numeric", minute: "2-digit" });
}

function shipmentSteps(status) {
  const current = String(status || "CREATED").toUpperCase();
  if (["CANCELLED", "RTO"].includes(current)) return ["CREATED", current];
  const steps = ["CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"];
  const index = steps.indexOf(current);
  return steps.map((step, i) => ({ label: pretty(step), done: index >= i }));
}

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const { customer, status } = useAuth();
  const [order, setOrder] = useState(null);
  const [shipment, setShipment] = useState(null);
  const [trackingTimeline, setTrackingTimeline] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [cancelBusy, setCancelBusy] = useState(false);
  const [cancelMessage, setCancelMessage] = useState("");

  async function load() {
    setLoading(true);
    setError("");
    try {
      const result = await getOrder(orderId);
      setOrder(result);
      try {
        const shipmentResult = await getOrderShipment(orderId);
        setShipment(shipmentResult);
        try {
          setTrackingTimeline(await getOrderShipmentTimeline(orderId));
        } catch {
          setTrackingTimeline([]);
        }
      } catch {
        setShipment(null);
        setTrackingTimeline([]);
      }
    } catch (err) {
      setError(err.message || "Unable to load this order.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (status === "loading") return;
    load();

    // Keep the tracking card fresh while the order is open. This lets an
    // admin-created Shadowfax AWB/status appear without requiring the
    // customer to manually refresh the page.
    const refreshTimer = window.setInterval(() => {
      load();
    }, 30000);

    return () => window.clearInterval(refreshTimer);
  }, [customer, status, orderId]);

  const steps = useMemo(() => shipmentSteps(shipment?.shipmentStatus), [shipment]);

  async function handleCancel() {
    const reason = window.prompt("Why would you like to cancel this order?", "Changed my mind");
    if (reason === null) return;
    setCancelBusy(true);
    setCancelMessage("");
    try {
      await cancelCustomerOrder(order.id, reason.trim() || "Customer requested cancellation");
      setCancelMessage("Your cancellation request was processed.");
      await load();
    } catch (err) {
      setCancelMessage(err.message || "Unable to cancel this order.");
    } finally {
      setCancelBusy(false);
    }
  }

  if (status === "loading" || loading) return <section className="page-container state-page"><p>Loading your order…</p></section>;
  if (error || !order) return <section className="page-container state-page"><div className="state-box error-state"><strong>Unable to load this order</strong><span>{error}</span><Link className="secondary-button" to="/account/orders">Back to orders</Link></div></section>;

  const canCancel = ["PLACED", "CONFIRMED", "PROCESSING"].includes(String(order.orderStatus || "").toUpperCase());
  const returnEligible = String(order.orderStatus || "").toUpperCase() === "DELIVERED" && order.deliveredAt && (new Date(order.deliveredAt).getTime() + 2 * 86400000 >= Date.now());
  const returnWindowText = order.deliveredAt ? `Return window until ${new Date(new Date(order.deliveredAt).getTime() + 2 * 86400000).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}` : "Return window unavailable";

  return (
    <section className="order-detail-page page-container">
      <div className="order-detail-heading">
        <div>
          <p className="eyebrow">ORDER {order.orderNumber}</p>
          <h1>{pretty(order.orderStatus)}</h1>
          <p>Placed {formatDate(order.createdAt)}</p>
        </div>
        <Link className="secondary-button" to="/account/orders">All orders</Link>
      </div>

      <div className="order-detail-layout">
        <main className="order-detail-main">
          <section className="order-detail-card">
            <div className="checkout-card-heading"><div><p className="eyebrow">ITEMS</p><h2>Your purchase</h2></div></div>
            <div className="order-detail-items">
              {(order.items || []).map((item) => (
                <div className="order-detail-item" key={item.id || `${item.productSku}-${item.productName}`}>
                  <div><strong>{item.productName}</strong><span>SKU: {item.productSku || "—"}</span></div>
                  <span>× {item.quantity}</span>
                  <strong>{money(item.total)}</strong>
                </div>
              ))}
            </div>
          </section>

          <section className="order-detail-card">
            <div className="checkout-card-heading"><div><p className="eyebrow">DELIVERY</p><h2>Tracking</h2></div></div>
            {!shipment ? (
              <div className="tracking-empty"><strong>Shipment not created yet.</strong><span>We’ll show courier and tracking details here once your order is handed to the delivery partner.</span></div>
            ) : (
              <>
                <div className="tracking-topline">
                  <div><span>Courier</span><strong>{shipment.courierName || shipment.providerCode || "—"}</strong></div>
                  <div><span>Tracking number</span><strong>{shipment.trackingNumber || "Pending"}</strong></div>
                  <div><span>Status</span><strong>{pretty(shipment.externalStatusDisplay || shipment.shipmentStatus)}</strong></div>
                </div>
                <div className="tracking-steps">
                  {steps.map((step) => <div className={step.done ? "tracking-step done" : "tracking-step"} key={step.label}><span className="tracking-dot" /><span>{step.label}</span></div>)}
                </div>
                {trackingTimeline.length > 0 && (
                  <div className="tracking-timeline">
                    <div className="tracking-timeline-heading">
                      <span>SHIPMENT UPDATES</span>
                      <strong>Latest delivery activity</strong>
                    </div>
                    {[...trackingTimeline].reverse().map((event, index) => (
                      <div className="tracking-timeline-event" key={`${event.eventAt || "event"}-${event.shipmentStatus}-${index}`}>
                        <span className="tracking-timeline-dot" />
                        <div>
                          <strong>{pretty(event.externalStatusDisplay || event.shipmentStatus)}</strong>
                          {event.remarks && <span>{event.remarks}</span>}
                          {event.location && <span>{event.location}</span>}
                          {event.eventAt && <small>{formatDate(event.eventAt)}</small>}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                <div className="tracking-meta">
                  {shipment.currentLocation && <div><span>Current location</span><strong>{shipment.currentLocation}</strong></div>}
                  {shipment.latestTrackingComment && <div><span>Latest update</span><strong>{shipment.latestTrackingComment}</strong></div>}
                  {shipment.lastEventAt && <div><span>Last event</span><strong>{formatDate(shipment.lastEventAt)}</strong></div>}
                </div>
                {shipment.customerTrackUrl && <a className="primary-button" href={shipment.customerTrackUrl} target="_blank" rel="noreferrer">Open courier tracking</a>}
              </>
            )}
          </section>

          <section className="order-detail-card">
            <div className="checkout-card-heading"><div><p className="eyebrow">DELIVERY ADDRESS</p><h2>Deliver to</h2></div></div>
            <div className="order-address-snapshot"><strong>{order.addressName}</strong><span>{order.addressPhone}</span><span>{order.addressLine1}</span>{order.addressLine2 && <span>{order.addressLine2}</span>}<span>{order.addressCity}, {order.addressState} — {order.addressPincode}</span></div>
          </section>
        </main>

        <aside className="order-detail-side">
          <section className="checkout-summary-card order-summary-card">
            <p className="eyebrow">PAYMENT</p>
            <h2>Order summary</h2>
            <div className="checkout-summary-lines">
              <div><span>Subtotal</span><strong>{money(order.subtotal)}</strong></div>
              <div><span>Discount</span><strong>− {money(order.discount)}</strong></div>
              {order.couponCode && <div><span>Coupon</span><strong>{order.couponCode}</strong></div>}
              <div><span>Shipping</span><strong>{Number(order.shippingCharge || 0) === 0 ? "Free" : money(order.shippingCharge)}</strong></div>
            </div>
            <div className="checkout-total"><span>Total</span><strong>{money(order.total)}</strong></div>
            <div className="order-payment-meta"><span>Method</span><strong>{order.paymentMethod === "COD" ? "Cash on delivery" : order.paymentMethod}</strong><span>Status</span><strong>{pretty(order.paymentStatus)}</strong></div>
            {canCancel && <button className="secondary-button danger-button" disabled={cancelBusy} onClick={handleCancel}>{cancelBusy ? "Cancelling…" : "Cancel order"}</button>}
            {returnEligible && <><Link className="primary-button return-action-button" to={`/account/orders/${order.id}/return`}>Request return</Link><p className="return-window-note">{returnWindowText}. Eligible reasons: damaged, defective, wrong or missing item. Change-of-mind returns are not accepted.</p></>}
            {cancelMessage && <p className="order-action-message">{cancelMessage}</p>}
          </section>
        </aside>
      </div>
    </section>
  );
}
