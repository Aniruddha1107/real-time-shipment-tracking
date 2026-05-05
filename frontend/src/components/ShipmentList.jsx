import { useEffect, useState } from "react";
import { getAllShipments } from "../services/api";
import ShipmentCard from "./ShipmentCard";

function ShipmentList() {
  const [shipments, setShipments] = useState([]);

  const loadData = async () => {
    try {
      const data = await getAllShipments();
      setShipments(data);
    } catch (err) {
      alert(err.message);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div>
      <h2>Shipments</h2>

      {shipments.length === 0 ? (
        <p>No shipments found</p>
      ) : (
        shipments.map((s) => (
          <ShipmentCard
            key={s.shipmentId}
            shipment={s}
            refresh={loadData}
          />
        ))
      )}
    </div>
  );
}

export default ShipmentList;