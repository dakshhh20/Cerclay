import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { addWishlistItem, getWishlist, removeWishlistItem } from "../lib/api";
import { useAuth } from "./AuthContext";

const WishlistContext = createContext(null);

export function WishlistProvider({ children }) {
  const { customer, status } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const refreshWishlist = useCallback(async () => {
    if (!customer) {
      setItems([]);
      return [];
    }
    setLoading(true);
    try {
      const next = await getWishlist();
      setItems(Array.isArray(next) ? next : []);
      return next;
    } finally {
      setLoading(false);
    }
  }, [customer]);

  useEffect(() => {
    if (status === "authenticated" && customer) {
      refreshWishlist().catch(() => setItems([]));
    } else if (status === "anonymous") {
      setItems([]);
      setLoading(false);
    }
  }, [status, customer, refreshWishlist]);

  const isWishlisted = useCallback(
    (productId) => items.some((item) => String(item?.id) === String(productId)),
    [items]
  );

  const add = useCallback(
    async (productId) => {
      if (!customer) throw new Error("Please sign in to save products to your wishlist.");
      const next = await addWishlistItem(productId);
      setItems(next);
      return next;
    },
    [customer]
  );

  const remove = useCallback(
    async (productId) => {
      if (!customer) throw new Error("Please sign in to manage your wishlist.");
      const next = await removeWishlistItem(productId);
      setItems(next);
      return next;
    },
    [customer]
  );

  const toggle = useCallback(
    async (productId) => {
      return isWishlisted(productId) ? remove(productId) : add(productId);
    },
    [add, isWishlisted, remove]
  );

  const value = useMemo(
    () => ({
      items,
      count: items.length,
      loading,
      refreshWishlist,
      isWishlisted,
      add,
      remove,
      toggle,
    }),
    [items, loading, refreshWishlist, isWishlisted, add, remove, toggle]
  );

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
}

export function useWishlist() {
  const value = useContext(WishlistContext);
  if (!value) throw new Error("useWishlist must be used inside WishlistProvider");
  return value;
}
