import React from "react";

function ShipmentCard({ shipment, role, onAssign }) {
  return (
    <div className="shipment-card">
      <h4>
        {shipment.origin} → {shipment.destination}
      </h4>

      <p>Status: {shipment.status}</p>

      {/* ✅ FIXED BUTTON */}
      {role === "SHIPPER" && shipment.status === "OPEN" && (
        <button
          onClick={() => onAssign(shipment.shipmentId)}
        >
          Assign Carrier
        </button>
      )}
    </div>
  );
}

export default ShipmentCard;