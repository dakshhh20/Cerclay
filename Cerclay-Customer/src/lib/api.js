const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    headers: {
      Accept: "application/json",
      ...(options.body && !(options.body instanceof FormData)
        ? { "Content-Type": "application/json" }
        : {}),
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    let errorCode = null;
    try {
      const data = await response.json();
      if (typeof data === "string") message = data;
      else {
        message = data.message || data.error || data.detail || message;
        if (data.code) errorCode = data.code;
      }
    } catch {
      // Keep the HTTP status message when the response is not JSON.
    }
    const error = new Error(message);
    error.status = response.status;
    error.code = errorCode;
    throw error;
  }

  if (response.status === 204) return null;

  // Read the body once and handle both JSON and plain-text Spring responses.
  // Some OTP endpoints intentionally return a success message as String,
  // while older/dev configurations may still label that response as JSON.
  const contentType = response.headers.get("content-type") || "";
  const bodyText = await response.text();
  if (!bodyText) return null;

  if (contentType.includes("application/json")) {
    try {
      return JSON.parse(bodyText);
    } catch {
      // A plain-text body with an incorrect JSON content type should still
      // be handled as text rather than producing "Unexpected token" errors.
      return bodyText;
    }
  }

  return bodyText;
}

function normaliseCustomerResponse(data) {
  return data?.customer || data?.user || data;
}

function resolveImageUrl(value) {
  if (!value) return value;
  const apiOrigin = API_BASE_URL.replace(/\/api$/, "");
  const raw = String(value).trim();
  if (!raw) return raw;

  // Some older rows contain only the stored filename. Treat it as a product
  // upload instead of resolving it relative to /api or the storefront.
  if (!raw.includes("/") && /\.(jpe?g|png|webp)$/i.test(raw)) {
    return `${apiOrigin}/uploads/products/${encodeURIComponent(raw)}`;
  }

  try {
    const parsed = new URL(raw, `${apiOrigin}/`);
    if (parsed.pathname.startsWith("/uploads/products/")) {
      return `${apiOrigin}${parsed.pathname}`;
    }
    return parsed.toString();
  } catch {
    return raw;
  }
}

function normaliseProductImages(product) {
  if (!product || typeof product !== "object") return product;
  return {
    ...product,
    image: resolveImageUrl(product.image),
    imageUrl: resolveImageUrl(product.imageUrl),
    primaryImageUrl: resolveImageUrl(product.primaryImageUrl),
    images: Array.isArray(product.images)
      ? product.images.map((image) => ({
          ...image,
          imageUrl: resolveImageUrl(image?.imageUrl || image?.url),
        }))
      : product.images,
  };
}

export { API_BASE_URL };

export function getProducts(params = {}) {
  const query = new URLSearchParams();
  if (params.search) query.set("search", params.search);
  if (params.category) query.set("category", params.category);
  if (params.minPrice !== undefined && params.minPrice !== null && params.minPrice !== "")
    query.set("minPrice", String(params.minPrice));
  if (params.maxPrice !== undefined && params.maxPrice !== null && params.maxPrice !== "")
    query.set("maxPrice", String(params.maxPrice));
  if (params.inStock !== undefined && params.inStock !== null && params.inStock !== "")
    query.set("inStock", String(params.inStock));
  if (params.sort) query.set("sort", params.sort);
  if (params.colorGroup) query.set("colorGroup", params.colorGroup);
  const suffix = query.toString() ? `?${query.toString()}` : "";
  return request(`/products${suffix}`).then((data) =>
    Array.isArray(data) ? data.map(normaliseProductImages) : data
  );
}

export async function getProduct(identifier) {
  const value = String(identifier ?? "").trim();
  if (!value) throw new Error("Product not found.");

  // Resolve the product from the catalogue first. This keeps the customer
  // page resilient even if a product-detail response is temporarily stale
  // or a backend detail endpoint is unavailable. The catalogue is already
  // the source used by the shop and is backend-driven.
  const data = await getProducts();
  const products = Array.isArray(data) ? data : data?.content || data?.products || [];
  const product = products.find(
    (item) =>
      String(item?.id ?? "") === value ||
      String(item?.slug ?? "").toLowerCase() === value.toLowerCase()
  );

  if (product) return normaliseProductImages(product);

  // Keep a direct-ID fallback for a product that is not included in a
  // paginated catalogue response.
  if (/^\d+$/.test(value)) {
    const detail = await request(`/products/${encodeURIComponent(value)}`);
    if (detail) return normaliseProductImages(detail);
  }

  throw new Error("Product not found.");
}

export function getStoreSettings() {
  return request("/store-settings");
}

export function getAvailableDiscounts() {
  return request("/discounts/available");
}

export function getFeaturedReviews() {
  return request("/reviews/featured");
}

export function getMyReviews() {
  return request("/reviews/mine");
}

export function getProductReviews(productId) {
  return request(`/reviews/product/${encodeURIComponent(productId)}`);
}

export function getReviewEligibility(productId) {
  return request(`/reviews/product/${encodeURIComponent(productId)}/eligibility`);
}

export function submitProductReview(productId, payload) {
  return request(`/reviews/product/${encodeURIComponent(productId)}`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getWishlist() {
  return request("/wishlist").then((data) =>
    Array.isArray(data) ? data.map(normaliseProductImages) : []
  );
}

export function addWishlistItem(productId) {
  return request(`/wishlist/${encodeURIComponent(productId)}`, {
    method: "POST",
  }).then((data) => (Array.isArray(data) ? data.map(normaliseProductImages) : []));
}

export function removeWishlistItem(productId) {
  return request(`/wishlist/${encodeURIComponent(productId)}`, {
    method: "DELETE",
  }).then((data) => (Array.isArray(data) ? data.map(normaliseProductImages) : []));
}

export function getRecentlyViewedProducts() {
  return request("/recently-viewed").then((data) =>
    Array.isArray(data) ? data.map(normaliseProductImages) : []
  );
}

export function recordRecentlyViewedProduct(productId) {
  return request(`/recently-viewed/${encodeURIComponent(productId)}`, {
    method: "POST",
  }).then((data) => (Array.isArray(data) ? data.map(normaliseProductImages) : []));
}

export function getLocalRecentlyViewedIds() {
  try {
    const value = JSON.parse(window.localStorage.getItem("cerclay_recently_viewed") || "[]");
    return Array.isArray(value) ? value.map(Number).filter(Number.isFinite) : [];
  } catch {
    return [];
  }
}

export function recordLocalRecentlyViewedProduct(productId) {
  try {
    const id = Number(productId);
    if (!Number.isFinite(id)) return;
    const next = [id, ...getLocalRecentlyViewedIds().filter((value) => value !== id)].slice(0, 12);
    window.localStorage.setItem("cerclay_recently_viewed", JSON.stringify(next));
  } catch {
    // Local storage is an enhancement for anonymous visitors; ignore browser storage failures.
  }
}

export function clearLocalRecentlyViewed() {
  try {
    window.localStorage.removeItem("cerclay_recently_viewed");
  } catch {
    // Ignore storage failures.
  }
}

function normaliseCartResponse(cart) {
  if (!cart || !Array.isArray(cart.items)) return cart;
  return {
    ...cart,
    items: cart.items.map((item) => ({
      ...item,
      product: item?.product ? normaliseProductImages(item.product) : item?.product,
    })),
  };
}

export function getCart(guestId) {
  return request("/cart", { headers: { "X-Guest-Id": guestId } }).then(normaliseCartResponse);
}

export function addCartItem(guestId, productId, quantity = 1, packSize = 1) {
  const params = new URLSearchParams({
    productId: String(productId),
    quantity: String(quantity),
    packSize: String(packSize),
  });
  return request(`/cart/items?${params.toString()}`, {
    method: "POST",
    headers: { "X-Guest-Id": guestId },
  }).then(normaliseCartResponse);
}

export function updateCartItem(guestId, productId, quantity, packSize = 1) {
  const params = new URLSearchParams({
    productId: String(productId),
    quantity: String(quantity),
    packSize: String(packSize),
  });
  return request(`/cart/items?${params.toString()}`, {
    method: "PUT",
    headers: { "X-Guest-Id": guestId },
  }).then(normaliseCartResponse);
}

export function removeCartItem(guestId, productId, packSize = 1) {
  const params = new URLSearchParams({ productId: String(productId), packSize: String(packSize) });
  return request(`/cart/items?${params.toString()}`, {
    method: "DELETE",
    headers: { "X-Guest-Id": guestId },
  }).then(normaliseCartResponse);
}

export function getCurrentCustomer() {
  return request("/auth/me").then(normaliseCustomerResponse);
}

export function updateCustomer(customerId, payload) {
  return request(`/customers/${encodeURIComponent(customerId)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  }).then(normaliseCustomerResponse);
}

export function loginCustomer(email, password) {
  return request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  }).then(normaliseCustomerResponse);
}

export function registerCustomer(payload) {
  return request("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  }).then(normaliseCustomerResponse);
}

export function sendMobileLoginOtp(phone) {
  const params = new URLSearchParams({
    destination: phone,
    destinationType: "PHONE",
    purpose: "PHONE_LOGIN",
  });
  return request(`/auth/otp/send?${params.toString()}`, {
    method: "POST",
  });
}

export async function verifyMobileLoginOtp(phone, otp, name = "", email = "") {
  const params = new URLSearchParams({
    destination: phone,
    destinationType: "PHONE",
    purpose: "PHONE_LOGIN",
    otp,
  });

  if (name.trim()) params.set("name", name.trim());
  if (email.trim()) params.set("email", email.trim().toLowerCase());

  const result = await request(`/auth/otp/verify?${params.toString()}`, {
    method: "POST",
  });

  // The backend currently returns the text "OTP verified successfully"
  // after creating the customer session. Fetch /auth/me so the frontend
  // receives the actual customer object instead of treating that text as
  // the authenticated customer.
  if (typeof result === "string") {
    return getCurrentCustomer();
  }

  return normaliseCustomerResponse(result);
}

export function requestPasswordResetOtp(phone) {
  const params = new URLSearchParams({
    destination: phone,
    destinationType: "PHONE",
    purpose: "PASSWORD_RESET",
  });
  return request(`/auth/otp/send?${params.toString()}`, {
    method: "POST",
  });
}

export function resetPasswordWithOtp(phone, otp, newPassword) {
  return request("/auth/otp/password-reset", {
    method: "POST",
    body: JSON.stringify({
      destination: phone,
      destinationType: "PHONE",
      otp,
      newPassword,
    }),
  });
}

export function loginWithGoogleCredential(credential) {
  return request("/auth/google", {
    method: "POST",
    body: JSON.stringify({ credential }),
  }).then(normaliseCustomerResponse);
}

export function logoutCustomer() {
  return request("/auth/logout", { method: "POST" });
}

export function mergeGuestCart(guestId) {
  return request("/cart/merge-guest", {
    method: "POST",
    headers: { "X-Guest-Id": guestId },
  });
}

export function getCustomerAddresses(customerId) {
  return request(`/addresses/customer/${encodeURIComponent(customerId)}`);
}

export function createAddress(customerId, payload) {
  return request(`/addresses/customer/${encodeURIComponent(customerId)}`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateAddress(addressId, payload) {
  return request(`/addresses/${encodeURIComponent(addressId)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteAddress(addressId) {
  return request(`/addresses/${encodeURIComponent(addressId)}`, {
    method: "DELETE",
  });
}

export function getCheckoutSummary(payload) {
  return request("/checkout/summary", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function createOrder(payload) {
  return request("/orders", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getOrder(orderId) {
  return request(`/orders/${encodeURIComponent(orderId)}`);
}

export function createCashfreeOrder(orderId) {
  return request(`/payments/cashfree/orders/${encodeURIComponent(orderId)}`, {
    method: "POST",
  });
}

export function verifyCashfreePayment(orderId) {
  return request(`/payments/cashfree/verify?orderId=${encodeURIComponent(orderId)}`, {
    method: "POST",
  });
}

export function getCustomerOrders() {
  return request("/orders");
}

export function getOrderShipment(orderId) {
  return request(`/orders/${encodeURIComponent(orderId)}/shipment`);
}

export function getOrderShipmentTimeline(orderId) {
  return request(`/orders/${encodeURIComponent(orderId)}/shipment/timeline`);
}

export function cancelCustomerOrder(orderId, reason) {
  return request(`/orders/${encodeURIComponent(orderId)}/cancel`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function getCustomerReturns() {
  return request("/returns");
}

export function getCustomerReturn(returnId) {
  return request(`/returns/${encodeURIComponent(returnId)}`);
}

export function createCustomerReturn(orderId, payload) {
  return request(`/returns/orders/${encodeURIComponent(orderId)}`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function uploadReturnPhotos(returnId, files) {
  const form = new FormData();
  Array.from(files || []).forEach((file) => form.append("files", file));
  return request(`/returns/${encodeURIComponent(returnId)}/photos`, { method: "POST", body: form });
}
