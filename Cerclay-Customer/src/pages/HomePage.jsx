import React, { useEffect, useState } from "react";
import { ArrowDownRight, ArrowRight, ChevronLeft, ChevronRight, Pause, Play, Star } from "lucide-react";
import { Link } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import {
  getProducts,
  getFeaturedReviews,
  getAvailableDiscounts,
} from "../lib/api";

function money(value) {
  return `₹${Number(value ?? 0).toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

export default function HomePage() {
  const [featured, setFeatured] = useState([]);
  const [homeSections, setHomeSections] = useState([]);
  const [allProducts, setAllProducts] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [offers, setOffers] = useState([]);
  const [heroProductIndex, setHeroProductIndex] = useState(0);
  const [heroPaused, setHeroPaused] = useState(false);
  const [categoryIndex, setCategoryIndex] = useState(0);
  const [categoryPaused, setCategoryPaused] = useState(false);
  const [reviewIndex, setReviewIndex] = useState(0);
  const [reviewPaused, setReviewPaused] = useState(false);

  useEffect(() => {
    let active = true;
    getProducts({ sort: "featured" })
      .then((data) => {
        if (!active) return;
        const list = Array.isArray(data) ? data : data.content || data.products || [];
        setAllProducts(list);
        const featuredProducts = list.filter((product) => product?.featured);
        const ordered = [...featuredProducts, ...list.filter((product) => !product?.featured)];
        const unique = Array.from(new Map(ordered.map((product) => [product?.id ?? product?.slug ?? product?.name, product])).values());
        setFeatured(unique.slice(0, 8));
        const grouped = new Map();
        unique.forEach((product) => {
          const category = String(product?.category || "").trim();
          if (!category) return;
          if (!grouped.has(category)) grouped.set(category, []);
          grouped.get(category).push(product);
        });
        const sections = [...grouped.entries()]
          .sort((a, b) => b[1].length - a[1].length || a[0].localeCompare(b[0]))
          .slice(0, 3)
          .map(([category, products]) => ({ category, products: products.slice(0, 4) }));
        setHomeSections(sections);
      })
      .catch(() => {
        if (active) setFeatured([]);
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    getFeaturedReviews()
      .then((data) => setReviews(Array.isArray(data) ? data.filter(Boolean) : []))
      .catch(() => setReviews([]));
    getAvailableDiscounts().then((data) => setOffers(Array.isArray(data) ? data : [])).catch(() => setOffers([]));
  }, []);

  const heroProducts = allProducts.filter(Boolean).slice(0, 6);
  const categories = Array.from(new Set(allProducts.map((product) => String(product?.category || "").trim()).filter(Boolean)))
    .sort((a, b) => a.localeCompare(b));
  const activeCategory = categories[categoryIndex] || categories[0] || "";
  const categoryProducts = activeCategory
    ? allProducts.filter((product) => String(product?.category || "").trim().toLowerCase() === activeCategory.toLowerCase())
    : [];

  useEffect(() => {
    if (heroPaused || heroProducts.length < 2) return undefined;
    const timer = window.setInterval(() => {
      setHeroProductIndex((current) => (current + 1) % heroProducts.length);
    }, 3600);
    return () => window.clearInterval(timer);
  }, [heroPaused, heroProducts.length]);

  useEffect(() => {
    if (heroProductIndex >= heroProducts.length && heroProducts.length) setHeroProductIndex(0);
  }, [heroProductIndex, heroProducts.length]);

  useEffect(() => {
    if (categoryIndex >= categories.length && categories.length) setCategoryIndex(0);
  }, [categoryIndex, categories.length]);

  useEffect(() => {
    if (categoryPaused || categories.length < 2) return undefined;
    const timer = window.setInterval(() => {
      setCategoryIndex((current) => (current + 1) % categories.length);
    }, 5200);
    return () => window.clearInterval(timer);
  }, [categoryPaused, categories.length]);

  const previewReviews = [
    { id: "preview-1", rating: 5, customerName: "Ananya Kapoor", review: "The finish feels lovely in person and the little details make it feel special on the shelf." },
    { id: "preview-2", rating: 5, customerName: "Rohan Mehta", review: "A simple piece, but it has such a warm presence. It fits beautifully into our everyday setup." },
    { id: "preview-3", rating: 4, customerName: "Meera Iyer", review: "The shape is thoughtful and the piece feels both useful and decorative without trying too hard." },
    { id: "preview-4", rating: 5, customerName: "Kavya Sharma", review: "Really liked the finish and proportions. It looks just as considered in the room as it did online." },
    { id: "preview-5", rating: 5, customerName: "Arjun Malhotra", review: "A lovely everyday addition. The kind of ceramic piece you naturally keep reaching for." },
  ];
  const usingPreviewReviews = reviews.length === 0;
  const displayReviews = usingPreviewReviews ? previewReviews : reviews;

  useEffect(() => {
    if (reviewPaused || displayReviews.length < 2) return undefined;
    const timer = window.setInterval(() => setReviewIndex((current) => (current + 1) % displayReviews.length), 4200);
    return () => window.clearInterval(timer);
  }, [reviewPaused, displayReviews.length]);

  const visibleReviews = Array.from({ length: Math.min(3, displayReviews.length) }, (_, offset) => displayReviews[(reviewIndex + offset) % displayReviews.length]);
  const reviewAverage = reviews.length
    ? (reviews.reduce((sum, review) => sum + Number(review.rating || 0), 0) / reviews.length).toFixed(1)
    : null;

  const activeHeroProduct = heroProducts[heroProductIndex];

  function heroImage(product) {
    return product?.images?.find((image) => image.primary)?.imageUrl || product?.images?.[0]?.imageUrl || product?.primaryImageUrl || product?.imageUrl || product?.image || null;
  }

  return (
    <div className="home-page">
      <section className="hero hero-brand hero-centered">
        <div className="hero-copy hero-brand-copy hero-centered-copy">
          <div className="hero-emblem-wrap">
            <img src="/assets/cerclay-emblem.jpg" alt="Cerclay emblem" className="hero-emblem" />
          </div>
          <p className="eyebrow">CERAMICS FOR EVERYDAY RITUALS</p>
          <h1>Crafted for a warmer home.</h1>
          <p className="hero-description">
            Thoughtful ceramic pieces for coffee, meals, shelves and the small rituals that make a home feel like yours.
          </p>
          <div className="hero-actions">
            <Link className="primary-button" to="/shop">Explore the collection <ArrowRight size={16} /></Link>
            <a className="text-link" href="#about">Discover Cerclay <ArrowDownRight size={16} /></a>
          </div>
          <div className="hero-trust-row" aria-label="Cerclay qualities">
            <span><b>01</b> Thoughtful forms</span>
            <span><b>02</b> Everyday rituals</span>
            <span><b>03</b> Considered slowly</span>
          </div>
        </div>
      </section>

      {categories.length > 0 && (
        <section
          className="category-motion-section featured-section"
          onMouseEnter={() => setCategoryPaused(true)}
          onMouseLeave={() => setCategoryPaused(false)}
        >
          <div className="category-motion-heading">
            <div>
              <p className="eyebrow">MOVING THROUGH THE COLLECTION</p>
              <h2>Made for every corner of the ritual.</h2>
              <p>Explore the collection by the forms you love. From quiet tableware to pieces that bring a little character to your space, there is always something new to discover.</p>
            </div>
            <Link className="text-link" to={`/shop?category=${encodeURIComponent(activeCategory)}`}>Explore {activeCategory} <ArrowRight size={16} /></Link>
          </div>
          <div className="category-motion-nav" role="tablist" aria-label="Product categories">
            {categories.map((category, index) => (
              <button
                type="button"
                key={category}
                role="tab"
                aria-selected={index === categoryIndex}
                className={index === categoryIndex ? "active" : ""}
                onClick={() => setCategoryIndex(index)}
              >
                <span>{category}</span><small>{allProducts.filter((product) => String(product?.category || "").trim().toLowerCase() === category.toLowerCase()).length}</small>
              </button>
            ))}
          </div>
          <div className="category-motion-window">
            <div className="category-motion-track" key={activeCategory}>
              {[...categoryProducts, ...categoryProducts].slice(0, Math.max(6, Math.min(categoryProducts.length * 2, 12))).map((product, index) => (
                <Link className="category-motion-card" to={product?.id ? `/products/${product.id}` : "/shop"} key={`${product?.id ?? product?.name}-${index}`}>
                  <div className="category-motion-image">
                    {heroImage(product) ? <img src={heroImage(product)} alt={product?.name || activeCategory} /> : <span>CERCLAY</span>}
                    <span className="category-motion-index">{String((index % Math.max(categoryProducts.length, 1)) + 1).padStart(2, "0")}</span>
                  </div>
                  <div className="category-motion-info">
                    <div><small>{activeCategory}</small><strong>{product?.name || "Cerclay piece"}</strong></div>
                    <div className="category-motion-price">
                      <strong>{money(product?.price)}</strong>
                      {product?.setOf2Enabled && Number(product?.setOf2Price) >= 0 && <small>Set of 2 · {money(product?.setOf2Price)}</small>}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>
          <div className="category-motion-footer">
            <span>A considered edit for everyday living.</span>
            <span>{categoryIndex + 1} / {categories.length}</span>
          </div>
        </section>
      )}

      <div className="home-marquee" aria-hidden="true">
        <div className="home-marquee-track">
          <span>CLAY • FORM • WARMTH</span><span>CLAY • FORM • WARMTH</span><span>CLAY • FORM • WARMTH</span><span>CLAY • FORM • WARMTH</span>
        </div>
      </div>

      {offers.length > 0 && (
        <section className="offers-section featured-section">
          <div className="section-heading-row">
            <div><p className="eyebrow">CURRENT OFFERS</p><h2>Save on your next order.</h2></div>
            <Link className="text-link" to="/checkout">Apply at checkout <ArrowRight size={16} /></Link>
          </div>
          <div className="offers-grid">
            {offers.slice(0, 4).map((offer) => {
              const label = offer.type === "PERCENTAGE" ? `${offer.value}% off` : `${money(offer.value)} off`;
              const min = Number(offer.minimumOrderValue || 0);
              return <article className="offer-card" key={offer.code}><span className="offer-code">{offer.code}</span><h3>{label}</h3><p>{offer.firstTimeOnly ? "For first-time customers" : "Available on eligible orders"}{min > 0 ? ` · Minimum order ${money(min)}` : ""}</p><Link to="/checkout" className="secondary-button">Use offer</Link></article>;
            })}
          </div>
        </section>
      )}

      <section className="featured-section">
        <div className="section-heading-row">
          <div>
            <h2>Everyday pieces, quietly distinctive.</h2>
          </div>
          <Link className="text-link" to="/shop">View all <ArrowRight size={16} /></Link>
        </div>

        {featured.length > 0 ? (
          <div className="product-grid home-product-grid">
            {featured.map((product) => (
              <ProductCard key={product.id ?? product.slug ?? product.name} product={product} />
            ))}
          </div>
        ) : (
          <div className="collection-placeholder">
            <span>No pieces are showing yet.</span>
            <Link to="/shop">Browse the collection</Link>
          </div>
        )}
      </section>

      {homeSections.map((section) => (
        <section className="featured-section home-category-section" key={section.category}>
          <div className="section-heading-row">
            <div><p className="eyebrow">SHOP BY CATEGORY</p><h2>{section.category}</h2></div>
            <Link className="text-link" to={`/shop?category=${encodeURIComponent(section.category)}`}>View all <ArrowRight size={16} /></Link>
          </div>
          <div className="product-grid home-product-grid">
            {section.products.map((product) => <ProductCard key={product.id ?? product.slug ?? product.name} product={product} />)}
          </div>
        </section>
      ))}

      <section className="customer-voices-section" onMouseEnter={() => setReviewPaused(true)} onMouseLeave={() => setReviewPaused(false)}>
        <div className="reviews-editorial-header">
          <div>
            <p className="eyebrow">FROM OUR HANDS TO YOUR HOME</p>
            <h2>Made to be loved. <span>♥</span></h2>
            <small className="review-source-note">{usingPreviewReviews ? "A small preview of how customer stories will appear." : "Words shared by Cerclay customers."}</small>
          </div>
          {reviews.length > 0 && <div className="review-score"><strong>{reviewAverage}</strong><span>/ 5</span><div className="review-score-stars">{[1,2,3,4,5].map(n => <Star key={n} size={17} fill="currentColor" />)}</div><small>based on {reviews.length} customer reviews</small></div>}
        </div>
        <div className="customer-voices-stage">
          <button type="button" className="review-carousel-arrow review-carousel-prev" onClick={() => setReviewIndex((current) => (current - 1 + displayReviews.length) % displayReviews.length)} aria-label="Previous reviews"><ChevronLeft size={20}/></button>
          <div className="customer-voices-grid">
            {visibleReviews.map((review, index) => <article className="customer-voice-card" key={`${review.id}-${index}`}>
              <div className="review-card-stars">{[1,2,3,4,5].map(n => <Star key={n} size={16} fill={n <= Number(review.rating || 0) ? "currentColor" : "none"}/>)}</div>
              <p>“{review.review}”</p>
              <div><strong>{review.customerName || "Customer"}</strong><span>{usingPreviewReviews ? "Preview story" : (review.verifiedPurchase ? "Verified purchase" : "Customer review")}</span></div>
            </article>)}
          </div>
          <button type="button" className="review-carousel-arrow review-carousel-next" onClick={() => setReviewIndex((current) => (current + 1) % displayReviews.length)} aria-label="Next reviews"><ChevronRight size={20}/></button>
        </div>
        <div className="review-carousel-dots">{displayReviews.slice(0, Math.min(displayReviews.length, 8)).map((_, index) => <button key={index} type="button" className={index === reviewIndex % Math.min(displayReviews.length, 8) ? "active" : ""} onClick={() => setReviewIndex(index)} aria-label={`Show review ${index + 1}`}/>)}</div>
        {usingPreviewReviews && <p className="review-preview-disclaimer">Preview stories are illustrative only. Approved customer reviews automatically replace them here.</p>}
      </section>

      <section className="brand-story-band">
        <div className="brand-story-image">
          <img src="/assets/cerclay-emblem.jpg" alt="Cerclay emblem" />
        </div>
        <div className="brand-story-copy">
          <p className="eyebrow">A WARMER WAY TO LIVE WITH OBJECTS</p>
          <h2>Made to be used, not just admired.</h2>
          <p>From the morning cup you reach for without thinking to the bowl that becomes part of dinner every night — Cerclay is about making everyday objects feel considered.</p>
          <Link className="secondary-button" to="/shop">Shop Cerclay</Link>
        </div>
      </section>

      <section className="intro-section editorial-intro" id="about">
        <div className="section-number">01</div>
        <div>
          <p className="eyebrow">THE CERCLAY APPROACH</p>
          <h2>Simple forms. Warm materials. Pieces you keep reaching for.</h2>
        </div>
        <p>
          We believe the everyday deserves beautiful things — ceramics shaped with warmth, balance and purpose, made to settle naturally into the rhythms of home.
        </p>
      </section>

      <section className="home-cta editorial-cta">
        <div>
          <p className="eyebrow">START EXPLORING</p>
          <h2>Find a piece for your everyday.</h2>
        </div>
        <Link className="secondary-button" to="/shop">Shop all <ArrowRight size={16} /></Link>
      </section>
    </div>
  );
}
