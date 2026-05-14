import React, { useEffect, useState, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  Package, 
  Navigation, 
  Bell, 
  PlusCircle, 
  LogOut, 
  Truck, 
  Map as MapIcon,
  CheckCircle,
  Clock
} from "lucide-react";
import { getAllShipments, createShipment, assignCarrier } from "../services/api";
import websocketService from "../services/websocket";
import TrackingMap from "../components/TrackingMap";
import "./dashboard.css";

const Dashboard = () => {
  const [shipments, setShipments] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [activeTracking, setActiveTracking] = useState(null);
  const [trackingData, setTrackingData] = useState({});
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newShipment, setNewShipment] = useState({ origin: "", destination: "", price: "" });

  const role = localStorage.getItem("role");
  const userId = localStorage.getItem("userId");
  const email = localStorage.getItem("email");
  const token = localStorage.getItem("token");

  // ─── INITIAL FETCH ──────────────────────────────────
  const fetchShipments = useCallback(async () => {
    try {
      const data = await getAllShipments();
      setShipments(data);
    } catch (err) {
      console.error("Fetch error:", err);
    }
  }, []);

  // ─── WEBSOCKET SETUP ────────────────────────────────
  useEffect(() => {
    if (token) {
      websocketService.connect(token);
      
      // Subscribe to global notifications
      websocketService.subscribe('/topic/notifications', (msg) => {
        setNotifications(prev => [{ id: Date.now(), ...msg }, ...prev].slice(0, 5));
      });

      // Subscribe to personal notifications if userId exists
      if (userId) {
        websocketService.subscribe(`/topic/notifications/${userId}`, (msg) => {
          setNotifications(prev => [{ id: Date.now(), ...msg }, ...prev].slice(0, 5));
        });
      }
    }

    fetchShipments();

    return () => websocketService.disconnect();
  }, [token, userId, fetchShipments]);

  // ─── TRACKING SUBSCRIPTION ─────────────────────────
  const startTracking = (shipmentId) => {
    if (activeTracking === shipmentId) {
      websocketService.unsubscribe(`/topic/tracking/${shipmentId}`);
      setActiveTracking(null);
      return;
    }

    // Unsubscribe from previous if any
    if (activeTracking) {
      websocketService.unsubscribe(`/topic/tracking/${activeTracking}`);
    }

    setActiveTracking(shipmentId);
    websocketService.subscribe(`/topic/tracking/${shipmentId}`, (data) => {
      setTrackingData(prev => ({ ...prev, [shipmentId]: data }));
    });
  };

  // ─── HANDLERS ──────────────────────────────────────
  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        title: `${newShipment.origin} → ${newShipment.destination}`,
        origin: newShipment.origin,
        destination: newShipment.destination,
        priceExpected: Number(newShipment.price),
        shipperId: Number(userId),
        weight: 10,
        status: "OPEN"
      };
      await createShipment(payload);
      setIsCreateOpen(false);
      setNewShipment({ origin: "", destination: "", price: "" });
      fetchShipments();
    } catch (err) {
      alert("Failed to create shipment");
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    window.location.href = "/login";
  };

  return (
    <div className="dashboard-root">
      {/* 🧭 NAVIGATION */}
      <nav className="glass-nav">
        <div className="nav-logo">
          <Navigation className="logo-icon" />
          <span>RSTP <small>Live</small></span>
        </div>
        <div className="nav-user">
          <div className="user-meta">
            <span className="user-email">{email}</span>
            <span className="user-role">{role}</span>
          </div>
          <button onClick={handleLogout} className="icon-btn logout"><LogOut size={20} /></button>
        </div>
      </nav>

      <div className="dashboard-content">
        {/* 📟 LEFT PANEL: SHIPMENTS */}
        <aside className="shipments-panel">
          <header className="panel-header">
            <h3><Package size={20} /> My Shipments</h3>
            {role === "SHIPPER" && (
              <button onClick={() => setIsCreateOpen(!isCreateOpen)} className="add-btn">
                <PlusCircle size={20} />
              </button>
            )}
          </header>

          <AnimatePresence>
            {isCreateOpen && (
              <motion.form 
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: "auto", opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                onSubmit={handleCreate} 
                className="create-form-card"
              >
                <input placeholder="Origin" value={newShipment.origin} onChange={e => setNewShipment({...newShipment, origin: e.target.value})} required />
                <input placeholder="Destination" value={newShipment.destination} onChange={e => setNewShipment({...newShipment, destination: e.target.value})} required />
                <input type="number" placeholder="Price (₹)" value={newShipment.price} onChange={e => setNewShipment({...newShipment, price: e.target.value})} required />
                <button type="submit" className="submit-btn">Publish Shipment</button>
              </motion.form>
            )}
          </AnimatePresence>

          <div className="shipment-list">
            {shipments.map((s) => (
              <motion.div 
                layout
                key={s.shipmentId} 
                className={`shipment-card ${activeTracking === s.shipmentId ? 'active' : ''}`}
                onClick={() => startTracking(s.shipmentId)}
              >
                <div className="card-top">
                  <span className={`status-pill ${s.status.toLowerCase()}`}>{s.status}</span>
                  <span className="price-tag">₹{s.priceExpected}</span>
                </div>
                <h4>{s.origin} <Navigation size={14} className="arrow" /> {s.destination}</h4>
                <div className="card-actions">
                  <button className="track-btn">
                    {activeTracking === s.shipmentId ? "Stop Tracking" : "Live Track"}
                  </button>
                </div>
              </motion.div>
            ))}
          </div>
        </aside>

        {/* 🗺️ CENTER PANEL: MAP */}
        <main className="map-panel">
          {activeTracking ? (
            <div className="map-container-inner">
              <TrackingMap 
                latitude={trackingData[activeTracking]?.latitude}
                longitude={trackingData[activeTracking]?.longitude}
                locationDesc={trackingData[activeTracking]?.locationDesc}
              />
              <div className="tracking-overlay">
                <div className="overlay-pill">
                  <Clock size={16} /> 
                  <span>Latest Update: {trackingData[activeTracking]?.eventTimestamp ? new Date(trackingData[activeTracking].eventTimestamp).toLocaleTimeString() : 'Waiting for pings...'}</span>
                </div>
              </div>
            </div>
          ) : (
            <div className="map-placeholder">
              <div className="placeholder-content">
                <MapIcon size={64} />
                <h2>Select a shipment to begin live tracking</h2>
                <p>Real-time GPS updates will appear here once you select an active shipment.</p>
              </div>
            </div>
          )}
        </main>

        {/* 🔔 RIGHT PANEL: NOTIFICATIONS */}
        <aside className="notif-panel">
          <header className="panel-header">
            <h3><Bell size={20} /> Alerts</h3>
          </header>
          <div className="notif-list">
            {notifications.map((n) => (
              <motion.div 
                initial={{ x: 50, opacity: 0 }}
                animate={{ x: 0, opacity: 1 }}
                key={n.id} 
                className="notif-card"
              >
                <div className="notif-icon"><CheckCircle size={16} /></div>
                <div className="notif-body">
                  <p>{n.message || n.eventType}</p>
                  <small>{new Date().toLocaleTimeString()}</small>
                </div>
              </motion.div>
            ))}
            {notifications.length === 0 && <p className="empty-msg">No recent activity</p>}
          </div>
        </aside>
      </div>
    </div>
  );
};

export default Dashboard;