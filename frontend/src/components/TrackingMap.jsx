import React from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix for default marker icons in Leaflet + React
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: markerIcon2x,
    iconUrl: markerIcon,
    shadowUrl: markerShadow,
});

// Component to dynamically update map center
function ChangeView({ center }) {
    const map = useMap();
    map.setView(center, map.getZoom());
    return null;
}

const TrackingMap = ({ latitude, longitude, locationDesc }) => {
    const position = [latitude || 20.5937, longitude || 78.9629]; // Default to India center

    const customIcon = new L.Icon({
        iconUrl: 'https://cdn-icons-png.flaticon.com/512/3063/3063822.png', // Truck icon
        iconSize: [38, 38],
        iconAnchor: [19, 38],
        popupAnchor: [0, -38]
    });

    return (
        <MapContainer 
            center={position} 
            zoom={13} 
            style={{ height: '100%', width: '100%', borderRadius: '12px' }}
        >
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {latitude && longitude && (
                <>
                    <ChangeView center={position} />
                    <Marker position={position} icon={customIcon}>
                        <Popup>
                            <strong>Live Location</strong> <br />
                            {locationDesc || 'In Transit'}
                        </Popup>
                    </Marker>
                </>
            )}
        </MapContainer>
    );
};

export default TrackingMap;
