import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  getCurrentCustomer,
  loginCustomer,
  loginWithGoogleCredential,
  logoutCustomer,
  mergeGuestCart,
  registerCustomer,
  requestPasswordResetOtp,
  resetPasswordWithOtp,
  getLocalRecentlyViewedIds,
  recordRecentlyViewedProduct,
  clearLocalRecentlyViewed,
  updateCustomer as updateCustomerApi,
  sendMobileLoginOtp,
  verifyMobileLoginOtp,
} from "../lib/api";
import { useCart } from "./CartContext";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const { guestId, refreshCart } = useCart();
  const [customer, setCustomer] = useState(null);
  const [status, setStatus] = useState("loading");

  const refreshAuth = useCallback(async () => {
    try {
      const data = await getCurrentCustomer();
      setCustomer(data);
      setStatus("authenticated");
      return data;
    } catch (error) {
      if (error.status === 401) {
        setCustomer(null);
        setStatus("anonymous");
        return null;
      }
      setStatus("error");
      throw error;
    }
  }, []);

  useEffect(() => {
    refreshAuth().catch(() => setStatus("anonymous"));
  }, [refreshAuth]);

  const completeLogin = useCallback(async (data) => {
    const nextCustomer = data?.customer || data?.user || data;
    setCustomer(nextCustomer);
    setStatus("authenticated");

    await mergeGuestCart(guestId).catch(() => {});
    await refreshCart().catch(() => {});

    // Carry anonymous recently viewed history into the signed-in customer account.
    const localRecentlyViewed = getLocalRecentlyViewedIds();
    for (const productId of localRecentlyViewed) {
      await recordRecentlyViewedProduct(productId).catch(() => {});
    }
    if (localRecentlyViewed.length) clearLocalRecentlyViewed();

    return nextCustomer;
  }, [guestId, refreshCart]);

  const login = useCallback(async (email, password) => {
    return completeLogin(await loginCustomer(email, password));
  }, [completeLogin]);

  const loginWithMobileOtp = useCallback(async (phone, otp, name = "", email = "") => {
    return completeLogin(await verifyMobileLoginOtp(phone, otp, name, email));
  }, [completeLogin]);

  const loginWithGoogle = useCallback(async (credential) => {
    return completeLogin(await loginWithGoogleCredential(credential));
  }, [completeLogin]);

  const register = useCallback(async (payload) => {
    const data = await registerCustomer(payload);
    // Registration creates the account; login is explicit so an email-verification
    // requirement can be added by the backend later without changing this contract.
    return data;
  }, []);

  const sendMobileOtp = useCallback((phone) => sendMobileLoginOtp(phone), []);
  const sendPasswordResetOtp = useCallback((phone) => requestPasswordResetOtp(phone), []);
  const resetPassword = useCallback((phone, otp, newPassword) =>
    resetPasswordWithOtp(phone, otp, newPassword), []);

  const updateCustomer = useCallback(async (payload) => {
    if (!customer?.id) throw new Error("Customer account is not available.");
    const updated = await updateCustomerApi(customer.id, payload);
    setCustomer(updated);
    return updated;
  }, [customer?.id]);

  const logout = useCallback(async () => {
    try {
      await logoutCustomer();
    } finally {
      setCustomer(null);
      setStatus("anonymous");
      await refreshCart().catch(() => {});
    }
  }, [refreshCart]);

  const value = useMemo(() => ({
    customer,
    status,
    isAuthenticated: Boolean(customer),
    refreshAuth,
    login,
    loginWithMobileOtp,
    loginWithGoogle,
    register,
    sendMobileOtp,
    sendPasswordResetOtp,
    resetPassword,
    updateCustomer,
    logout,
  }), [
    customer,
    status,
    refreshAuth,
    login,
    loginWithMobileOtp,
    loginWithGoogle,
    register,
    sendMobileOtp,
    sendPasswordResetOtp,
    resetPassword,
    updateCustomer,
    logout,
  ]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}
