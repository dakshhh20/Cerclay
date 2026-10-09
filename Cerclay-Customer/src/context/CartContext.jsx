import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { addCartItem, getCart, removeCartItem, updateCartItem } from "../lib/api";

const CartContext = createContext(null);
const GUEST_ID_KEY = "cerclay_guest_id";

function getGuestId() {
  let guestId = localStorage.getItem(GUEST_ID_KEY);
  if (!guestId) {
    const randomId =
      typeof crypto?.randomUUID === "function"
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
    guestId = randomId;
    localStorage.setItem(GUEST_ID_KEY, guestId);
  }
  return guestId;
}

function normaliseCart(cart) {
  return cart && Array.isArray(cart.items) ? cart : { ...(cart || {}), items: [] };
}

export function CartProvider({ children }) {
  const [cart, setCart] = useState({ items: [] });
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");
  const guestId = useMemo(() => getGuestId(), []);

  const refreshCart = useCallback(async () => {
    try {
      setError("");
      const data = await getCart(guestId);
      setCart(normaliseCart(data));
      setStatus("ready");
      return normaliseCart(data);
    } catch (err) {
      setError(err.message || "Unable to load your bag.");
      setStatus("error");
      throw err;
    }
  }, [guestId]);

  useEffect(() => {
    refreshCart().catch(() => {});
  }, [refreshCart]);

  const addItem = useCallback(
    async (productId, quantity = 1, packSize = 1) => {
      const data = await addCartItem(guestId, productId, quantity, packSize);
      const nextCart = normaliseCart(data);
      setCart(nextCart);
      setError("");
      return nextCart;
    },
    [guestId]
  );

  const updateItem = useCallback(
    async (productId, quantity, packSize = 1) => {
      const data = await updateCartItem(guestId, productId, quantity, packSize);
      const nextCart = normaliseCart(data);
      setCart(nextCart);
      return nextCart;
    },
    [guestId]
  );

  const removeItem = useCallback(
    async (productId, packSize = 1) => {
      const data = await removeCartItem(guestId, productId, packSize);
      const nextCart = normaliseCart(data);
      setCart(nextCart);
      return nextCart;
    },
    [guestId]
  );

  const itemCount = cart.items.reduce(
    (sum, item) => sum + Number(item.quantity || 0) * Number(item.packSize || 1),
    0
  );
  const subtotal = cart.items.reduce((sum, item) => {
    const packSize = Number(item.packSize || 1);
    const price =
      packSize === 2 ? Number(item.product?.setOf2Price ?? 0) : Number(item.product?.price ?? 0);
    return sum + price * Number(item.quantity || 0);
  }, 0);

  const value = useMemo(
    () => ({
      guestId,
      cart,
      status,
      error,
      itemCount,
      subtotal,
      refreshCart,
      addItem,
      updateItem,
      removeItem,
    }),
    [cart, status, error, itemCount, subtotal, refreshCart, addItem, updateItem, removeItem]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const value = useContext(CartContext);
  if (!value) throw new Error("useCart must be used inside CartProvider");
  return value;
}
