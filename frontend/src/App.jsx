import { BrowserRouter, Routes, Route } from "react-router-dom";

function Dashboard() {
  return <h2>Dashboard Page</h2>;
}

function CreateShipment() {
  return <h2>Create Shipment Page</h2>;
}

function ShipmentList() {
  return <h2>Shipment List Page</h2>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/create" element={<CreateShipment />} />
        <Route path="/shipments" element={<ShipmentList />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;