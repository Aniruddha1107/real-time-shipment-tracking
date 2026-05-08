import React from "react";

const ShipmentCard = ({ shipment, onAssign }) => {

  const handleAssignClick = () => {

    const carrierId = prompt("Enter Carrier ID");

    if (!carrierId) return;

    onAssign(shipment.shipmentId, carrierId);
  };

  return (
    <div className="shipment-card">

      <h4>
        {shipment.origin} → {shipment.destination}
      </h4>

      <p>Status: {shipment.status}</p>

      <button onClick={handleAssignClick}>
        Assign Carrier
      </button>

    </div>
  );
};

export default ShipmentCard;