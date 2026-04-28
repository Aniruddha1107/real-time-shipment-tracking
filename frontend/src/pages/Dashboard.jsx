import { useNavigate } from "react-router-dom";
import { logout, getRole } from "../services/api";
import "./dashboard.css";

function Dashboard() {
  const navigate = useNavigate();
  const role = getRole();
  const email = localStorage.getItem("email");

  const handleLogout = () => {
    logout();
    navigate("/login");
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
          <h2>Welcome to your Dashboard</h2>
          <p>
            {role === "SHIPPER"
              ? "You can create shipments, view bids, and track deliveries in real-time."
              : "You can browse available shipments, place bids, and update delivery status."}
          </p>
        </div>

        <div className="stats-grid">
          <div className="stat-card">
            <h3>📦</h3>
            <p className="stat-label">Shipments</p>
            <p className="stat-value">—</p>
          </div>
          <div className="stat-card">
            <h3>💰</h3>
            <p className="stat-label">Active Bids</p>
            <p className="stat-value">—</p>
          </div>
          <div className="stat-card">
            <h3>🔔</h3>
            <p className="stat-label">Notifications</p>
            <p className="stat-value">—</p>
          </div>
          <div className="stat-card">
            <h3>📍</h3>
            <p className="stat-label">Live Tracking</p>
            <p className="stat-value">—</p>
          </div>
        </div>
      </main>
    </div>
  );
}

export default Dashboard;
