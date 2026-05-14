import React, { useEffect, useMemo, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap, Polyline } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { geocodeLocation, getKnownCoordinates } from '../utils/geocoding';

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

export const getCoordinates = getKnownCoordinates;

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
    const [originCoords, setOriginCoords] = useState(() => getKnownCoordinates(originStr));
    const [destCoords, setDestCoords] = useState(() => getKnownCoordinates(destStr));
    const [geocodeStatus, setGeocodeStatus] = useState('');

    useEffect(() => {
        let isMounted = true;
        setOriginCoords(getKnownCoordinates(originStr));
        setDestCoords(getKnownCoordinates(destStr));
        setGeocodeStatus('Resolving route points...');

        Promise.all([geocodeLocation(originStr), geocodeLocation(destStr)])
            .then(([origin, destination]) => {
                if (!isMounted) return;
                setOriginCoords(origin);
                setDestCoords(destination);
                setGeocodeStatus(origin && destination ? '' : 'Could not resolve one or more route points');
            })
            .catch(() => {
                if (isMounted) setGeocodeStatus('Could not resolve route points');
            });

        return () => {
            isMounted = false;
        };
    }, [originStr, destStr]);

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

            {geocodeStatus ? (
                <div className="leaflet-top leaflet-left" style={{ marginTop: 10, marginLeft: 50 }}>
                    <div className="leaflet-control" style={{
                        background: 'white',
                        padding: '8px 10px',
                        borderRadius: 6,
                        boxShadow: '0 8px 20px rgba(15, 23, 42, 0.18)',
                        fontSize: 13,
                        fontWeight: 700
                    }}>
                        {geocodeStatus}
                    </div>
                </div>
            ) : null}
        </MapContainer>
    );
};

export default TrackingMap;
