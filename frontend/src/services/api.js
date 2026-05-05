const BASE_URL = "http://localhost:8084";

// ─── AUTH HELPERS ─────────────────────────────────────────

export const saveAuth = (data) => {
  localStorage.setItem("token", data.token);
  localStorage.setItem("role", data.role);
  localStorage.setItem("email", data.email);

  // ✅ Optional (only if backend sends it)
  if (data.userId) {
    localStorage.setItem("userId", data.userId);
  }
};

export const getToken = () => localStorage.getItem("token");
export const getRole = () => localStorage.getItem("role");
export const getEmail = () => localStorage.getItem("email");
export const getUserId = () => localStorage.getItem("userId");

export const isLoggedIn = () => !!getToken();

export const logout = () => {
  localStorage.clear();
};

// ─── GENERIC REQUEST ──────────────────────────────────────

const request = async (method, path, body = null, auth = false) => {
  const headers = {
    "Content-Type": "application/json",
  };

  // ✅ Add JWT only when needed
  if (auth) {
    const token = getToken();

    if (!token) {
      throw new Error("User not authenticated ❌");
    }

    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : null,
  });

  // ✅ Safe JSON parsing
  let data = {};
  try {
    data = await res.json();
  } catch {
    data = {};
  }

  // ✅ Proper error handling
  if (!res.ok) {
    throw new Error(data.message || `Error ${res.status}`);
  }

  return data;
};

// ─── AUTH API ─────────────────────────────────────────────

export const loginUser = (email, password) =>
  request("POST", "/api/auth/login", { email, password });

export const registerUser = (name, email, password, role) =>
  request("POST", "/api/auth/register", {
    name,
    email,
    password,
    role,
  });

// ─── SHIPMENT API ─────────────────────────────────────────

// ✅ Get all shipments
export const getAllShipments = () =>
  request("GET", "/api/shipments", null, true);

// ✅ Create shipment
export const createShipment = (shipmentData) =>
  request("POST", "/api/shipments", shipmentData, true);

// ✅ Assign carrier
export const assignCarrier = (shipmentId, carrierId) =>
  request(
    "PUT",
    `/api/shipments/${shipmentId}/assign/${carrierId}`,
    null,
    true
  );

// ─── NOTIFICATIONS API ────────────────────────────────────

export const getNotifications = (userId) =>
  request("GET", `/api/notifications/user/${userId}`, null, true);

export const getUnreadNotifications = (userId) =>
  request("GET", `/api/notifications/user/${userId}/unread`, null, true);

export const markNotificationRead = (notificationId) =>
  request("PUT", `/api/notifications/${notificationId}/read`, null, true);

// ─── TRACKING API ─────────────────────────────────────────

export const getTrackingHistory = (shipmentId) =>
  request("GET", `/api/tracking/${shipmentId}`, null, true);
