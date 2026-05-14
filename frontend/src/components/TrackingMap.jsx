import React, { useEffect, useMemo, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap, Polyline } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const makeLabelIcon = (label, color) => L.divIcon({
    className: '',
    html: `
        <div style="
            width: 34px;
            height: 34px;
            border-radius: 50%;
            background: ${color};
            color: white;
            display: grid;
            place-items: center;
            font-weight: 800;
            border: 3px solid white;
            box-shadow: 0 10px 24px rgba(15, 23, 42, 0.28);
        ">${label}</div>
    `,
    iconSize: [34, 34],
    iconAnchor: [17, 17],
    popupAnchor: [0, -18]
});

const originIcon = makeLabelIcon('S', '#16a34a');
const destinationIcon = makeLabelIcon('E', '#dc2626');

// Truck icon for the shipment
const truckIcon = new L.Icon({
    iconUrl: 'https://cdn-icons-png.flaticon.com/512/3063/3063822.png',
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -40]
});

// Mock Geocoder for Demo Purposes
const MOCK_COORDS = {
    'new york': [40.7128, -74.0060],
    'los angeles': [34.0522, -118.2437],
    'chicago': [41.8781, -87.6298],
    'houston': [29.7604, -95.3698],
    'mumbai': [19.0760, 72.8777],
    'delhi': [28.7041, 77.1025],
    'bangalore': [12.9716, 77.5946],
    'hyderabad': [17.3850, 78.4867],
    'salapur': [17.6599, 75.9064],
    'pune': [18.5204, 73.8567],
    'begampet': [17.4447, 78.4664],
    'nampalli': [17.3847, 78.4682]
};

const INDIA_BOUNDS = {
    minLat: 8.4,
    maxLat: 32.5,
    minLng: 68.7,
    maxLng: 88.2
};

const hashText = (text) => {
    let hash = 0;
    for (let i = 0; i < text.length; i += 1) {
        hash = ((hash << 5) - hash) + text.charCodeAt(i);
        hash |= 0;
    }
    return Math.abs(hash);
};

export const getCoordinates = (cityStr) => {
    if (!cityStr || typeof cityStr !== 'string') return null;
    const key = cityStr.toLowerCase().split(',')[0].trim();
    if (MOCK_COORDS[key]) return MOCK_COORDS[key];

    const hash = hashText(key);
    const latRange = INDIA_BOUNDS.maxLat - INDIA_BOUNDS.minLat;
    const lngRange = INDIA_BOUNDS.maxLng - INDIA_BOUNDS.minLng;
    const latitude = INDIA_BOUNDS.minLat + ((hash % 10000) / 10000) * latRange;
    const longitude = INDIA_BOUNDS.minLng + (((Math.floor(hash / 10000)) % 10000) / 10000) * lngRange;

    return [Number(latitude.toFixed(4)), Number(longitude.toFixed(4))];
};

function ChangeView({ center, bounds }) {
    const map = useMap();
    useEffect(() => {
        if (bounds && bounds.length > 0) {
            map.fitBounds(bounds, { padding: [50, 50] });
        } else if (center) {
            map.setView(center, map.getZoom() || 13);
        }
    }, [center, bounds, map]);
    return null;
}

const TrackingMap = ({ latitude, longitude, locationDesc, originStr, destStr }) => {
    const originCoords = useMemo(() => getCoordinates(originStr), [originStr]);
    const destCoords = useMemo(() => getCoordinates(destStr), [destStr]);
    const routeCoordinates = useMemo(
        () => originCoords && destCoords ? [originCoords, destCoords] : [],
        [originCoords, destCoords]
    );
    const hasLivePosition = latitude !== null && latitude !== undefined && longitude !== null && longitude !== undefined;
    const position = hasLivePosition ? [Number(latitude), Number(longitude)] : originCoords;

    const [osrmRoute, setOsrmRoute] = useState([]);

    // Fetch realistic road route using OSRM
    useEffect(() => {
        if (routeCoordinates.length === 2) {
            const fetchRoute = async () => {
                try {
                    const [lat1, lon1] = routeCoordinates[0];
                    const [lat2, lon2] = routeCoordinates[1];
                    const res = await fetch(`https://router.project-osrm.org/route/v1/driving/${lon1},${lat1};${lon2},${lat2}?overview=full&geometries=geojson`);
                    const data = await res.json();
                    if (data.routes && data.routes.length > 0) {
                        const coords = data.routes[0].geometry.coordinates.map(c => [c[1], c[0]]);
                        setOsrmRoute(coords);
                    } else {
                        setOsrmRoute(routeCoordinates);
                    }
                } catch (e) {
                    console.error("OSRM Routing Error:", e);
                    setOsrmRoute(routeCoordinates);
                }
            };
            fetchRoute();
        } else {
            setOsrmRoute([]);
        }
    }, [routeCoordinates]);

    const mapCenter = position || originCoords || [20.5937, 78.9629];
    const mapBounds = routeCoordinates.length > 0 ? routeCoordinates.concat(position ? [position] : []) : null;

    return (
        <MapContainer 
            center={mapCenter} 
            zoom={position ? 14 : 5} 
            style={{ height: '100%', width: '100%', borderRadius: '12px', background: '#1e293b' }}
        >
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            
            <ChangeView center={mapCenter} bounds={mapBounds} />

            {/* Draw Planned Route - Uses realistic road geometries if available */}
            {osrmRoute.length > 0 ? <Polyline positions={osrmRoute} color="#6366f1" weight={4} dashArray="10, 10" /> : null}
            
            {routeCoordinates.length === 2 ? (
                <Marker position={originCoords} icon={originIcon}>
                    <Popup><span>Start: {originStr}</span></Popup>
                </Marker>
            ) : null}

            {routeCoordinates.length === 2 ? (
                <Marker position={destCoords} icon={destinationIcon}>
                    <Popup><span>End: {destStr}</span></Popup>
                </Marker>
            ) : null}

            {position ? (
                <Marker position={position} icon={truckIcon}>
                    <Popup>
                        <div>
                            <strong>{hasLivePosition ? 'Current Carrier Location' : 'Awaiting GPS at Start Point'}</strong> <br />
                            {locationDesc || 'Carrier location will update here'}
                        </div>
                    </Popup>
                </Marker>
            ) : null}
        </MapContainer>
    );
};

export default TrackingMap;
