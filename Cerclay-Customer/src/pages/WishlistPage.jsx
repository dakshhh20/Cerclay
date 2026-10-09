import React from "react";
import { Heart, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import { useWishlist } from "../context/WishlistContext";

export default function WishlistPage() {
  const { items, loading } = useWishlist();

  return (
    <section className="wishlist-page page-container">
      <div className="wishlist-heading">
        <div>
          <p className="eyebrow">SAVED PIECES</p>
          <h1>Your wishlist</h1>
          <p>Keep the pieces you love close until you’re ready to bring them home.</p>
        </div>
        <span className="wishlist-count">
          {items.length} {items.length === 1 ? "piece" : "pieces"}
        </span>
      </div>

      {loading ? (
        <div className="state-box wishlist-state">
          <span>Loading your saved pieces…</span>
        </div>
      ) : items.length === 0 ? (
        <div className="wishlist-empty">
          <div className="wishlist-empty-icon">
            <Heart size={25} strokeWidth={1.4} />
          </div>
          <p className="eyebrow">NOTHING SAVED YET</p>
          <h2>Find something worth keeping.</h2>
          <p>Tap the heart on any Cerclay piece and it will appear here.</p>
          <Link to="/shop" className="primary-button">
            Explore the collection <ArrowRight size={16} />
          </Link>
        </div>
      ) : (
        <div className="product-grid wishlist-grid">
          {items.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </section>
  );
}
