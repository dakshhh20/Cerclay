import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Star, UserRound } from "lucide-react";
import { getMyReviews } from "../lib/api";
import { useAuth } from "../context/AuthContext";

function formatDate(value) {
  if (!value) return "—";
  return new Date(value).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

function Stars({ value }) {
  return (
    <span className="account-review-stars" aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((star) => <Star key={star} size={15} fill={star <= Number(value) ? "currentColor" : "none"} />)}
    </span>
  );
}

function statusLabel(status) {
  const normalized = String(status || "PENDING").toUpperCase();
  if (normalized === "APPROVED") return "Published";
  if (normalized === "REJECTED") return "Rejected";
  return "Pending review";
}

export default function ReviewsPage() {
  const { status: authStatus } = useAuth();
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (authStatus === "loading") return;
    getMyReviews()
      .then(setReviews)
      .catch((err) => setError(err.message || "Unable to load your reviews."))
      .finally(() => setLoading(false));
  }, [authStatus]);

  if (authStatus === "loading" || loading) return <section className="page-container state-page"><p>Loading your reviews…</p></section>;
  if (error) return <section className="page-container state-page"><div className="state-box error-state"><strong>Unable to load reviews</strong><span>{error}</span><button className="secondary-button" onClick={() => window.location.reload()}>Try again</button></div></section>;

  return (
    <section className="account-reviews-page page-container">
      <div className="reviews-page-heading">
        <div>
          <p className="eyebrow">MY ACCOUNT</p>
          <h1>My reviews</h1>
          <p>Keep track of the reviews you have shared with Cerclay.</p>
        </div>
        <Link className="secondary-button" to="/account">Back to account</Link>
      </div>

      {reviews.length === 0 ? (
        <div className="state-box account-reviews-empty">
          <div className="account-review-empty-icon"><UserRound size={23} /></div>
          <strong>No reviews yet.</strong>
          <span>After a delivered purchase, you can review the product from its product page.</span>
          <Link className="primary-button" to="/shop">Browse products</Link>
        </div>
      ) : (
        <div className="account-reviews-list">
          {reviews.map((review) => (
            <article className="account-review-card" key={review.id}>
              <div className="account-review-card-top">
                <div>
                  <p className="eyebrow">{review.verifiedPurchase ? "VERIFIED PURCHASE" : "REVIEW"}</p>
                  <h2>{review.productName || "Product"}</h2>
                  <span>{formatDate(review.createdAt)}</span>
                </div>
                <span className={`account-review-status status-${String(review.status || "PENDING").toLowerCase()}`}>{statusLabel(review.status)}</span>
              </div>
              <div className="account-review-rating"><Stars value={review.rating} /><strong>{review.rating}/5</strong></div>
              <p className="account-review-text">{review.review}</p>
              {review.status === "REJECTED" && <p className="account-review-note">This review is not currently published. You can contact Cerclay support if you need help.</p>}
              <div className="account-review-actions">
                <Link className="text-link" to={`/products/${review.productId}`}>View product →</Link>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
