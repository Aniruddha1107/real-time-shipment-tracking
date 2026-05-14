const KNOWN_COORDS = {
  "new york": [40.7128, -74.0060],
  "los angeles": [34.0522, -118.2437],
  chicago: [41.8781, -87.6298],
  houston: [29.7604, -95.3698],
  mumbai: [19.0760, 72.8777],
  delhi: [28.7041, 77.1025],
  bangalore: [12.9716, 77.5946],
  bengaluru: [12.9716, 77.5946],
  hyderabad: [17.3850, 78.4867],
  solapur: [17.6599, 75.9064],
  salapur: [17.6599, 75.9064],
  pune: [18.5204, 73.8567],
  begampet: [17.4447, 78.4664],
  nampally: [17.3847, 78.4682],
  nampalli: [17.3847, 78.4682],
  mangaluru: [12.9141, 74.8560],
  mangalore: [12.9141, 74.8560]
};

const memoryCache = new Map();

const normalizePlace = (place) => {
  if (!place || typeof place !== "string") return "";
  return place.trim().toLowerCase();
};

const parseCoords = (value) => {
  const match = String(value || "").match(/(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)/);
  if (!match) return null;

  const lat = Number(match[1]);
  const lng = Number(match[2]);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null;

  return [lat, lng];
};

export const getKnownCoordinates = (place) => {
  const directCoords = parseCoords(place);
  if (directCoords) return directCoords;

  const key = normalizePlace(place).split(",")[0].trim();
  return KNOWN_COORDS[key] || null;
};

export const geocodeLocation = async (place) => {
  const key = normalizePlace(place);
  if (!key) return null;

  const known = getKnownCoordinates(place);
  if (known) return known;

  if (memoryCache.has(key)) return memoryCache.get(key);

  try {
    const cached = localStorage.getItem(`geo:${key}`);
    if (cached) {
      const coords = JSON.parse(cached);
      memoryCache.set(key, coords);
      return coords;
    }
  } catch {
    // Ignore storage failures; geocoding can still proceed.
  }

  const params = new URLSearchParams({
    q: place,
    format: "json",
    limit: "1"
  });

  const response = await fetch(`https://nominatim.openstreetmap.org/search?${params.toString()}`, {
    headers: {
      Accept: "application/json"
    }
  });

  if (!response.ok) return null;

  const results = await response.json();
  if (!Array.isArray(results) || results.length === 0) return null;

  const coords = [Number(results[0].lat), Number(results[0].lon)];
  if (!Number.isFinite(coords[0]) || !Number.isFinite(coords[1])) return null;

  memoryCache.set(key, coords);
  try {
    localStorage.setItem(`geo:${key}`, JSON.stringify(coords));
  } catch {
    // Ignore storage quota/private mode failures.
  }

  return coords;
};
