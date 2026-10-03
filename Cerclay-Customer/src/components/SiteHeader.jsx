import React, { useEffect, useRef, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { ArrowRight, Menu, Search, ShoppingBag, UserRound, Heart, X } from "lucide-react";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { getProducts, getStoreSettings } from "../lib/api";
import { useWishlist } from "../context/WishlistContext";

export default function SiteHeader() {
  const { itemCount } = useCart();
  const { customer } = useAuth();
  const { count: wishlistCount } = useWishlist();
  const location = useLocation();
  const navigate = useNavigate();
  const inputRef = useRef(null);
  const [searchOpen, setSearchOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [products, setProducts] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [freeShippingThreshold, setFreeShippingThreshold] = useState(null);
  const [whatsappNumber, setWhatsappNumber] = useState("");
  const [headerCategories, setHeaderCategories] = useState([]);

  useEffect(() => {
    const current = new URLSearchParams(location.search).get("q") || "";
    setQuery(current);
  }, [location.search]);

  useEffect(() => {
    let active = true;
    getStoreSettings()
      .then((settings) => {
        if (!active) return;
        const threshold = Number(settings?.freeShippingThreshold);
        setFreeShippingThreshold(Number.isFinite(threshold) && threshold > 0 ? threshold : null);
        setWhatsappNumber(String(settings?.whatsappNumber || "").replace(/\D/g, ""));
      })
      .catch(() => {
        if (active) {
          setFreeShippingThreshold(null);
          setWhatsappNumber("");
        }
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!searchOpen) return;
    window.setTimeout(() => inputRef.current?.focus(), 0);

    if (products.length || searchLoading) return;
    setSearchLoading(true);
    getProducts()
      .then((data) => {
        const list = Array.isArray(data) ? data : data?.content || data?.products || [];
        setProducts(list);
      })
      .catch(() => setProducts([]))
      .finally(() => setSearchLoading(false));
  }, [searchOpen, products.length, searchLoading]);

  useEffect(() => {
    let active = true;
    getProducts().then((data) => {
      if (!active) return;
      const list = Array.isArray(data) ? data : data?.content || data?.products || [];
      const cats = Array.from(new Set(list.map((p) => String(p?.category || "").trim()).filter(Boolean))).sort((a,b) => a.localeCompare(b));
      setHeaderCategories(cats);
    }).catch(() => { if (active) setHeaderCategories([]); });
    return () => { active = false; };
  }, []);

  const currentCategory = React.useMemo(() => new URLSearchParams(location.search).get("category") || "", [location.search]);

  const searchSuggestions = React.useMemo(() => {
    const value = query.trim().toLowerCase();
    if (!value) return [];
    return products
      .filter((product) => {
        const haystack = [product.name, product.category, product.description, product.sku]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();
        return haystack.includes(value);
      })
      .slice(0, 6);
  }, [products, query]);

  function suggestionImage(product) {
    return (
      product.primaryImageUrl ||
      product.imageUrl ||
      product.image ||
      product.images?.find((image) => image.primary)?.imageUrl ||
      product.images?.[0]?.imageUrl ||
      null
    );
  }

  function openSuggestion(product) {
    const identifier = product.slug || product.id;
    setSearchOpen(false);
    setMenuOpen(false);
    navigate(`/products/${identifier}`);
  }

  function submitSearch(event) {
    event.preventDefault();
    const value = query.trim();
    setSearchOpen(false);
    setMenuOpen(false);
    navigate(value ? `/shop?q=${encodeURIComponent(value)}` : "/shop");
  }

  return (
    <>
      <div className="announcement-bar" aria-label="Store promotion">
        <div className="announcement-track" aria-hidden="true">
          {Array.from({ length: 2 }).map((_, index) => (
            <React.Fragment key={index}>
              <span className="announcement-highlight">
                {freeShippingThreshold != null
                  ? `FREE DELIVERY ON ORDERS ABOVE ₹${freeShippingThreshold.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`
                  : "FREE DELIVERY — SEE DELIVERY TERMS AT CHECKOUT"}
              </span><b>•</b>
              <span>CRAFTED FOR A WARMER HOME</span><b>•</b>
              <span>EVERYDAY CERAMICS, CONSIDERED SLOWLY</span><b>•</b>
            </React.Fragment>
          ))}
        </div>
      </div>

      <header className="site-header">
        <div className="header-inner">
          <button className="mobile-menu-button icon-button" type="button" aria-label={menuOpen ? "Close menu" : "Open menu"} onClick={() => setMenuOpen((open) => !open)}>
            {menuOpen ? <X size={21} /> : <Menu size={21} />}
          </button>

          <Link className="brand" to="/" onClick={() => setMenuOpen(false)} aria-label="Cerclay home">
            <img src="/assets/cerclay-wordmark.jpg" alt="Cerclay — Crafted for a warmer home" />
          </Link>

          <nav className={`desktop-nav ${menuOpen ? "mobile-open" : ""}`} aria-label="Main navigation">
            <NavLink to="/" end onClick={() => setMenuOpen(false)}>Home</NavLink>
            <NavLink to="/shop" onClick={() => setMenuOpen(false)}>Collection</NavLink>
            <a href="/#about" onClick={() => setMenuOpen(false)}>Our Story</a>
            <NavLink to={customer ? "/account" : "/login"} onClick={() => setMenuOpen(false)}>Account</NavLink>
          </nav>

          <div className="header-actions">
            <button className="icon-button" aria-label="Search products" onClick={() => setSearchOpen((open) => !open)}>
              {searchOpen ? <X size={20} strokeWidth={1.7} /> : <Search size={20} strokeWidth={1.7} />}
            </button>
            <Link to={customer ? "/account/wishlist" : "/login"} className="icon-button cart-button" aria-label={customer ? `Wishlist, ${wishlistCount} saved` : "Sign in to view wishlist"}>
              <Heart size={20} strokeWidth={1.7} />
              {customer && wishlistCount > 0 && <span className="cart-count wishlist-count-badge">{wishlistCount > 99 ? "99+" : wishlistCount}</span>}
            </Link>
            <Link to={customer ? "/account" : "/login"} className="icon-button" aria-label={customer ? "Account" : "Sign in"}>
              <UserRound size={20} strokeWidth={1.7} />
            </Link>
            <Link to="/cart" className="icon-button cart-button" aria-label={`Cart, ${itemCount} items`}>
              <ShoppingBag size={20} strokeWidth={1.7} />
              {itemCount > 0 && <span className="cart-count">{itemCount > 99 ? "99+" : itemCount}</span>}
            </Link>
          </div>
        </div>

        {searchOpen && (
          <div className="header-search-panel">
            <div className="header-search-shell">
              <form onSubmit={submitSearch} className="header-search-form">
                <Search size={18} aria-hidden="true" />
                <input
                  ref={inputRef}
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Search mugs, bowls, plates…"
                  aria-label="Search products"
                  aria-autocomplete="list"
                  aria-controls="cerclay-search-suggestions"
                />
                <button type="submit" className="primary-button">Search collection</button>
              </form>

              {query.trim() && (
                <div className="search-suggestions" id="cerclay-search-suggestions" role="listbox">
                  {searchLoading ? (
                    <div className="search-suggestion-state">Finding pieces from the collection…</div>
                  ) : searchSuggestions.length > 0 ? (
                    <>
                      <div className="search-suggestions-heading">
                        <span>Suggested pieces</span>
                        <span>{searchSuggestions.length} match{searchSuggestions.length === 1 ? "" : "es"}</span>
                      </div>
                      {searchSuggestions.map((product) => {
                        const image = suggestionImage(product);
                        return (
                          <button
                            key={product.id ?? product.slug ?? product.name}
                            type="button"
                            className="search-suggestion"
                            role="option"
                            onClick={() => openSuggestion(product)}
                          >
                            <span className="search-suggestion-image">
                              {image ? <img src={image} alt="" /> : <span>C</span>}
                            </span>
                            <span className="search-suggestion-copy">
                              <strong>{product.name || "Ceramic piece"}</strong>
                              <small>{product.category || "Cerclay collection"}</small>
                            </span>
                            <span className="search-suggestion-price">
                              ₹{Number(product.price ?? 0).toLocaleString("en-IN")}
                            </span>
                            <ArrowRight size={15} aria-hidden="true" />
                          </button>
                        );
                      })}
                      <button type="button" className="search-view-all" onClick={submitSearch}>
                        View all results for “{query.trim()}” <ArrowRight size={15} />
                      </button>
                    </>
                  ) : (
                    <div className="search-suggestion-state">
                      <strong>No matching pieces yet.</strong>
                      <span>Try “mug”, “bowl”, or another collection name.</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </header>

      <nav className={`category-nav ${menuOpen ? "category-nav-mobile-open" : ""}`} aria-label="Shop categories">
        <div className="category-nav-inner">
          <Link
            to="/shop"
            className={`category-nav-link${location.pathname === "/shop" && !currentCategory ? " active" : ""}`}
            onClick={() => setMenuOpen(false)}
          >
            Shop All
          </Link>
          {headerCategories.map((category) => (
            <Link
              key={category}
              to={`/shop?category=${encodeURIComponent(category)}`}
              className={`category-nav-link${location.pathname === "/shop" && currentCategory.toLowerCase() === category.toLowerCase() ? " active" : ""}`}
              onClick={() => setMenuOpen(false)}
            >
              {category}
            </Link>
          ))}
        </div>
      </nav>

      {searchOpen && <div
        className="search-open-spacer"
        aria-hidden="true"
        style={{ height: `${query.trim() ? Math.min(520, 112 + (searchLoading ? 64 : (searchSuggestions.length > 0 ? 94 + Math.min(searchSuggestions.length, 6) * 68 : 92))) : 104}px` }}
      />}

      {whatsappNumber && (
        <a
          className="whatsapp-contact-button"
          href={`https://wa.me/${whatsappNumber}?text=${encodeURIComponent("Hello Cerclay, I need help with my order.")}`}
          target="_blank"
          rel="noreferrer"
          aria-label="Chat with Cerclay on WhatsApp"
        >
          <svg className="whatsapp-contact-icon" viewBox="0 0 24 24" role="img" aria-hidden="true">
            <path d="M20.52 3.48A11.82 11.82 0 0 0 12.08 0C5.53 0 .2 5.33.2 11.88c0 2.09.55 4.13 1.6 5.92L.1 24l6.35-1.66a11.86 11.86 0 0 0 5.63 1.43h.01c6.55 0 11.88-5.33 11.88-11.88 0-3.17-1.23-6.14-3.45-8.41Zm-8.44 18.27h-.01a9.87 9.87 0 0 1-5.03-1.38l-.36-.21-3.77.99 1.01-3.67-.23-.38a9.85 9.85 0 0 1-1.51-5.22C2.18 6.43 6.61 2 12.08 2c2.65 0 5.14 1.03 7.01 2.91a9.85 9.85 0 0 1 2.9 7.01c0 5.47-4.45 9.83-9.91 9.83Zm5.4-7.39c-.3-.15-1.77-.87-2.05-.97-.27-.1-.47-.15-.67.15-.2.3-.77.97-.94 1.17-.17.2-.35.22-.65.07-.3-.15-1.27-.47-2.42-1.5-.9-.8-1.51-1.78-1.69-2.08-.17-.3-.02-.46.13-.61.14-.14.3-.35.45-.52.15-.17.2-.3.3-.5.1-.2.05-.37-.02-.52-.07-.15-.67-1.62-.92-2.22-.24-.58-.49-.5-.67-.51h-.57c-.2 0-.52.07-.79.37-.27.3-1.04 1.02-1.04 2.49s1.07 2.89 1.22 3.09c.15.2 2.1 3.21 5.09 4.5.71.31 1.27.49 1.71.63.72.23 1.38.2 1.9.12.58-.09 1.77-.72 2.02-1.42.25-.7.25-1.3.17-1.42-.07-.12-.27-.2-.57-.35Z" fill="currentColor"/>
          </svg>
          <span>Chat on WhatsApp</span>
        </a>
      )}
    </>
  );
}
