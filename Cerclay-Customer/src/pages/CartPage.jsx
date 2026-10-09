import React, { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { getStoreSettings } from "../lib/api";

function imageFor(product) {
  return (
    product?.images?.find((image) => image.primary)?.imageUrl ||
    product?.images?.[0]?.imageUrl ||
    product?.primaryImageUrl ||
    product?.imageUrl ||
    product?.image ||
    null
  );
}

function money(value) {
  return `₹${Number(value ?? 0).toLocaleString("en-IN")}`;
}

export default function CartPage() {
  const { cart, status, error, subtotal, updateItem, removeItem } = useCart();
  const { isAuthenticated } = useAuth();
  const [busyProductId, setBusyProductId] = useState(null);
  const [freeShippingThreshold, setFreeShippingThreshold] = useState(null);
  useEffect(() => {
    getStoreSettings()
      .then((settings) => {
        const value = Number(settings?.freeShippingThreshold);
        setFreeShippingThreshold(Number.isFinite(value) && value > 0 ? value : null);
      })
      .catch(() => setFreeShippingThreshold(null));
  }, []);
  const items = cart.items || [];
  const shippingProgress = useMemo(() => {
    if (!freeShippingThreshold) return null;
    const remaining = Math.max(freeShippingThreshold - Number(subtotal || 0), 0);
    return {
      remaining,
      percent: Math.min(100, (Number(subtotal || 0) / freeShippingThreshold) * 100),
    };
  }, [freeShippingThreshold, subtotal]);

  async function changeQuantity(productId, quantity, packSize = 1) {
    if (quantity < 1) return;
    setBusyProductId(productId);
    try {
      await updateItem(productId, quantity, packSize);
    } catch (err) {
      // Keep the server-backed cart unchanged when the request fails.
      window.alert(err.message || "Unable to update quantity.");
    } finally {
      setBusyProductId(null);
    }
  }

  async function remove(productId, packSize = 1) {
    setBusyProductId(productId);
    try {
      await removeItem(productId, packSize);
    } catch (err) {
      window.alert(err.message || "Unable to remove this item.");
    } finally {
      setBusyProductId(null);
    }
  }

  if (status === "loading") {
    return (
      <section className="cart-page">
        <div className="state-box">Loading your bag…</div>
      </section>
    );
  }

  if (status === "error") {
    return (
      <section className="cart-page">
        <div className="state-box error-state">
          <strong>We couldn't load your bag.</strong>
          <span>{error}</span>
          <button type="button" className="primary-button" onClick={() => window.location.reload()}>
            Try again
          </button>
        </div>
      </section>
    );
  }

  if (!items.length) {
    return (
      <section className="cart-page">
        <div className="cart-empty">
          <p className="eyebrow">YOUR BAG</p>
          <h1>Your bag is waiting.</h1>
          <p>Take a look around and find something made for your everyday rituals.</p>
          <Link to="/shop" className="primary-button">
            Continue shopping
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="cart-page">
      <div className="page-heading cart-heading">
        <div>
          <p className="eyebrow">YOUR BAG</p>
          <h1>Your cart</h1>
        </div>
        <p className="page-heading-copy">Your cart is saved with Cerclay while you browse.</p>
      </div>

      {shippingProgress && (
        <div
          className={`cart-shipping-progress ${shippingProgress.remaining <= 0 ? "complete" : ""}`}
        >
          <div>
            <span>
              {shippingProgress.remaining > 0
                ? `Add ${money(shippingProgress.remaining)} more for free delivery`
                : "You unlocked free delivery"}
            </span>
            <strong>{Math.round(shippingProgress.percent)}%</strong>
          </div>
          <div className="cart-progress-track">
            <span style={{ width: `${shippingProgress.percent}%` }} />
          </div>
        </div>
      )}

      <div className="cart-layout">
        <div className="cart-items-list">
          {items.map((item) => {
            const product = item.product || {};
            const productId = product.id;
            const image = imageFor(product);
            const quantity = Number(item.quantity || 0);
            const packSize = Number(item.packSize || 1);
            const linePrice =
              packSize === 2 ? Number(product.setOf2Price || 0) : Number(product.price || 0);
            const lineTotal = linePrice * quantity;
            const busy = busyProductId === productId;

            return (
              <article className="cart-line-item" key={item.id ?? productId}>
                <Link to={`/products/${product.slug || productId}`} className="cart-line-image">
                  {image ? (
                    <img src={image} alt={product.name || "Ceramic piece"} />
                  ) : (
                    <span>Cerclay</span>
                  )}
                </Link>

                <div className="cart-line-main">
                  <Link to={`/products/${product.slug || productId}`} className="cart-line-name">
                    {product.name || "Ceramic piece"}
                  </Link>
                  {product.category && (
                    <span className="cart-line-category">{product.category}</span>
                  )}
                  <span className="cart-line-unit">
                    {packSize === 2
                      ? `Set of 2 · ${money(linePrice)} per set`
                      : `${money(linePrice)} each`}
                  </span>

                  <div className="cart-line-controls">
                    <div
                      className="quantity-control"
                      aria-label={`Quantity for ${product.name || "product"}`}
                    >
                      <button
                        type="button"
                        disabled={busy || quantity <= 1}
                        onClick={() => changeQuantity(productId, quantity - 1, packSize)}
                      >
                        −
                      </button>
                      <span>{quantity}</span>
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => changeQuantity(productId, quantity + 1, packSize)}
                      >
                        +
                      </button>
                    </div>
                    <button
                      type="button"
                      className="remove-link"
                      disabled={busy}
                      onClick={() => remove(productId, packSize)}
                    >
                      Remove
                    </button>
                  </div>
                </div>

                <strong className="cart-line-total">{money(lineTotal)}</strong>
              </article>
            );
          })}
        </div>

        <aside className="cart-summary">
          <p className="eyebrow">CART TOTAL</p>
          <div className="estimated-total-row">
            <span>Estimated total</span>
            <strong>{money(subtotal)}</strong>
          </div>
          <div className="summary-note">Taxes included. Shipping calculated at checkout.</div>
          <div className="cart-summary-trust">
            <span>Secure payment</span>
            <span>Tracked delivery</span>
            <span>Easy support</span>
          </div>
          <Link
            to={isAuthenticated ? "/checkout" : "/login?next=/checkout"}
            className="primary-button cart-checkout-button"
          >
            {isAuthenticated ? "Proceed to checkout" : "Sign in to checkout"}
          </Link>
          <Link to="/shop" className="continue-shopping">
            Continue shopping
          </Link>
        </aside>
      </div>
    </section>
  );
}
