import React from "react";
import { useEffect, useMemo, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import { Grid2X2, List, SlidersHorizontal, X } from "lucide-react";
import { getProducts } from "../lib/api";

const SORT_OPTIONS = [
  ["featured", "Featured"],
  ["newest", "Newest"],
  ["price_asc", "Price: low to high"],
  ["price_desc", "Price: high to low"],
  ["name_asc", "Name: A–Z"],
];

function money(value) {
  return `₹${Number(value || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
}

export default function CataloguePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const search = (searchParams.get("q") || "").trim();

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [location.pathname, location.search]);
  const [products, setProducts] = useState([]);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");
  const [category, setCategory] = useState(searchParams.get("category") || "");
  const [priceMax, setPriceMax] = useState(searchParams.get("maxPrice") || "");
  const [inStock, setInStock] = useState(searchParams.get("inStock") === "true");
  const [sort, setSort] = useState(searchParams.get("sort") || "featured");
  const [viewMode, setViewMode] = useState(() => window.localStorage.getItem("cerclay-catalogue-view") || "grid");

  useEffect(() => {
    let active = true;
    setStatus("loading");
    setError("");

    getProducts({ sort: "featured" })
      .then((data) => {
        if (!active) return;
        const list = Array.isArray(data) ? data : data?.content || data?.products || [];
        setProducts(list);
        setStatus("ready");
      })
      .catch((err) => {
        if (!active) return;
        setError(err.message || "Unable to load the collection.");
        setStatus("error");
      });

    return () => { active = false; };
  }, []);

  const urlCategory = searchParams.get("category") || "";

  useEffect(() => {
    setCategory(urlCategory);
  }, [urlCategory]);

  useEffect(() => {
    const next = new URLSearchParams(searchParams);
    category ? next.set("category", category) : next.delete("category");
    priceMax ? next.set("maxPrice", priceMax) : next.delete("maxPrice");
    inStock ? next.set("inStock", "true") : next.delete("inStock");
    sort !== "featured" ? next.set("sort", sort) : next.delete("sort");
    setSearchParams(next, { replace: true });
  }, [category, priceMax, inStock, sort]);

  const categories = useMemo(() => {
    return [...new Set(products.map((product) => String(product.category || "").trim()).filter(Boolean))]
      .sort((a, b) => a.localeCompare(b));
  }, [products]);

  const visibleProducts = useMemo(() => {
    const normalizedSearch = search.toLowerCase();
    const max = priceMax === "" ? null : Number(priceMax);

    const filtered = products.filter((product) => {
      const haystack = [product.name, product.category, product.description, product.sku]
        .filter(Boolean).join(" ").toLowerCase();
      if (normalizedSearch && !haystack.includes(normalizedSearch)) return false;
      if (category && String(product.category || "").toLowerCase() !== category.toLowerCase()) return false;
      if (max !== null && Number(product.price || 0) > max) return false;
      if (inStock && Number(product.stock || 0) <= 0) return false;
      return true;
    });

    return [...filtered].sort((a, b) => {
      if (sort === "price_asc") return Number(a.price || 0) - Number(b.price || 0);
      if (sort === "price_desc") return Number(b.price || 0) - Number(a.price || 0);
      if (sort === "name_asc") return String(a.name || "").localeCompare(String(b.name || ""));
      if (sort === "newest") return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
      return 0;
    });
  }, [products, search, category, priceMax, inStock, sort]);

  useEffect(() => {
    window.localStorage.setItem("cerclay-catalogue-view", viewMode);
  }, [viewMode]);

  function clearFilters() {
    setCategory("");
    setPriceMax("");
    setInStock(false);
    setSort("featured");
  }

  const hasFilters = Boolean(category || priceMax || inStock || sort !== "featured");

  return (
    <section className="catalogue-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">THE COLLECTION</p>
          <h1>{search ? `Search results for “${searchParams.get("q")}”` : category ? `Shop ${category}` : "Shop ceramics"}</h1>
        </div>
        <p className="page-heading-copy">Everyday forms designed to feel at home in your space.</p>
      </div>

      {status === "ready" && (
        <div className="catalogue-toolbar" aria-label="Collection filters">
          <div className="catalogue-filter-intro">
            <div className="catalogue-filter-title"><SlidersHorizontal size={15} /> <span>Refine the collection</span></div>
            <span className="catalogue-filter-hint">Choose a category, price range or view.</span>
          </div>
          <div className="catalogue-filter-group">
            <label>
              <span>Category</span>
              <select value={category} onChange={(event) => setCategory(event.target.value)}>
                <option value="">All categories</option>
                {categories.map((value) => <option key={value} value={value}>{value}</option>)}
              </select>
            </label>
            <label>
              <span>Max price</span>
              <select value={priceMax} onChange={(event) => setPriceMax(event.target.value)}>
                <option value="">Any price</option>
                <option value="500">Under {money(500)}</option>
                <option value="1000">Under {money(1000)}</option>
                <option value="2000">Under {money(2000)}</option>
                <option value="5000">Under {money(5000)}</option>
              </select>
            </label>
            <label>
              <span>Sort by</span>
              <select value={sort} onChange={(event) => setSort(event.target.value)}>
                {SORT_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
            <label className="catalogue-stock-filter">
              <input type="checkbox" checked={inStock} onChange={(event) => setInStock(event.target.checked)} />
              <span>In stock only</span>
            </label>
          </div>
          <div className="catalogue-toolbar-meta">
            <span>{visibleProducts.length} {visibleProducts.length === 1 ? "piece" : "pieces"}</span>
            {hasFilters && <button type="button" className="text-button" onClick={clearFilters}>Clear filters</button>}
            <div className="catalogue-view-switch" aria-label="Catalogue layout">
              <button type="button" className={viewMode === "grid" ? "active" : ""} onClick={() => setViewMode("grid")} aria-label="Grid view" title="Grid view"><Grid2X2 size={16} /></button>
              <button type="button" className={viewMode === "list" ? "active" : ""} onClick={() => setViewMode("list")} aria-label="List view" title="List view"><List size={17} /></button>
            </div>
          </div>
        </div>
      )}

      {status === "loading" && <div className="state-box">Loading the collection…</div>}

      {status === "error" && (
        <div className="state-box error-state">
          <strong>We couldn't load the collection.</strong>
          <span>{error}</span>
          <small>Please try again in a moment. If the collection is still unavailable, refresh the page.</small>
        </div>
      )}

      {status === "ready" && visibleProducts.length === 0 && (
        <div className="state-box">
          <strong>{search || hasFilters ? "No matching products." : "No products yet."}</strong>
          <span>{search || hasFilters ? "Try another search or clear the filters." : "Our collection is taking shape. Check back soon for new pieces."}</span>
        </div>
      )}

      {status === "ready" && visibleProducts.length > 0 && (
        <>
          {(category || priceMax || inStock || search) && (
            <div className="catalogue-active-filters" aria-label="Active filters">
              {search && <span>Search: “{search}”</span>}
              {category && <span>{category}</span>}
              {priceMax && <span>Under {money(priceMax)}</span>}
              {inStock && <span>In stock</span>}
              <button type="button" onClick={clearFilters} aria-label="Clear filters"><X size={13} /></button>
            </div>
          )}
          <div className={`product-grid catalogue-products ${viewMode === "list" ? "catalogue-list-view" : ""}`}>
            {visibleProducts.map((product, index) => (
              <div className="catalogue-product-item" key={product.id ?? product.slug ?? product.name} style={{ "--catalogue-index": index }}>
                <ProductCard product={product} />
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}
