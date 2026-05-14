import React, { useEffect, useState, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  Package, 
  Navigation, 
  Bell, 
  PlusCircle, 
  LogOut, 
  Map as MapIcon,
  CheckCircle,
  Clock,
  Loader,
  DollarSign,
  Users
} from "lucide-react";
import { 
  getAllShipments, 
  createShipment, 
  placeBid, 
  getBidsByShipment, 
  acceptLowestBid 
} from "../services/api";
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
  const [loading, setLoading] = useState(true);

  // Bidding State
  const [bids, setBids] = useState({});
  const [viewingBidsFor, setViewingBidsFor] = useState(null);
  const [bidInputs, setBidInputs] = useState({}); // { shipmentId: amount }

  const role = localStorage.getItem("role") || "USER";
  const userId = localStorage.getItem("userId");
  const email = localStorage.getItem("email") || "Guest";
  const token = localStorage.getItem("token");

  // ─── INITIAL FETCH ──────────────────────────────────
  const fetchShipments = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getAllShipments();
      setShipments(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Fetch error:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  // ─── WEBSOCKET SETUP ────────────────────────────────
  useEffect(() => {
    if (token) {
      try {
        websocketService.connect(token);
        
        websocketService.subscribe('/topic/notifications', (msg) => {
          setNotifications(prev => [{ id: Date.now(), message: msg.message || msg }, ...prev].slice(0, 5));
        });

        if (userId) {
          websocketService.subscribe(`/topic/notifications/${userId}`, (msg) => {
            setNotifications(prev => [{ id: Date.now(), message: msg.message || msg }, ...prev].slice(0, 5));
            // Auto refresh shipments when a bid is accepted or tracking updates
            fetchShipments();
          });
        }
      } catch (wsErr) {
        console.error("WS Connection Error:", wsErr);
      }
    }

    fetchShipments();

    return () => {
      try {
        websocketService.disconnect();
      } catch (err) {}
    };
  }, [token, userId, fetchShipments]);

  // ─── TRACKING SUBSCRIPTION ─────────────────────────
  const startTracking = (shipment) => {
    const shipmentId = shipment.shipmentId;
    if (!shipmentId) return;

    if (activeTracking === shipmentId) {
      websocketService.unsubscribe(`/topic/tracking/${shipmentId}`);
      setActiveTracking(null);
      return;
    }

    if (activeTracking) {
      websocketService.unsubscribe(`/topic/tracking/${activeTracking}`);
    }

    setActiveTracking(shipmentId);
    setViewingBidsFor(null); // Close bids view when tracking
    
    // Set origin/dest for map routing
    setTrackingData(prev => ({ 
      ...prev, 
      [shipmentId]: { 
        ...prev[shipmentId], 
        originStr: shipment.origin, 
        destStr: shipment.destination 
      } 
    }));

    websocketService.subscribe(`/topic/tracking/${shipmentId}`, (data) => {
      if (data) {
        setTrackingData(prev => ({ 
          ...prev, 
          [shipmentId]: { ...prev[shipmentId], ...data } 
        }));
      }
    });
  };

  // ─── BIDDING HANDLERS ──────────────────────────────
  const handlePlaceBid = async (e, shipmentId) => {
    e.stopPropagation();
    const amount = bidInputs[shipmentId];
    if (!amount) return;

    try {
      await placeBid(shipmentId, userId, amount, "Bid placed from dashboard");
      alert(`Bid of ₹${amount} placed successfully!`);
      setBidInputs(prev => ({ ...prev, [shipmentId]: "" }));
    } catch (err) {
      alert("Failed to place bid: " + err.message);
    }
  };

  const loadBids = async (e, shipmentId) => {
    e.stopPropagation();
    if (viewingBidsFor === shipmentId) {
      setViewingBidsFor(null);
      return;
    }
    
    try {
      const data = await getBidsByShipment(shipmentId);
      setBids(prev => ({ ...prev, [shipmentId]: data }));
      setViewingBidsFor(shipmentId);
      setActiveTracking(null); // Close tracking if opening bids
    } catch (err) {
      alert("Could not load bids: " + err.message);
    }
  };

  const handleAcceptLowestBid = async (shipmentId) => {
    if (!window.confirm("Accept the lowest bid?")) return;
    try {
      await acceptLowestBid(shipmentId);
      alert("Lowest bid accepted! Shipment is now IN_TRANSIT.");
      setViewingBidsFor(null);
      fetchShipments();
    } catch (err) {
      alert("Failed to accept bid: " + err.message);
    }
  };

  // ─── SHIPMENT HANDLERS ─────────────────────────────
  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        title: `${newShipment.origin} to ${newShipment.destination}`,
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
      alert("Failed to create shipment: " + err.message);
    }
  };

  if (loading && shipments.length === 0) {
    return (
      <div className="loading-screen">
        <Loader className="spinner" size={48} />
        <p>Loading your dashboard...</p>
      </div>
    );
  }

  return (
    <div className="dashboard-root">
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
          <button onClick={() => { localStorage.clear(); window.location.href = "/login"; }} className="icon-btn logout">
            <LogOut size={20} />
          </button>
        </div>
      </nav>

      <div className="dashboard-content">
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
                initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }}
                onSubmit={handleCreate} className="create-form-card"
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
                layout key={s.shipmentId} 
                className={`shipment-card ${activeTracking === s.shipmentId ? 'active' : ''}`}
                onClick={() => startTracking(s)}
              >
                <div className="card-top">
                  <span className={`status-pill ${(s.status || 'OPEN').toLowerCase()}`}>{s.status || 'OPEN'}</span>
                  <span className="price-tag">₹{s.priceExpected || 0}</span>
                </div>
                <h4>{s.origin || 'N/A'} <Navigation size={14} className="arrow" /> {s.destination || 'N/A'}</h4>
                
                {/* ─── ACTION BUTTONS ─── */}
                <div className="card-actions" style={{ marginTop: '1rem' }}>
                  
                  {/* Carrier Bidding Logic */}
                  {role === "CARRIER" && s.status === "OPEN" && (
                    <div className="bid-input-group" onClick={e => e.stopPropagation()}>
                      <input 
                        type="number" 
                        placeholder="₹ Bid Amount" 
                        value={bidInputs[s.shipmentId] || ""}
                        onChange={e => setBidInputs({...bidInputs, [s.shipmentId]: e.target.value})}
                      />
                      <button className="bid-btn" onClick={(e) => handlePlaceBid(e, s.shipmentId)}>
                        Place Bid
                      </button>
                    </div>
                  )}

                  {/* Shipper Bidding Logic */}
                  {role === "SHIPPER" && s.status === "OPEN" && (
                    <button 
                      className="view-bids-btn" 
                      onClick={(e) => loadBids(e, s.shipmentId)}
                    >
                      <Users size={14} /> {viewingBidsFor === s.shipmentId ? "Hide Bids" : "View Bids"}
                    </button>
                  )}

                  {/* Tracking Logic */}
                  {s.status === "IN_TRANSIT" && (
                    <button className="track-btn">
                      <MapIcon size={14} style={{ marginRight: '4px' }}/> 
                      {activeTracking === s.shipmentId ? "Stop Tracking" : "Live Track"}
                    </button>
                  )}
                </div>

              </motion.div>
            ))}
            {!loading && shipments.length === 0 && <p className="empty-msg">No shipments found</p>}
          </div>
        </aside>

        <main className="map-panel">
          {viewingBidsFor ? (
            <div className="bids-view-panel">
              <div className="bids-header">
                <h2>Bids for Shipment #{viewingBidsFor}</h2>
                <button className="accept-lowest-btn" onClick={() => handleAcceptLowestBid(viewingBidsFor)}>
                  <DollarSign size={16} /> Accept Lowest Bid
                </button>
              </div>
              <div className="bids-grid">
                {bids[viewingBidsFor]?.length > 0 ? (
                  bids[viewingBidsFor].map(bid => (
                    <div key={bid.bidId} className="bid-card">
                      <div className="bid-amount">₹{bid.bidPrice}</div>
                      <div className="bid-carrier">Carrier ID: {bid.carrierId}</div>
                      <div className="bid-message">"{bid.message}"</div>
                    </div>
                  ))
                ) : (
                  <p className="no-bids">No bids received yet.</p>
                )}
              </div>
            </div>
          ) : activeTracking ? (
            <div className="map-container-inner">
              <TrackingMap 
                latitude={trackingData[activeTracking]?.latitude}
                longitude={trackingData[activeTracking]?.longitude}
                locationDesc={trackingData[activeTracking]?.locationDesc}
                originStr={trackingData[activeTracking]?.originStr}
                destStr={trackingData[activeTracking]?.destStr}
              />
              <div className="tracking-overlay">
                <div className="overlay-pill">
                  <Clock size={16} /> 
                  <span>{trackingData[activeTracking]?.eventTimestamp ? new Date(trackingData[activeTracking].eventTimestamp).toLocaleTimeString() : 'Awaiting GPS...'}</span>
                </div>
              </div>
            </div>
          ) : (
            <div className="map-placeholder">
              <div className="placeholder-content">
                <MapIcon size={64} />
                <h2>Select a shipment to begin live tracking</h2>
                <p>Real-time GPS updates will appear here.</p>
              </div>
            </div>
          )}
        </main>

        <aside className="notif-panel">
          <header className="panel-header">
            <h3><Bell size={20} /> Alerts</h3>
          </header>
          <div className="notif-list">
            {notifications.map((n) => (
              <motion.div initial={{ x: 50, opacity: 0 }} animate={{ x: 0, opacity: 1 }} key={n.id} className="notif-card">
                <div className="notif-icon"><CheckCircle size={16} /></div>
                <div className="notif-body">
                  <p>{n.message}</p>
                  <small>{new Date(n.id).toLocaleTimeString()}</small>
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