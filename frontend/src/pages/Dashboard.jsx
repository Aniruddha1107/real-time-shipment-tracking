import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { 
  logout, 
  getRole, 
  getUserId, 
  getAllShipments, 
  createShipment, 
  assignCarrier, 
  getNotifications 
} from "../services/api";
import "./dashboard.css";

function Dashboard() {
  const navigate = useNavigate();
  const role = getRole();
  const email = localStorage.getItem("email");
  const userId = getUserId();

  const [shipments, setShipments] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [newShipment, setNewShipment] = useState({ origin: "", destination: "", priceExpected: "" });

  // ─── LOAD DATA ────────────────────────────────
  const loadData = async () => {
    try {
      const shipData = await getAllShipments();
      setShipments(shipData);

      if (userId) {
        const notifData = await getNotifications(userId);
        setNotifications(notifData.slice(0, 5)); // show latest 5
      }
    } catch (err) {
      console.error("Failed to load dashboard data:", err);
    }
  };

  useEffect(() => {
    loadData();
    // Refresh data every 10 seconds for real-time feel
    const interval = setInterval(loadData, 10000);
    return () => clearInterval(interval);
  }, []);

  // ─── ACTIONS ──────────────────────────────────
  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const handleCreateShipment = async (e) => {
    e.preventDefault();
    try {
      await createShipment({
        ...newShipment,
        title: `${newShipment.origin} → ${newShipment.destination}`,
        description: "Standard Delivery",
        weight: 100, // default
        status: "OPEN"
      });
      alert("Shipment created successfully ✅");
      setNewShipment({ origin: "", destination: "", priceExpected: "" });
      loadData();
    } catch (err) {
      alert(err.message);
    }
  };

  const handleAssign = async (shipmentId) => {
    const carrierId = prompt("Enter Carrier ID (e.g. 2)");
    if (!carrierId) return;

    try {
      await assignCarrier(shipmentId, carrierId);
      alert("Carrier assigned successfully!");
      loadData();
    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <div className="dashboard-container">
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
        <div className="welcome-card">
          <h2>Welcome to your Dashboard 👋</h2>
          <p>Create shipments and assign carriers.</p>
        </div>

        {/* NOTIFICATIONS SECTION */}
        <div className="card notifications-section">
          <h3>📦 Notifications</h3>
          <div className="notif-list">
            {notifications.length === 0 ? (
              <p className="empty-msg">No new notifications</p>
            ) : (
              notifications.map((n) => (
                <div key={n.id} className="notif-item">
                  👉 📦 {n.message}
                </div>
              ))
            )}
          </div>
        </div>

        {/* CREATE SHIPMENT (Only for Shippers) */}
        {role === "SHIPPER" && (
          <div className="card create-section">
            <h3>Create Shipment</h3>
            <form onSubmit={handleCreateShipment} className="form-row">
              <input
                type="text"
                placeholder="Origin"
                value={newShipment.origin}
                onChange={(e) => setNewShipment({ ...newShipment, origin: e.target.value })}
                required
              />
              <input
                type="text"
                placeholder="Destination"
                value={newShipment.destination}
                onChange={(e) => setNewShipment({ ...newShipment, destination: e.target.value })}
                required
              />
              <input
                type="number"
                placeholder="Minimum Price"
                value={newShipment.priceExpected}
                onChange={(e) => setNewShipment({ ...newShipment, priceExpected: e.target.value })}
                required
              />
              <button type="submit">Create</button>
            </form>
          </div>
        )}

        {/* SHIPMENT LIST */}
        <div className="shipments-grid">
          <h3>📦 Shipments ({shipments.length})</h3>
          {shipments.map((s) => (
            <div key={s.shipmentId} className="shipment-card">
              <h4>{s.title}</h4>
              <p className="shipment-status">Status: <span>{s.status}</span></p>
              <p>Price: ₹{s.acceptedBidAmount || s.priceExpected || "N/A"}</p>
              
              {role === "SHIPPER" && s.status === "OPEN" && (
                <button onClick={() => handleAssign(s.shipmentId)}>Assign Carrier</button>
              )}
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}

export default Dashboard;
