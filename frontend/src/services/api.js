const BASE_URL = "http://localhost:8084";

// ─── Auth helpers ─────────────────────────────────────────────────────────────

export const saveAuth = (data) => {
  localStorage.setItem("token", data.token);
  localStorage.setItem("role", data.role);
  localStorage.setItem("email", data.email);
};

export const getToken = () => localStorage.getItem("token");
export const getRole = () => localStorage.getItem("role");
export const isLoggedIn = () => !!getToken();

export const logout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("role");
  localStorage.removeItem("email");
};

// ─── Generic fetch wrapper ────────────────────────────────────────────────────

const request = async (method, path, body = null, auth = false) => {
  const headers = { "Content-Type": "application/json" };
  if (auth) headers["Authorization"] = `Bearer ${getToken()}`;

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : null,
  });

  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || "Something went wrong");
  return data;
};

// ─── Auth API ─────────────────────────────────────────────────────────────────

export const loginUser = (email, password) =>
  request("POST", "/auth/login", { email, password });

export const registerUser = (name, email, password, role) =>
  request("POST", "/auth/register", { name, email, password, role });

// ─── Shipments API ────────────────────────────────────────────────────────────

export const getAllShipments = () =>
  request("GET", "/api/shipments", null, true);

// ─── Notifications API ───────────────────────────────────────────────────────

export const getNotifications = (userId) =>
  request("GET", `/api/notifications/user/${userId}`, null, true);

export const getUnreadNotifications = (userId) =>
  request("GET", `/api/notifications/user/${userId}/unread`, null, true);

export const markNotificationRead = (notificationId) =>
  request("PUT", `/api/notifications/${notificationId}/read`, null, true);

// ─── Tracking API ─────────────────────────────────────────────────────────────

export const getTrackingHistory = (shipmentId) =>
  request("GET", `/api/tracking/${shipmentId}`, null, true);
