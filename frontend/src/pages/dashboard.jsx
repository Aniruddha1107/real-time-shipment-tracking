import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./dashboard.css";

function Dashboard() {
  const navigate = useNavigate();

  const email = localStorage.getItem("email");
  const username = email ? email.split("@")[0] : "User";

  // ✅ Dummy shipment data
  const [shipments] = useState([
    { origin: "Mumbai", destination: "Pune", status: "In Transit" },
    { origin: "Delhi", destination: "Bangalore", status: "Delivered" },
  ]);

  const handleLogout = () => {
    localStorage.clear();
    navigate("/login");
  };

  return (
    <div className="dashboard-container">

      {/* 🔴 Logout */}
      <button className="logout-btn" onClick={handleLogout}>
        Logout
      </button>

      {/* 🔵 Header */}
      <div className="dashboard-header">
        <h1>Dashboard</h1>
        <h2>Welcome {username} </h2>
          <p style={{ color: "#666", marginTop: "5px" }}>
    Logged in as: <b>{email}</b>
    </p>
        
      </div>

      {/* 🔥 Cards */}
      <div className="cards-container">

        {/* 📦 Shipments */}
        <div className="card">
          <h3>📦 My Shipments ({shipments.length})</h3>

          {shipments.map((s, i) => (
            <div key={i} className="shipment-card">
              <p>
                <b>{s.origin}</b> → <b>{s.destination}</b>
              </p>

              <p style={{ color: s.status === "Delivered" ? "green" : "#ff7a00" }}>
                Status: {s.status}
              </p>
            </div>
          ))}
        </div>

        {/* 🗺️ Tracking */}
        <div className="card">
          <h3>🗺️ Live Tracking</h3>
          <p>Tracking feature coming soon...</p>
        </div>

      </div>
    </div>
  );
}

export default Dashboard;