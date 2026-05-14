import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap, Polyline, FeatureGroup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix for default marker icons using CDN links
const defaultIcon = new L.Icon({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
});

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
    'hyderabad': [17.3850, 78.4867]
};

const getCoordinates = (cityStr) => {
    if (!cityStr || typeof cityStr !== 'string') return null;
    const key = cityStr.toLowerCase().trim();
    return MOCK_COORDS[key] || null;
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
    // Current Live Position
    const position = latitude && longitude ? [latitude, longitude] : null;

    // Route calculation
    const originCoords = getCoordinates(originStr);
    const destCoords = getCoordinates(destStr);
    const routeCoordinates = originCoords && destCoords ? [originCoords, destCoords] : [];

    // Map Center & Bounds logic
    const mapCenter = position || originCoords || [20.5937, 78.9629];
    const mapBounds = routeCoordinates.length > 0 && !position ? routeCoordinates : null;

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

            {/* Draw Planned Route - Flattened completely to avoid FeatureGroup context bugs */}
            {routeCoordinates.length === 2 ? <Polyline positions={routeCoordinates} color="#6366f1" weight={4} dashArray="10, 10" /> : null}
            
            {routeCoordinates.length === 2 ? (
                <Marker position={originCoords} icon={defaultIcon}>
                    <Popup><span>Origin: {originStr}</span></Popup>
                </Marker>
            ) : null}

            {routeCoordinates.length === 2 ? (
                <Marker position={destCoords} icon={defaultIcon}>
                    <Popup><span>Destination: {destStr}</span></Popup>
                </Marker>
            ) : null}

            {/* Draw Live Truck */}
            {position ? (
                <Marker position={position} icon={truckIcon}>
                    <Popup>
                        <div>
                            <strong>Live Location</strong> <br />
                            {locationDesc || 'In Transit'}
                        </div>
                    </Popup>
                </Marker>
            ) : null}
        </MapContainer>
    );
};

export default TrackingMap;
