import React, { useEffect, useMemo, useState } from "react";
import { Heart, ChevronLeft, ChevronRight, Minus, Plus, Star, ShieldCheck, Truck, PackageCheck, CreditCard, RotateCcw, CheckCircle2 } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import {
  getProduct,
  getProducts,
  getProductReviews,
  getReviewEligibility,
  submitProductReview,
} from "../lib/api";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { useWishlist } from "../context/WishlistContext";
import ProductCard from "../components/ProductCard";

function imageFor(product) {
  if (!product) return null;
  return (
    (Array.isArray(product.images) ? product.images.find((image) => image?.primary)?.imageUrl : null) ||
    (Array.isArray(product.images) ? product.images[0]?.imageUrl : null) ||
    product.primaryImageUrl || product.imageUrl || product.image || null
  );
}

function imageList(product) {
  const images = Array.isArray(product?.images)
    ? product.images.map((image) => image?.imageUrl || image?.url).filter(Boolean)
    : [];
  const primary = imageFor(product);
  return Array.from(new Set(primary ? [primary, ...images] : images));
}

function money(value) { return `₹${Number(value ?? 0).toLocaleString("en-IN")}`; }

export default function ProductDetailPage() {
  const { identifier } = useParams();
  const [product, setProduct] = useState(null);
  const [activeIndex, setActiveIndex] = useState(0);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [selectedPack, setSelectedPack] = useState(1);
  const { addItem, updateItem, removeItem, cart } = useCart();
  const { customer } = useAuth();
  const { isWishlisted, toggle } = useWishlist();
  const [wishlistBusy, setWishlistBusy] = useState(false);
  const [relatedProducts, setRelatedProducts] = useState([]);
  const [colorVariants, setColorVariants] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [reviewEligible, setReviewEligible] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewText, setReviewText] = useState("");
  const [reviewBusy, setReviewBusy] = useState(false);
  const [reviewMessage, setReviewMessage] = useState("");

  useEffect(() => {
    let active = true;
    setStatus("loading");
    setError("");
    setProduct(null);
    getProduct(identifier).then((data) => {
      if (!active) return;
      setProduct(data);
      setActiveIndex(0);
      setStatus("ready");
    }).catch((err) => {
      if (!active) return;
      setError(err.message || "Unable to load this product.");
      setStatus("error");
    });
    return () => { active = false; };
  }, [identifier]);

  useEffect(() => {
    if (!product?.id) return undefined;
    let active = true;

    getProducts(product.category ? { category: product.category } : {})
      .then((data) => {
        if (!active) return;
        const list = Array.isArray(data) ? data : data?.content || data?.products || [];
        const currentId = String(product.id);
        const related = list.filter((item) => String(item?.id) !== currentId && (!product.colorGroup || item.colorGroup !== product.colorGroup)).slice(0, 4);
        setRelatedProducts(related);
      })
      .catch(() => {
        if (active) setRelatedProducts([]);
      });

    return () => { active = false; };
  }, [product?.id, product?.category, product?.colorGroup, customer?.id]);

  useEffect(() => {
    if (!product?.colorGroup) {
      setColorVariants([]);
      return undefined;
    }
    let active = true;
    getProducts({ colorGroup: product.colorGroup, sort: "name_asc" })
      .then((data) => {
        if (!active) return;
        const list = Array.isArray(data) ? data : [];
        setColorVariants(list.filter((item) => item?.active !== false && item?.colorGroup === product.colorGroup));
      })
      .catch(() => { if (active) setColorVariants([]); });
    return () => { active = false; };
  }, [product?.colorGroup]);

  const images = useMemo(() => imageList(product), [product]);
  const price = Number(product?.price ?? 0);
  const hasSet2 = Boolean(product?.setOf2Enabled && Number(product?.setOf2Price) >= 0);
  const mrp = product?.mrp != null ? Number(product.mrp) : null;
  const set2Price = product?.setOf2Price != null ? Number(product.setOf2Price) : null;
  const set2Mrp = product?.setOf2Mrp != null ? Number(product.setOf2Mrp) : null;
  const selectedPrice = selectedPack === 2 && hasSet2 ? set2Price : price;
  const selectedMrp = selectedPack === 2 && hasSet2 ? set2Mrp : mrp;
  const stock = product?.stock == null ? null : Number(product.stock);
  const inStock = stock == null || stock >= selectedPack;
  const discount = selectedMrp && selectedMrp > selectedPrice ? Math.round(((selectedMrp - selectedPrice) / selectedMrp) * 100) : 0;
  const cartItem = cart.items.find((item) => String(item.product?.id) === String(product?.id) && Number(item.packSize || 1) === selectedPack);
  const quantity = Number(cartItem?.quantity || 0);
  const atStockLimit = stock != null && quantity * selectedPack >= stock;
  const [failedImages, setFailedImages] = useState(new Set());
  const usableImages = images.filter((src) => !failedImages.has(src));
  const safeIndex = usableImages.length ? Math.min(activeIndex, usableImages.length - 1) : 0;
  const selectedImage = usableImages[safeIndex] || null;

  function moveImage(direction) {
    if (usableImages.length < 2) return;
    setActiveIndex((current) => (current + direction + usableImages.length) % usableImages.length);
  }

  async function setQuantity(nextQuantity) {
    if (!product?.id || busy || nextQuantity < 0) return;
    setBusy(true);
    try {
      if (nextQuantity === 0) await removeItem(product.id, selectedPack);
      else if (quantity === 0) await addItem(product.id, nextQuantity, selectedPack);
      else await updateItem(product.id, nextQuantity, selectedPack);
    } catch (err) {
      window.alert(err.message || "Unable to update your bag.");
    } finally {
      setBusy(false);
    }
  }

  async function handleWishlist() {
    if (!product?.id) return;
    if (!customer) {
      window.location.assign(`/login?from=${encodeURIComponent(`/products/${identifier}`)}`);
      return;
    }
    if (wishlistBusy) return;
    setWishlistBusy(true);
    try {
      await toggle(product.id);
    } catch (err) {
      window.alert(err.message || "Unable to update your wishlist.");
    } finally {
      setWishlistBusy(false);
    }
  }

  const saved = product ? isWishlisted(product.id) : false;

  useEffect(() => {
    if (!product?.id) return;
    getProductReviews(product.id).then((data) => setReviews(Array.isArray(data) ? data : [])).catch(() => setReviews([]));
    if (customer?.id) {
      getReviewEligibility(product.id).then((data) => setReviewEligible(Boolean(data))).catch(() => setReviewEligible(false));
    } else {
      setReviewEligible(false);
    }
  }, [product?.id, customer?.id]);

  async function submitReview(e) {
    e.preventDefault();
    if (!product?.id || reviewBusy) return;
    setReviewBusy(true); setReviewMessage("");
    try {
      await submitProductReview(product.id, { rating: Number(reviewRating), review: reviewText });
      setReviewText(""); setReviewRating(5); setReviewEligible(false);
      setReviewMessage("Thank you. Your review has been submitted for approval.");
    } catch (err) {
      setReviewMessage(err.message || "Unable to submit your review.");
    } finally { setReviewBusy(false); }
  }

  if (status === "loading") return <section className="product-detail-page"><div className="state-box">Loading product…</div></section>;
  if (status === "error" || !product) return <section className="product-detail-page"><div className="state-box error-state"><strong>We couldn't load this product.</strong><span>{error}</span><Link to="/shop" className="primary-button">Back to shop</Link></div></section>;

  return (
    <section className="product-detail-page">
      <div className="product-breadcrumb"><Link to="/shop">Shop</Link><span>/</span><span>{product.name || "Product"}</span></div>
      <div className="product-detail-layout">
        <div className="product-gallery">
          <div className="product-main-image">
            {selectedImage ? <img src={selectedImage} alt={product.name || "Cerclay product"} onError={() => setFailedImages((current) => new Set([...current, selectedImage]))} /> : <div className="product-image-placeholder"><span>Cerclay</span></div>}
            {usableImages.length > 1 && <>
              <button type="button" className="gallery-arrow gallery-arrow-left" onClick={() => moveImage(-1)} aria-label="Previous product image"><ChevronLeft size={22} /></button>
              <button type="button" className="gallery-arrow gallery-arrow-right" onClick={() => moveImage(1)} aria-label="Next product image"><ChevronRight size={22} /></button>
              <div className="gallery-counter">{safeIndex + 1} / {usableImages.length}</div>
            </>}
          </div>
          {usableImages.length > 1 && <div className="product-thumbnails">
            {usableImages.map((image, index) => <button type="button" key={`${image}-${index}`} className={`product-thumbnail ${safeIndex === index ? "selected" : ""}`} onClick={() => setActiveIndex(index)} aria-label={`View product image ${index + 1}`}><img src={image} alt="" /></button>)}
          </div>}
          {product.description && <div className="product-description product-description-left"><h2>Description</h2><div>{product.description}</div></div>}
        </div>

        <div className="product-detail-copy">
          <div className="detail-kicker"><span>{product.category || "CERCLAY COLLECTION"}</span>{discount > 0 && <em>{discount}% off</em>}</div>
          <div className="product-title-row">
            <h1>{product.name || "Ceramic piece"}</h1>
            <button
              type="button"
              className={`detail-wishlist-button ${saved ? "saved" : ""}`}
              onClick={handleWishlist}
              disabled={wishlistBusy}
              aria-label={saved ? "Remove from wishlist" : "Add to wishlist"}
              aria-pressed={saved}
              title={saved ? "Remove from wishlist" : "Add to wishlist"}
            >
              <Heart size={23} strokeWidth={1.5} fill={saved ? "currentColor" : "none"} />
            </button>
          </div>
          {colorVariants.length > 1 && (
            <div className="product-colour-picker">
              <div className="product-colour-heading"><span>Colour</span><strong>{product.colorName || "Select a colour"}</strong></div>
              <div className="product-colour-options">
                {colorVariants.map((variant) => {
                  const activeColour = String(variant.id) === String(product.id);
                  return <Link key={variant.id} to={`/products/${variant.id}`} className={`product-colour-option ${activeColour ? "selected" : ""}`} aria-label={variant.colorName || variant.name}>
                    <span className="product-colour-swatch" style={variant.colorHex ? { backgroundColor: variant.colorHex } : undefined}>{!variant.colorHex && (variant.colorName || "C").slice(0, 1).toUpperCase()}</span>
                    <span>{variant.colorName || variant.name}</span>
                  </Link>;
                })}
              </div>
            </div>
          )}
          <div className="detail-price"><span>{money(selectedPrice)}</span>{selectedMrp != null && selectedMrp > selectedPrice && <del>{money(selectedMrp)}</del>}</div>
          {hasSet2 && <div className="detail-pack-options" role="group" aria-label="Choose pack size">
            <button type="button" className={selectedPack === 1 ? "active" : ""} onClick={() => setSelectedPack(1)}><span>Single</span><strong>{money(price)}</strong></button>
            <button type="button" className={selectedPack === 2 ? "active" : ""} onClick={() => setSelectedPack(2)}>
              <span>Set of 2</span>
              <strong>{money(set2Price)}</strong>
              {set2Mrp != null && set2Mrp > set2Price && <del>{money(set2Mrp)}</del>}
              <em>Bundle price</em>
            </button>
          </div>}
          <div className={`stock-status ${inStock ? "in-stock" : "out-of-stock"}`}>{inStock ? "Available" : "Currently unavailable"}</div>

          {inStock && quantity > 0 ? (
            <div className="detail-cart-actions">
              <div className="quantity-control detail-quantity-control" aria-label={`Quantity for ${product.name}`}>
                <button type="button" disabled={busy} onClick={() => setQuantity(quantity - 1)} aria-label="Decrease quantity"><Minus size={17} /></button>
                <span>{busy ? "…" : quantity}</span>
                <button type="button" disabled={busy || atStockLimit} onClick={() => setQuantity(quantity + 1)} aria-label="Increase quantity"><Plus size={17} /></button>
              </div>
              <Link to="/cart" className="primary-button product-view-cart">View bag</Link>
            </div>
          ) : (
            <button type="button" className="primary-button product-add-button" disabled={!inStock || busy} onClick={() => setQuantity(1)}>{busy ? "Adding…" : inStock ? "Add to bag" : "Out of stock"}</button>
          )}

          <div className="product-trust-strip">
            <div><ShieldCheck size={18} strokeWidth={1.6}/><strong>100% Secure Transactions</strong><span>Protected checkout and secure payment methods</span></div>
            <div><Truck size={18} strokeWidth={1.6}/><strong>Reliable Delivery</strong><span>Shipping calculated at checkout</span></div>
            <div><PackageCheck size={18} strokeWidth={1.6}/><strong>Order Tracking</strong><span>Tracking appears after dispatch</span></div>
          </div>
          <div className="product-payment-delivery">
            <div className="payment-security-card">
              <div className="payment-card-heading"><ShieldCheck size={19} strokeWidth={1.6}/><div><strong>100% Secure Transactions</strong><span>Your payment is processed securely.</span></div></div>
              <div className="payment-methods payment-logo-methods" aria-label="Accepted payment methods">
                <img src="/assets/payment-logos/upi-reference.png" alt="UPI"/><img src="/assets/payment-logos/gpay-reference.png" alt="Google Pay"/><img src="/assets/payment-logos/phonepe.svg" alt="PhonePe"/><img src="/assets/payment-logos/rupay.svg" alt="RuPay"/><img src="/assets/payment-logos/visa.svg" alt="Visa"/><img src="/assets/payment-logos/mastercard.svg" alt="Mastercard"/><span className="generic-card-logo"><CreditCard size={15}/> Cards</span>
              </div>
              <div className="payment-note"><CreditCard size={14}/><span>UPI, cards and other secure payment methods are available at checkout.</span></div>
            </div>
            <div className="delivery-card">
              <div className="delivery-card-heading"><Truck size={19} strokeWidth={1.6}/><div><strong>Delivery &amp; tracking</strong><span>Delivery availability is confirmed at checkout.</span></div></div>
              <div className="delivery-steps">
                <div><span className="delivery-step-icon"><PackageCheck size={17}/></span><strong>Order placed</strong><small>Your order is confirmed</small></div>
                <div><span className="delivery-step-icon"><Truck size={17}/></span><strong>Dispatched</strong><small>Tracking starts after dispatch</small></div>
                <div><span className="delivery-step-icon"><PackageCheck size={17}/></span><strong>Delivered</strong><small>Tracking updates appear in your order</small></div>
              </div>
            </div>
            <div className="return-card">
              <div className="return-card-heading"><RotateCcw size={20} strokeWidth={1.6}/><div><strong>2-Day Return Window</strong><span>Request a return within 2 days of delivery.</span></div></div>
              <div className="return-points">
                <span><CheckCircle2 size={14}/>2-day window from delivery date</span>
                <span><CheckCircle2 size={14}/>Eligible for damaged, defective or wrong items</span>
                <span><CheckCircle2 size={14}/>Simple return request from your account</span>
                <span><CheckCircle2 size={14}/>Refund after the returned product is received and inspected</span>
              </div>
              <small className="return-note">Change-of-mind returns are currently not accepted.</small>
            </div>
          </div>
          <div className="product-meta">{product.sku && <div><span>SKU</span><strong>{product.sku}</strong></div>}{product.category && <div><span>Category</span><strong>{product.category}</strong></div>}</div>
        </div>
      </div>

      <section className="product-reviews-section">
        <div className="section-heading-row">
          <div><p className="eyebrow">CUSTOMER VOICES</p><h2>Reviews</h2></div>
          <div className="review-summary">
            <span className="review-stars" aria-label={`${product.rating || 0} out of 5 stars`}>{[1,2,3,4,5].map(n => <Star key={n} size={17} fill={Number(product.rating || 0) >= n ? "currentColor" : "none"} />)}</span>
            <strong>{product.rating ? Number(product.rating).toFixed(1) : "No rating yet"}</strong>
            <span>{Number(product.reviews || reviews.length || 0)} review{Number(product.reviews || reviews.length || 0) === 1 ? "" : "s"}</span>
          </div>
        </div>
        {customer && reviewEligible && <form className="review-form" onSubmit={submitReview}>
          <div><strong>Share your experience</strong><div className="review-rating-picker" aria-label="Choose rating">{[1,2,3,4,5].map(n => <button key={n} type="button" className={n <= reviewRating ? "active" : ""} onClick={() => setReviewRating(n)} aria-label={`${n} star${n === 1 ? "" : "s"}`}><Star size={20} fill={n <= reviewRating ? "currentColor" : "none"}/></button>)}</div></div>
          <textarea value={reviewText} onChange={e => setReviewText(e.target.value)} maxLength={2000} required placeholder="Tell other customers about your experience…" />
          <div className="review-form-actions"><span>{reviewMessage}</span><button className="primary-button" disabled={reviewBusy || !reviewText.trim()}>{reviewBusy ? "Submitting…" : "Submit review"}</button></div>
        </form>}
        {!customer && reviews.length === 0 && <p className="review-empty">Sign in after a delivered purchase to leave a review.</p>}
        {customer && !reviewEligible && reviews.length === 0 && <p className="review-empty">Reviews from verified purchases appear here after approval.</p>}
        {reviews.length > 0 ? <div className="review-list">{reviews.map(r => <article className="review-card" key={r.id}>
          <div className="review-card-head"><div><strong>{r.customerName || "Customer"}</strong>{r.verifiedPurchase && <span className="verified-review">Verified purchase</span>}</div><time>{r.createdAt ? new Date(r.createdAt).toLocaleDateString("en-IN", {day:"2-digit", month:"short", year:"numeric"}) : ""}</time></div>
          <div className="review-card-stars">{[1,2,3,4,5].map(n => <Star key={n} size={16} fill={n <= Number(r.rating || 0) ? "currentColor" : "none"}/>)}</div>
          <p>{r.review}</p>
        </article>)}</div> : <p className="review-empty">No approved reviews yet. Be the first verified customer to share your experience.</p>}
      </section>

      {relatedProducts.length > 0 && (
        <section className="product-related-section">
          <div className="section-heading-row">
            <div>
              <p className="eyebrow">FROM THE SAME COLLECTION</p>
              <h2>You may also like</h2>
            </div>
            <Link className="text-link" to={`/shop?category=${encodeURIComponent(product.category || "")}`}>Explore more <ChevronRight size={16} /></Link>
          </div>
          <div className="product-grid">
            {relatedProducts.map((item) => <ProductCard key={item.id} product={item} />)}
          </div>
        </section>
      )}
    </section>
  );
}
