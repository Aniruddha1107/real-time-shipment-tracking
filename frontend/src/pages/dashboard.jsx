import React, { useEffect, useState } from "react";
import {
  getAllShipments,
  createShipment,
  assignCarrier,
} from "../services/api";
import "./dashboard.css";

const Dashboard = () => {

  // ✅ STATES
  const [shipments, setShipments] = useState([]);

  const [notifications, setNotifications] = useState(() => {
    try {
      const saved = localStorage.getItem("notifications");
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [origin, setOrigin] = useState("");
  const [destination, setDestination] = useState("");
  const [price, setPrice] = useState("");

  // ✅ FETCH SHIPMENTS
  const fetchShipments = async () => {
    try {
      const data = await getAllShipments();
      setShipments(data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    fetchShipments();
  }, []);

  // ✅ SAVE NOTIFICATIONS (LOCAL STORAGE)
  useEffect(() => {
    localStorage.setItem(
      "notifications",
      JSON.stringify(notifications)
    );
  }, [notifications]);

  // ✅ CREATE SHIPMENT
  const handleCreate = async () => {

    if (!origin || !destination || !price) {
      alert("Please fill all fields ❗");
      return;
    }

    const userId = localStorage.getItem("userId");

    if (!userId) {
      alert("User not logged in ❌");
      return;
    }

    try {
      const payload = {
        title: `${origin} to ${destination}`,
        description: "Created from dashboard",
        origin: origin.trim(),
        destination: destination.trim(),
        weight: 10,
        priceExpected: Number(price),
        shipperId: Number(userId),
      };

      await createShipment(payload);

      // ✅ ADD NOTIFICATION (LATEST ON TOP)
      setNotifications(prev => [
        `📦 Shipment created: ${origin} → ${destination}`,
        ...prev
      ]);

      alert("Shipment created successfully ✅");

      setOrigin("");
      setDestination("");
      setPrice("");

      fetchShipments();

    } catch (err) {
      console.error(err);
      alert("Error creating shipment ❌");
    }
  };

  // ✅ ASSIGN CARRIER
  const handleAssignCarrier = async (shipmentId) => {

    const carrierId = prompt("Enter Carrier ID (e.g. 2)");
    if (!carrierId) return;

    try {
      await assignCarrier(shipmentId, carrierId);

      // ✅ ADD NOTIFICATION
      setNotifications(prev => [
        `🚚 Carrier ${carrierId} assigned to shipment ${shipmentId}`,
        ...prev
      ]);

      alert("Carrier Assigned ✅");

      fetchShipments();

    } catch (err) {
      console.error(err);
      alert("Assign failed ❌");
    }
  };

  return (
    <div className="dashboard-container">

      {/* HEADER */}
      <div className="dashboard-header">
        <h1>📍 Shipment Tracking</h1>

        <div className="header-right">
          <span>{localStorage.getItem("email")}</span>

          <span className="role-badge">
            {localStorage.getItem("role")}
          </span>

          <button
            className="logout-btn"
            onClick={() => {
              localStorage.clear();
              window.location.href = "/login";
            }}
          >
            Logout
          </button>
        </div>
      </div>

      <div className="dashboard-main">

        {/* WELCOME */}
        <div className="welcome-card">
          <h2>Welcome to your Dashboard 👋</h2>

          <p>
            {localStorage.getItem("role") === "SHIPPER"
              ? "Create shipments and assign carriers."
              : "View shipments and place bids."}
          </p>
        </div>

        {/* 🔔 NOTIFICATIONS */}
        <div className="card">
          <h3>📦 Notifications</h3>

          {notifications.length === 0 ? (
            <p>No notifications yet</p>
          ) : (
            notifications.map((n, index) => (
              <p key={index}>👉 {n}</p>
            ))
          )}
        </div>

        {/* CREATE SHIPMENT (ONLY SHIPPER) */}
        {localStorage.getItem("role") === "SHIPPER" && (
          <div className="card">
            <h3>Create Shipment</h3>

            <div className="form-row">

              <input
                placeholder="Origin"
                value={origin}
                onChange={(e) => setOrigin(e.target.value)}
              />

              <input
                placeholder="Destination"
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
              />

              <input
                type="number"
                placeholder="Minimum Price"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
              />

              <button onClick={handleCreate}>
                Create
              </button>

            </div>
          </div>
        )}

        {/* SHIPMENTS */}
        <div className="card">
          <h3>📦 Shipments ({shipments.length})</h3>

          {shipments.length === 0 && (
            <p>No shipments yet 🚚</p>
          )}

          {shipments.map((s) => (
            <div
              key={s.shipmentId}
              className="shipment-card"
            >

              <h4>{s.origin} → {s.destination}</h4>

              {/* ✅ STATUS COLOR */}
              <p style={{
                color: s.status === "OPEN" ? "orange" : "lightgreen"
              }}>
                Status: {s.status}
              </p>

              <p>Price: ₹{s.priceExpected}</p>

              {/* ROLE BASED BUTTON */}
              {localStorage.getItem("role") === "SHIPPER" ? (

                <button
                  className="assign-btn"
                  onClick={() =>
                    handleAssignCarrier(s.shipmentId)
                  }
                >
                  Assign Carrier
                </button>

              ) : (

                <button
                  className="assign-btn"
                  onClick={() => {
                    const amount = prompt("Enter Bid Amount");
                    if (!amount) return;

                    alert(`Bid placed: ₹${amount}`);
                  }}
                >
                  Place Bid
                </button>

              )}

            </div>
          ))}
        </div>

      </div>
    </div>
  );
};

export default Dashboard;