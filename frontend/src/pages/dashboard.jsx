import { useNavigate } from "react-router-dom";
import { logout, getRole, getUserId } from "../services/api";
import { useState, useEffect } from "react";
import {
  createShipment,
  getAllShipments,
  assignCarrier
} from "../services/api";
import ShipmentCard from "../components/ShipmentCard";
import "./dashboard.css";

function Dashboard() {
  const navigate = useNavigate();
  const role = getRole();
  const email = localStorage.getItem("email");
  const username = email ? email.split("@")[0] : "User";

  const [shipments, setShipments] = useState([]);
  const [notifications, setNotifications] = useState([]);

  const [form, setForm] = useState({
    origin: "",
    destination: "",
    minPrice: "",
  });

  // 🔥 Load Shipments
  const loadShipments = async () => {
    try {
      const data = await getAllShipments();
      setShipments(data);
    } catch (err) {
      console.error(err);
      alert("Failed to load shipments ❌");
    }
  };

  useEffect(() => {
    loadShipments();
  }, []);

  // 🔐 Logout
  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  // 📦 Create Shipment
  const handleCreateShipment = async (e) => {
    e.preventDefault();

    try {
      const userId = getUserId();

      const payload = {
        title: "Shipment",
        description: "Created via dashboard",
        origin: form.origin,
        destination: form.destination,
        weight: 10,
        priceExpected: Number(form.minPrice),
        shipperId: Number(userId),
      };

      await createShipment(payload);

      setNotifications((prev) => [
        ...prev,
        `📦 Shipment created: ${form.origin} → ${form.destination}`,
      ]);

      alert("Shipment Created ✅");

      setForm({ origin: "", destination: "", minPrice: "" });
      loadShipments();

    } catch (err) {
      console.error(err);
      alert("Error creating shipment ❌");
    }
  };

  // 🚚 Assign Carrier
  const handleAssign = async (shipmentId) => {
    console.log("Clicked:", shipmentId); // DEBUG

    const carrierId = prompt("Enter Carrier ID:");

    if (!carrierId) return;

    try {
      await assignCarrier(shipmentId, carrierId);

      setNotifications((prev) => [
        ...prev,
        `🚚 Carrier assigned to shipment ${shipmentId}`,
      ]);

      alert("Carrier Assigned ✅");
      loadShipments();

    } catch (err) {
      console.error(err);
      alert("Assign failed ❌");
    }
  };

  return (
    <div className="dashboard-container">

      {/* HEADER */}
      <header className="dashboard-header">
        <h1>📍 Shipment Tracking</h1>

        <div className="header-right">
          <span className="user-info">
            {email} <span className="role-badge">{role}</span>
          </span>

          <button className="logout-btn" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-main">

        {/* Welcome */}
        <div className="welcome-card">
          <h2>Welcome {username} 👋</h2>
          <p>
            {role === "SHIPPER"
              ? "Create shipments and assign carriers."
              : "View shipments and bid."}
          </p>
        </div>

        {/* Notifications */}
        <div className="card">
          <h3>🔔 Notifications</h3>

          {notifications.length === 0 ? (
            <p>No notifications</p>
          ) : (
            notifications.map((n, i) => <p key={i}>{n}</p>)
          )}
        </div>

        {/* Create Shipment */}
        {role === "SHIPPER" && (
          <div className="card">
            <h3>Create Shipment</h3>

            <form onSubmit={handleCreateShipment}>
              <input
                placeholder="Origin"
                value={form.origin}
                onChange={(e) =>
                  setForm({ ...form, origin: e.target.value })
                }
                required
              />

              <input
                placeholder="Destination"
                value={form.destination}
                onChange={(e) =>
                  setForm({ ...form, destination: e.target.value })
                }
                required
              />

              <input
                type="number"
                placeholder="Minimum Price"
                value={form.minPrice}
                onChange={(e) =>
                  setForm({ ...form, minPrice: e.target.value })
                }
                required
              />

              <button type="submit">Create</button>
            </form>
          </div>
        )}

        {/* Shipments */}
        <div className="card">
          <h3>📦 Shipments ({shipments.length})</h3>

          {shipments.map((s) => (
            <ShipmentCard
              key={s.shipmentId}
              shipment={s}
              role={role}
              onAssign={handleAssign}
            />
          ))}
        </div>

      </main>
    </div>
  );
}

export default Dashboard;