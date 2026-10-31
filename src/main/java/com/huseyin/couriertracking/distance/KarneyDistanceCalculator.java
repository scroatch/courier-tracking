package com.huseyin.couriertracking.distance;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicMask;

public class KarneyDistanceCalculator implements DistanceCalculator {

    @Override
    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        return Geodesic.WGS84
                .Inverse(lat1, lng1, lat2, lng2, GeodesicMask.DISTANCE)
                .s12;
    }
}
