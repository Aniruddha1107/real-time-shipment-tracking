const BASE_URL = "http://localhost:8084";

// ─── AUTH STORAGE ─────────────────────────────

export const saveAuth = (data) => {
  console.log("🔥 SAVING AUTH:", data);

  localStorage.setItem("token", data.token);
  localStorage.setItem("role", data.role);
  localStorage.setItem("email", data.email);

  // ✅ SAFE CHECK (important)
  if (data.userId !== undefined && data.userId !== null) {
    localStorage.setItem("userId", data.userId);
  }
};

export const getToken = () => localStorage.getItem("token");

export const getUserId = () => localStorage.getItem("userId");

export const getRole = () => localStorage.getItem("role");

export const isLoggedIn = () => {
  return !!localStorage.getItem("token");
};

export const logout = () => {
  localStorage.clear();
};

// ─── COMMON REQUEST FUNCTION ───────────────────

const request = async (method, path, body = null, auth = false) => {
  const headers = {
    "Content-Type": "application/json",
  };

  // ✅ Add token if needed
  if (auth) {
    const token = getToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  console.log("🔥 REQUEST:", method, path, body);

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : null,
  });

  let data = {};
  try {
    data = await res.json();
  } catch (err) {
    console.log("⚠️ No JSON response");
  }

  console.log("🔥 RESPONSE:", data);

  // ❗ better error message
  if (!res.ok) {
    throw new Error(
      data.message ||
      data.error ||
      `Server Error ${res.status}`
    );
  }

  return data;
};

// ─── AUTH APIs ────────────────────────────────

export const loginUser = (email, password) =>
  request("POST", "/api/auth/login", { email, password });

export const registerUser = (name, email, password, role) =>
  request("POST", "/api/auth/register", {
    name,
    email,
    password,
    role,
  });

// ─── SHIPMENT APIs ────────────────────────────

export const getAllShipments = () =>
  request("GET", "/api/shipments", null, true);

export const createShipment = (shipmentData) =>
  request("POST", "/api/shipments", shipmentData, true);

export const assignCarrier = (shipmentId, carrierId) =>
  request(
    "PUT",
    `/api/shipments/${shipmentId}/assign/${carrierId}`,
    null,
    true
  );

// ─── NOTIFICATIONS ────────────────────────────

export const getNotifications = (userId) =>
  request("GET", `/api/notifications/user/${userId}`, null, true);

export const markNotificationRead = (notificationId) =>
  request("PUT", `/api/notifications/${notificationId}/read`, null, true);

// ─── TRACKING ────────────────────────────────
export const getTrackingHistory = (shipmentId) =>
  request("GET", `/api/tracking/${shipmentId}`, null, true);

// ─── BIDDING ─────────────────────────────────
export const placeBid = (shipmentId, carrierId, bidPrice, message = "") =>
  request("POST", "/api/bids", { shipmentId, carrierId, bidPrice, message }, true);

export const getBidsByShipment = (shipmentId) =>
  request("GET", `/api/bids/shipment/${shipmentId}`, null, true);

export const acceptLowestBid = (shipmentId) =>
  request("PUT", `/api/bids/shipment/${shipmentId}/accept-lowest`, null, true);
