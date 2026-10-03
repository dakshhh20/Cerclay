import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Check, CreditCard, MapPin, Package, ShieldCheck, Sparkles, Tag, Truck } from "lucide-react";
import {
  createAddress,
  createOrder,
  createCashfreeOrder,
  getCheckoutSummary,
  getCustomerAddresses,
  getAvailableDiscounts,
  verifyCashfreePayment,
} from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";

const emptyAddress = {
  name: "",
  phone: "",
  house: "",
  street: "",
  city: "",
  state: "",
  pincode: "",
  addressType: "HOME",
  defaultAddress: false,
};

function money(value) {
  return `₹${Number(value ?? 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

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

function loadCashfreeScript() {
  if (window.Cashfree) return Promise.resolve(true);
  return new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-cashfree="checkout"]');
    if (existing) {
      existing.addEventListener("load", () => resolve(true), { once: true });
      existing.addEventListener("error", () => reject(new Error("Unable to load Cashfree checkout.")), { once: true });
      return;
    }
    const script = document.createElement("script");
    script.src = "https://sdk.cashfree.com/js/v3/cashfree.js";
    script.async = true;
    script.dataset.cashfree = "checkout";
    script.onload = () => resolve(true);
    script.onerror = () => reject(new Error("Unable to load Cashfree checkout."));
    document.body.appendChild(script);
  });
}

export default function CheckoutPage() {
  const { customer, status } = useAuth();
  const { cart, refreshCart } = useCart();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("CASHFREE");
  const [couponInput, setCouponInput] = useState("");
  const [couponCode, setCouponCode] = useState("");
  const [summary, setSummary] = useState(null);
  const [loadingAddresses, setLoadingAddresses] = useState(true);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState("");
  const [couponMessage, setCouponMessage] = useState("");
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [addressForm, setAddressForm] = useState(emptyAddress);
  const [savingAddress, setSavingAddress] = useState(false);
  const [availableDiscounts, setAvailableDiscounts] = useState([]);

  const items = cart?.items || [];

  const selectedAddress = useMemo(
    () => addresses.find((address) => String(address.id) === String(selectedAddressId)) || null,
    [addresses, selectedAddressId]
  );

  const loadAddresses = useCallback(async () => {
    if (!customer?.id) return;
    setLoadingAddresses(true);
    try {
      const data = await getCustomerAddresses(customer.id);
      const list = Array.isArray(data) ? data : [];
      setAddresses(list);
      setSelectedAddressId((current) => {
        if (current && list.some((address) => String(address.id) === String(current))) return current;
        return String((list.find((address) => address.defaultAddress) || list[0])?.id || "");
      });
    } catch (err) {
      setError(err.message || "Unable to load your delivery addresses.");
    } finally {
      setLoadingAddresses(false);
    }
  }, [customer?.id]);

  useEffect(() => {
    if (status === "authenticated") loadAddresses();
  }, [status, loadAddresses]);

  useEffect(() => {
    const returnedOrderId = searchParams.get("cashfree_order_id");
    if (status !== "authenticated" || !returnedOrderId) return;
    let active = true;
    setPlacing(true);
    setError("");
    verifyCashfreePayment(returnedOrderId)
      .then(async (verified) => {
        if (!active) return;
        await refreshCart();
        navigate(`/order-success/${verified.orderId}`, { replace: true });
      })
      .catch((err) => {
        if (!active) return;
        setError(err.message || "Cashfree payment was not completed. Please retry payment from your order.");
        setPlacing(false);
      })
      .finally(() => {
        if (active) setSearchParams({}, { replace: true });
      });
    return () => { active = false; };
  }, [status, searchParams, refreshCart, navigate, setSearchParams]);

  useEffect(() => {
    let active = true;
    getAvailableDiscounts().then((data) => {
      if (active) setAvailableDiscounts(Array.isArray(data) ? data : []);
    }).catch(() => { if (active) setAvailableDiscounts([]); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!selectedAddressId || !customer) {
      setSummary(null);
      return;
    }

    let cancelled = false;
    setSummaryLoading(true);
    setError("");

    getCheckoutSummary({
      addressId: Number(selectedAddressId),
      paymentMethod,
      couponCode: couponCode || null,
    })
      .then((data) => {
        if (!cancelled) {
          setSummary(data);
          setCouponMessage(couponCode ? `Coupon ${couponCode} applied.` : "");
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setSummary(null);
          if (couponCode) {
            const message = err.message || "This coupon cannot be applied to this order.";
            setCouponMessage(`Coupon ${couponCode} is inapplicable. ${message}`);
            setError("");
          } else {
            setCouponMessage("");
            setError(err.message || "Unable to calculate your checkout total.");
          }
        }
      })
      .finally(() => {
        if (!cancelled) setSummaryLoading(false);
      });

    return () => { cancelled = true; };
  }, [selectedAddressId, paymentMethod, couponCode, customer]);

  if (status === "loading") {
    return <section className="checkout-page page-container"><div className="state-box">Loading checkout…</div></section>;
  }

  if (!customer) {
    navigate("/login?next=/checkout", { replace: true });
    return null;
  }

  if (!items.length) {
    return (
      <section className="checkout-page page-container">
        <div className="checkout-empty">
          <p className="eyebrow">CHECKOUT</p>
          <h1>Your bag is empty.</h1>
          <p>Add something to your bag before starting checkout.</p>
          <Link className="primary-button" to="/shop">Continue shopping</Link>
        </div>
      </section>
    );
  }

  function applyCoupon() {
    const next = couponInput.trim().toUpperCase();
    setCouponMessage("");
    if (!next) {
      setCouponCode("");
      return;
    }
    setCouponCode(next);
    setCouponMessage("Checking coupon…");
  }

  function removeCoupon() {
    setCouponInput("");
    setCouponCode("");
    setCouponMessage("");
  }

  function changeAddressForm(event) {
    const { name, value, type, checked } = event.target;
    setAddressForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  }

  async function saveNewAddress(event) {
    event.preventDefault();
    setSavingAddress(true);
    setError("");
    try {
      const saved = await createAddress(customer.id, addressForm);
      await loadAddresses();
      if (saved?.id) setSelectedAddressId(String(saved.id));
      setAddressForm(emptyAddress);
      setShowAddressForm(false);
    } catch (err) {
      setError(err.message || "Unable to save this address.");
    } finally {
      setSavingAddress(false);
    }
  }

  async function placeOrder() {
    if (!selectedAddressId) {
      setError("Please select a delivery address.");
      return;
    }
    if (!summary?.serviceable) {
      setError("This address is not currently serviceable.");
      return;
    }

    setPlacing(true);
    setError("");
    try {
      const order = await createOrder({
        addressId: Number(selectedAddressId),
        paymentMethod,
        couponCode: couponCode || null,
      });

      if (paymentMethod === "COD") {
        await refreshCart();
        navigate(`/order-success/${order.id}`, { replace: true });
        return;
      }

      const cashfreeOrder = await createCashfreeOrder(order.id);
      await loadCashfreeScript();

      const cashfree = window.Cashfree({
        mode: cashfreeOrder.environment === "production" ? "production" : "sandbox",
      });
      await cashfree.checkout({
        paymentSessionId: cashfreeOrder.paymentSessionId,
        redirectTarget: "_self",
      });
    } catch (err) {
      setError(err.message || "Unable to place your order.");
      setPlacing(false);
    }
  }

  return (
    <section className="checkout-page page-container">
      <div className="checkout-heading checkout-heading-editorial">
        <div className="checkout-heading-copy">
          <div className="checkout-kicker"><span>SECURE CHECKOUT</span><i /> <span>STEP 01 — 04</span></div>
          <h1>Complete your order</h1>
          <p>Choose your delivery details, review your total and continue to a secure Cashfree payment.</p>
        </div>
        <div className="checkout-heading-side">
          <div className="checkout-progress" aria-label="Checkout progress">
            <span className="active"><b>01</b><small>Delivery</small></span>
            <i /><span><b>02</b><small>Payment</small></span><i /><span><b>03</b><small>Review</small></span>
          </div>
          <Link className="secondary-button" to="/cart">Back to bag <Package size={15} /></Link>
        </div>
      </div>
      <div className="checkout-trust-strip">
        <span><ShieldCheck size={16} /> Secure payment</span>
        <span><Truck size={16} /> Tracked delivery</span>
        <span><Check size={16} /> Carefully checked totals</span>
        <span><Sparkles size={16} /> Crafted for a warmer home</span>
      </div>

      {error && <div className="form-error checkout-error">{error}</div>}

      <div className="checkout-layout">
        <div className="checkout-main">
          <section className="checkout-card checkout-section-card">
            <div className="checkout-card-heading">
              <div className="checkout-section-title"><span className="checkout-section-icon"><MapPin size={17} /></span><div><p className="eyebrow">01 · DELIVERY</p><h2>Where should we send it?</h2></div></div>
              <button className="text-button" type="button" onClick={() => setShowAddressForm((value) => !value)}>
                {showAddressForm ? "Close" : "+ Add address"}
              </button>
            </div>

            {loadingAddresses ? (
              <div className="address-state">Loading your addresses…</div>
            ) : addresses.length ? (
              <div className="checkout-address-list">
                {addresses.map((address) => (
                  <label className={`checkout-address-option ${String(address.id) === String(selectedAddressId) ? "selected" : ""}`} key={address.id}>
                    <input
                      type="radio"
                      name="checkout-address"
                      value={address.id}
                      checked={String(address.id) === String(selectedAddressId)}
                      onChange={(event) => setSelectedAddressId(event.target.value)}
                    />
                    <span className="checkout-radio-mark" />
                    <span className="checkout-address-copy">
                      <strong>{address.name}</strong>
                      <small>{address.addressType || "Address"}{address.defaultAddress ? " · Default" : ""}</small>
                      <span>{address.house}, {address.street}</span>
                      <span>{address.city}, {address.state} — {address.pincode}</span>
                      <span>Phone: {address.phone}</span>
                    </span>
                  </label>
                ))}
              </div>
            ) : (
              <div className="address-state">
                <strong>No delivery address saved.</strong>
                <p>Add an address below to continue.</p>
              </div>
            )}

            {showAddressForm && (
              <form className="checkout-address-form" onSubmit={saveNewAddress}>
                <div className="checkout-form-grid">
                  <label><span>Full name</span><input name="name" value={addressForm.name} onChange={changeAddressForm} required maxLength={100} /></label>
                  <label><span>Phone</span><input name="phone" value={addressForm.phone} onChange={changeAddressForm} required inputMode="numeric" pattern="[0-9]{10}" maxLength={10} /></label>
                  <label className="address-form-wide"><span>House / flat / building</span><input name="house" value={addressForm.house} onChange={changeAddressForm} required maxLength={200} /></label>
                  <label className="address-form-wide"><span>Street / locality</span><input name="street" value={addressForm.street} onChange={changeAddressForm} required maxLength={200} /></label>
                  <label><span>City</span><input name="city" value={addressForm.city} onChange={changeAddressForm} required maxLength={100} /></label>
                  <label><span>State</span><input name="state" value={addressForm.state} onChange={changeAddressForm} required maxLength={100} /></label>
                  <label><span>PIN code</span><input name="pincode" value={addressForm.pincode} onChange={changeAddressForm} required inputMode="numeric" pattern="[0-9]{6}" maxLength={6} /></label>
                  <label><span>Address type</span><select name="addressType" value={addressForm.addressType} onChange={changeAddressForm}><option value="HOME">Home</option><option value="WORK">Work</option><option value="OTHER">Other</option></select></label>
                </div>
                <label className="checkbox-row"><input type="checkbox" name="defaultAddress" checked={addressForm.defaultAddress} onChange={changeAddressForm} /><span>Make this my default address</span></label>
                <button className="primary-button" type="submit" disabled={savingAddress}>{savingAddress ? "Saving…" : "Save address"}</button>
              </form>
            )}
          </section>

          <section className="checkout-card checkout-section-card">
            <div className="checkout-card-heading"><div className="checkout-section-title"><span className="checkout-section-icon"><CreditCard size={17} /></span><div><p className="eyebrow">02 · PAYMENT</p><h2>How would you like to pay?</h2></div></div></div>
            <div className="payment-options">
              <label className={`payment-option ${paymentMethod === "CASHFREE" ? "selected" : ""}`}>
                <input type="radio" name="payment" checked={paymentMethod === "CASHFREE"} onChange={() => setPaymentMethod("CASHFREE")} />
                <span><strong>Pay online</strong><small>UPI · Cards · Netbanking · Wallets via Cashfree</small><em className="payment-option-note">Secure Cashfree checkout</em></span>
              </label>
              <label className={`payment-option ${paymentMethod === "COD" ? "selected" : ""}`}>
                <input type="radio" name="payment" checked={paymentMethod === "COD"} onChange={() => setPaymentMethod("COD")} />
                <span><strong>Cash on delivery</strong><small>Pay when your order is delivered</small><em className="payment-option-note">Available where serviceable</em></span>
              </label>
            </div>
          </section>

          <section className="checkout-card checkout-section-card">
            <div className="checkout-card-heading"><div className="checkout-section-title"><span className="checkout-section-icon"><Tag size={17} /></span><div><p className="eyebrow">03 · OFFERS</p><h2>Make your order a little sweeter.</h2></div></div></div>
            <div className="coupon-row">
              <input value={couponInput} onChange={(event) => setCouponInput(event.target.value.toUpperCase())} placeholder="Enter coupon code" maxLength={60} />
              {couponCode ? <button type="button" className="secondary-button" onClick={removeCoupon}>Remove</button> : <button type="button" className="secondary-button" onClick={applyCoupon}>Apply</button>}
            </div>
            {couponCode && <p className={`coupon-applied ${couponMessage.includes("inapplicable") ? "coupon-inapplicable" : ""}`}>{summaryLoading ? "Checking…" : couponMessage || `Coupon ${couponCode} applied.`}</p>}
            {availableDiscounts.length > 0 && (
              <div className="available-offers">
                <div className="available-offers-head"><strong>Available offers</strong><span>Apply a live code</span></div>
                {availableDiscounts.map((offer) => {
                  const label = offer.type === "PERCENTAGE" ? `${offer.value}% off` : `${money(offer.value)} off`;
                  const min = Number(offer.minimumOrderValue || 0);
                  return (
                    <button type="button" className={`available-offer ${offer.firstTimeOnly ? "first-time-offer" : ""}`} key={offer.code} onClick={() => { setCouponInput(String(offer.code || "")); setCouponCode(String(offer.code || "")); setCouponMessage("Checking coupon…"); }}>
                      <span><strong>{offer.code}</strong><small>{label}{min > 0 ? ` · Min. ${money(min)}` : ""}{offer.firstTimeOnly ? " · First order" : ""}</small></span>
                      <em>Apply</em>
                    </button>
                  );
                })}
              </div>
            )}
          </section>

          <section className="checkout-card checkout-section-card">
            <div className="checkout-card-heading"><div className="checkout-section-title"><span className="checkout-section-icon"><Package size={17} /></span><div><p className="eyebrow">04 · YOUR ORDER</p><h2>A final look at your pieces.</h2></div></div></div>
            <div className="checkout-items">
              {items.map((item) => {
                const product = item.product || {};
                return (
                  <div className="checkout-item" key={item.id || product.id}>
                    <div className="checkout-item-image">{imageFor(product) ? <img src={imageFor(product)} alt={product.name || "Ceramic piece"} /> : <span>Cerclay</span>}</div>
                    <div><strong>{product.name || "Ceramic piece"}{Number(item.packSize || 1) === 2 ? " · Set of 2" : ""}</strong><span>Qty {item.quantity}{Number(item.packSize || 1) === 2 ? " set(s)" : ""}</span></div>
                    <strong>{money((Number(item.packSize || 1) === 2 ? Number(product.setOf2Price || 0) : Number(product.price || 0)) * Number(item.quantity || 0))}</strong>
                  </div>
                );
              })}
            </div>
          </section>
        </div>

        <aside className="checkout-summary-card">
          <div className="summary-card-topline"><span><ShieldCheck size={15} /> Secure order</span><small>{items.length} {items.length === 1 ? "piece" : "pieces"}</small></div>
          <p className="eyebrow">ORDER SUMMARY</p>
          <h2>Review & pay</h2>
          <div className="checkout-summary-lines">
            <div><span>Subtotal</span><strong>{money(summary?.subtotal)}</strong></div>
            {Number(summary?.productDiscount || 0) > 0 && <div><span>Product savings</span><strong>−{money(summary.productDiscount)}</strong></div>}
            <div><span>Coupon discount</span><strong>{Number(summary?.couponDiscount || 0) > 0 ? `−${money(summary.couponDiscount)}` : "—"}</strong></div>
            <div><span>Shipping {summary?.shippingZone ? `· Zone ${summary.shippingZone}` : ""}</span><strong>{Number(summary?.shippingCharge || 0) === 0 ? "FREE" : money(summary.shippingCharge)}</strong></div>
          </div>
          <div className="checkout-total"><span>Total</span><strong>{summaryLoading ? "Calculating…" : money(summary?.total)}</strong></div>
          <div className="summary-payment-note"><CreditCard size={14} /><span>Payments secured by Cashfree</span></div>
          <p className="checkout-security-note">Your total, availability and delivery options are checked again before your order is placed.</p>
          <button className="primary-button checkout-place-button" type="button" disabled={placing || summaryLoading || !selectedAddressId || !summary?.serviceable} onClick={placeOrder}>
            {placing ? "Processing…" : paymentMethod === "COD" ? `Place order · ${money(summary?.total)}` : `Continue to payment · ${money(summary?.total)}`}
          </button>
        </aside>
      </div>
    </section>
  );
}
