# 📍 Real-Time Shipment Tracking Platform (RSTP)

A premium, end-to-end logistics platform designed for real-time shipment monitoring, carrier bidding, and automated notifications. Built with high-performance real-time communication using Spring Boot WebSockets and a modern React frontend.

---

## 🚀 Core Features

- **Real-Time Live Tracking**: Carriers broadcast GPS pings over WebSockets; shippers view live location updates on a dynamic map.
- **Secure Bidding Engine**: Carriers can browse shipments and submit bids. Shippers can review and award shipments.
- **Smart Notification System**: Automated alerts for bid status, shipment pickups, and delivery completions.
- **Role-Based Access Control**: Strict segregation between `SHIPPER` and `CARRIER` roles using JWT-secured REST and WebSocket endpoints.
- **Simulated Tracking**: Built-in mock driver service to test live tracking without requiring physical hardware.

---

## 🏗️ System Architecture

### Backend Stack
- **Framework**: Spring Boot 3.5.x
- **Database**: PostgreSQL
- **Security**: Spring Security + JWT
- **Real-Time**: STOMP over WebSockets (SockJS supported)

### Frontend Stack
- **Framework**: React 18 (Vite)
- **Styling**: Vanilla CSS (Premium Custom Design)
- **Maps**: Leaflet.js
- **State Management**: React Context API

---

## 📡 Real-Time (WebSockets) Documentation

The platform uses **STOMP** for real-time messaging.

### Connection
- **Endpoint**: `/ws`
- **Security**: Requires `Authorization: Bearer <JWT_TOKEN>` in the STOMP connection headers.

### Topics (Subscriptions)
| Topic | Description | Payload |
|-------|-------------|---------|
| `/topic/tracking/{shipmentId}` | Live GPS updates for a specific shipment | `TrackingEventDTO` |
| `/topic/user/{userId}` | Private notifications for the logged-in user | `NotificationDTO` |

### Application Destinations (Sending)
| Destination | Description | Payload |
|-------------|-------------|---------|
| `/app/tracking.update` | Send a new GPS coordinate (from Carrier device) | `TrackingEventDTO` |

---

## 🔗 API Documentation (REST)

### Authentication
- `POST /auth/register`: Create a new account (`SHIPPER` or `CARRIER`).
- `POST /auth/login`: Authenticate and receive a JWT.

### Shipments
- `POST /api/shipments`: Create a new shipment (Shipper only).
- `GET /api/shipments`: List all available/assigned shipments.
- `GET /api/shipments/{id}`: Get detailed shipment info.
- `PATCH /api/shipments/{id}/status`: Update shipment status (e.g., `AWAITING_PICKUP` → `IN_TRANSIT`).

### Bids
- `POST /api/bids`: Submit a bid for a shipment (Carrier only).
- `GET /api/bids/shipment/{id}`: View all bids for a shipment.

### Tracking
- `GET /api/tracking/{shipmentId}`: Fetch historical tracking points for a shipment.

---

## 🛠️ Setup & Installation

### Backend (Java 21)
1. Configure your PostgreSQL database in `src/main/resources/application.properties`.
2. Run the application using Maven:
   ```bash
   mvn spring-boot:run
   ```

### Frontend (Node.js)
1. Navigate to the `frontend` folder.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```

### Running Simulation
The backend includes a `SimulatedDriverService` that automatically generates GPS pings every 10 seconds for shipment ID `1` when enabled. This allows for testing the frontend map UI without a real driver app.

---

## 🧪 Testing

Run the full test suite (unit + integration):
```bash
mvn test
```
