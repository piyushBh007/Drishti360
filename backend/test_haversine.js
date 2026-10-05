function haversineDistanceMeters(lat1, lon1, lat2, lon2) {
    const earthRadius = 6371000.0; // meters
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadius * c;
}

console.log("Jaipur (correct):", haversineDistanceMeters(37.421998, -122.084000, 26.9124, 75.7873));
console.log("Jodhpur (correct):", haversineDistanceMeters(37.421998, -122.084000, 26.2389, 73.0243));

// What if lat and lon are swapped for NGOs?
console.log("Swapped Jaipur:", haversineDistanceMeters(37.421998, -122.084000, 75.7873, 26.9124));
console.log("Swapped Jodhpur:", haversineDistanceMeters(37.421998, -122.084000, 73.0243, 26.2389));

// What if lat and lon are swapped for inspector?
console.log("Swapped Inspector Jaipur:", haversineDistanceMeters(-122.084000, 37.421998, 26.9124, 75.7873));
console.log("Swapped Inspector Jodhpur:", haversineDistanceMeters(-122.084000, 37.421998, 26.2389, 73.0243));

// What if BOTH swapped?
console.log("Swapped Both Jaipur:", haversineDistanceMeters(-122.084000, 37.421998, 75.7873, 26.9124));
console.log("Swapped Both Jodhpur:", haversineDistanceMeters(-122.084000, 37.421998, 73.0243, 26.2389));
