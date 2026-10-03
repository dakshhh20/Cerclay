import React, { useState } from "react";
import { Heart, Minus, Plus } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { useWishlist } from "../context/WishlistContext";

function imageFor(product) {
  return (
    (Array.isArray(product.images) ? product.images.find((image) => image?.primary)?.imageUrl : null) ||
    (Array.isArray(product.images) ? product.images[0]?.imageUrl : null) ||
    product.primaryImageUrl ||
    product.imageUrl ||
    product.image ||
    null
  );
}

export default function ProductCard({ product }) {
  const { addItem, updateItem, removeItem, cart } = useCart();
  const { customer } = useAuth();
  const { isWishlisted, toggle } = useWishlist();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [selectedPack, setSelectedPack] = useState(1);
  const [wishlistBusy, setWishlistBusy] = useState(false);
  const imageCandidates = Array.from(new Set([
    ...(Array.isArray(product.images) ? product.images.map((image) => image?.imageUrl).filter(Boolean) : []),
    product.primaryImageUrl,
    product.imageUrl,
    product.image,
  ].filter(Boolean)));
  const [imageIndex, setImageIndex] = useState(0);
  const image = imageCandidates[imageIndex] || imageFor(product);
  const name = product.name || "Ceramic piece";
  const price = product.price ?? 0;
  const mrp = product.mrp;
  const set2Price = product.setOf2Price;
  const set2Mrp = product.setOf2Mrp;
  const slug = product.id ?? product.slug;
  const cartItem = cart.items.find((item) => String(item.product?.id) === String(product.id) && Number(item.packSize || 1) === selectedPack);
  const quantity = Number(cartItem?.quantity || 0);
  const stock = product.stock == null ? null : Number(product.stock);
  const unavailable = product.active === false;
  const outOfStock = unavailable || (stock != null && stock < selectedPack);
  const atStockLimit = stock != null && quantity * selectedPack >= stock;
  const saved = isWishlisted(product.id);

  const hasSet2 = Boolean(product.setOf2Enabled && Number(set2Price) >= 0);
  const selectedPrice = selectedPack === 2 && hasSet2 ? Number(set2Price) : Number(price);
  const selectedMrp = selectedPack === 2 && hasSet2
    ? (set2Mrp != null ? Number(set2Mrp) : null)
    : (mrp != null ? Number(mrp) : null);
  const hasSelectedOffer = selectedMrp != null && selectedMrp > selectedPrice;
  const hasAnyOffer = (mrp != null && Number(mrp) > Number(price)) || (set2Mrp != null && hasSet2 && Number(set2Mrp) > Number(set2Price));

  async function setQuantity(nextQuantity) {
    if (!product.id || busy || nextQuantity < 0) return;
    setBusy(true);
    try {
      if (nextQuantity === 0) {
        await removeItem(product.id, selectedPack);
      } else if (quantity === 0) {
        await addItem(product.id, nextQuantity, selectedPack);
      } else {
        await updateItem(product.id, nextQuantity, selectedPack);
      }
    } catch (err) {
      window.alert(err.message || "Unable to update your bag.");
    } finally {
      setBusy(false);
    }
  }

  async function handleWishlist(event) {
    event.preventDefault();
    event.stopPropagation();
    if (!customer) {
      navigate("/login", { state: { from: `/products/${slug}` } });
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

  return (
    <article className="product-card">
      <div className="product-image-wrap">
        <Link to={`/products/${slug}`} className="product-image-link" aria-label={`View ${name}`}>
          {image ? <img src={image} alt={name} className="product-image" onError={() => setImageIndex((current) => current + 1)} /> : <div className="product-image-placeholder"><span>Cerclay</span></div>}
        </Link>
        <button
          type="button"
          className={`wishlist-button ${saved ? "saved" : ""}`}
          onClick={handleWishlist}
          disabled={wishlistBusy}
          aria-label={saved ? `Remove ${name} from wishlist` : `Save ${name} to wishlist`}
          aria-pressed={saved}
          title={saved ? "Remove from wishlist" : "Add to wishlist"}
        >
          <Heart size={19} strokeWidth={1.6} fill={saved ? "currentColor" : "none"} />
        </button>
        {hasAnyOffer && <span className="product-badge">Offer</span>}
      </div>

      <div className="product-card-body">
        {hasSet2 ? <div className="product-pack-pricing"><span className="pack-choice-label">Choose your pack</span>
          <button type="button" className={selectedPack === 1 ? "active" : ""} onClick={() => setSelectedPack(1)}>
            <span>Single</span>
            <strong>₹{Number(price).toLocaleString("en-IN")}</strong>
            {mrp != null && Number(mrp) > Number(price) && <del>₹{Number(mrp).toLocaleString("en-IN")}</del>}
          </button>
          <button type="button" className={selectedPack === 2 ? "active" : ""} onClick={() => setSelectedPack(2)}>
            <span>Set of 2</span>
            <strong>₹{Number(set2Price).toLocaleString("en-IN")}</strong>
            {set2Mrp != null && Number(set2Mrp) > Number(set2Price) && <del>₹{Number(set2Mrp).toLocaleString("en-IN")}</del>}
          </button>
        </div> : <div className="product-pack-pricing pack-placeholder" aria-hidden="true" />}

        <div className="product-card-info">
          <div className="product-card-copy">
            {product.category && <p className="product-category">{product.category}</p>}
            <h3><Link to={`/products/${slug}`}>{name}</Link></h3>
          </div>
          <div className="product-price">
            <span>₹{selectedPrice.toLocaleString("en-IN")}</span>
            {hasSelectedOffer && <del>₹{selectedMrp.toLocaleString("en-IN")}</del>}
          </div>
        </div>

        {quantity > 0 ? (
          <div className="product-quantity-row">
            <span className="in-bag-label">In your bag{selectedPack === 2 ? " · Set of 2" : ""}</span>
            <div className="quantity-control product-quantity-control" aria-label={`Quantity for ${name}`}>
              <button type="button" disabled={busy} onClick={() => setQuantity(quantity - 1)} aria-label={`Decrease ${name} quantity`}><Minus size={15} /></button>
              <span>{busy ? "…" : quantity}</span>
              <button type="button" disabled={busy || atStockLimit} onClick={() => setQuantity(quantity + 1)} aria-label={`Increase ${name} quantity`}><Plus size={15} /></button>
            </div>
          </div>
        ) : (
          <button
            type="button"
            className="product-card-add"
            disabled={busy || outOfStock}
            onClick={() => setQuantity(1)}
          >
            {busy ? "Adding…" : unavailable ? "Unavailable" : outOfStock ? "Out of stock" : selectedPack === 2 ? "Add set of 2" : "Add to bag"}
          </button>
        )}
      </div>
    </article>
  );
}
