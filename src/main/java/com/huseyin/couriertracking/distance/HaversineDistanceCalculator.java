package com.huseyin.couriertracking.distance;

public class HaversineDistanceCalculator implements DistanceCalculator{

    static final double EARTH_RADIUS = 6371008.8;

    @Override
    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians((lat2 - lat1));
        double dLng = Math.toRadians((lng2 - lng1));

        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = haversine(dLat) + Math.cos(lat1) * Math.cos(lat2) * haversine(dLng);
        a = Math.min(1.0, a); //floating point can round over 1.00
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    double haversine(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }
}
