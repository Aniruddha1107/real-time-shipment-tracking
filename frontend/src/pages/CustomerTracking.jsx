import React, { useEffect, useState } from "react";
import { Search, PackageCheck, Clock } from "lucide-react";
import { getPublicShipmentTracking, getPublicTrackingHistory } from "../services/api";
import websocketService from "../services/websocket";
import TrackingMap from "../components/TrackingMap";
import "./customer-tracking.css";

const CustomerTracking = () => {
  const [trackingId, setTrackingId] = useState("");
  const [activeId, setActiveId] = useState(null);
  const [shipment, setShipment] = useState(null);
  const [latestEvent, setLatestEvent] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const loadTracking = async (e) => {
    e.preventDefault();
    if (!trackingId.trim()) return;

    setLoading(true);
    setError("");
    try {
      const id = trackingId.trim();
      const [shipmentData, history] = await Promise.all([
        getPublicShipmentTracking(id),
        getPublicTrackingHistory(id)
      ]);
      setShipment(shipmentData);
      setLatestEvent(Array.isArray(history) && history.length > 0 ? history[0] : null);
      setActiveId(id);
    } catch (err) {
      setShipment(null);
      setLatestEvent(null);
      setActiveId(null);
      setError(err.message || "Tracking ID not found");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!activeId) return undefined;

    websocketService.connect();
    websocketService.subscribe(`/topic/tracking/${activeId}`, (event) => {
      setLatestEvent(event);
    });

    return () => {
      websocketService.unsubscribe(`/topic/tracking/${activeId}`);
    };
  }, [activeId]);

  return (
    <div className="customer-root">
      <header className="customer-header">
        <div>
          <span className="customer-kicker">Customer Tracking</span>
          <h1>Track shipment</h1>
        </div>
        <a href="/login">Staff login</a>
      </header>

      <form className="tracking-search" onSubmit={loadTracking}>
        <input
          value={trackingId}
          onChange={(e) => setTrackingId(e.target.value)}
          placeholder="Enter tracking ID"
          aria-label="Tracking ID"
        />
        <button type="submit" disabled={loading}>
          <Search size={18} />
          {loading ? "Checking" : "Track"}
        </button>
      </form>

      {error && <p className="tracking-error">{error}</p>}

      {shipment ? (
        <main className="customer-grid">
          <section className="customer-map">
            <TrackingMap
              latitude={latestEvent?.latitude}
              longitude={latestEvent?.longitude}
              locationDesc={latestEvent?.locationDesc}
              originStr={shipment.origin}
              destStr={shipment.destination}
            />
          </section>

          <aside className="tracking-summary">
            <div className="summary-row">
              <PackageCheck size={20} />
              <div>
                <span>Status</span>
                <strong>{shipment.status}</strong>
              </div>
            </div>
            <div className="summary-row">
              <Clock size={20} />
              <div>
                <span>ETA</span>
                <strong>
                  {shipment.estimatedDeliveryAt
                    ? new Date(shipment.estimatedDeliveryAt).toLocaleString()
                    : "Awaiting pickup"}
                </strong>
              </div>
            </div>
            <div className="route-copy">
              <span>{shipment.origin}</span>
              <strong>{shipment.destination}</strong>
            </div>
            <p>
              {shipment.deliveredAt
                ? `Delivered at ${new Date(shipment.deliveredAt).toLocaleString()}`
                : latestEvent?.locationDesc || "Live GPS updates will appear as the shipment moves."}
            </p>
          </aside>
        </main>
      ) : (
        <div className="customer-empty">
          <PackageCheck size={56} />
          <p>Enter the tracking ID shared by your shipper to view live progress.</p>
        </div>
      )}
    </div>
  );
};

export default CustomerTracking;
