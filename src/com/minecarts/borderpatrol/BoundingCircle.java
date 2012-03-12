package com.minecarts.borderpatrol;

import org.bukkit.Location;

public class BoundingCircle {
    protected final int centerX;
    protected final int centerZ;
    protected final int radius;
    protected final long radiusSquared;
    protected final long smallestAllowableDistance;
    
    public BoundingCircle(int radius) {
        this(radius, 0, 0);
    }
    
    public BoundingCircle(int radius, int centerX, int centerZ) {
        this.radius = radius;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radiusSquared = radius * radius;
        this.smallestAllowableDistance = Math.round(Math.sqrt(radiusSquared / 2));
    }
    
    public int getCenterX() {
        return centerX;
    }
    public int getCenterZ() {
        return centerZ;
    }
    public int getRadius() {
        return radius;
    }
    
    
    public boolean contains(Location location) {
        int distanceX = Math.abs(location.getBlockX() - centerX);
        int distanceZ = Math.abs(location.getBlockZ() - centerZ);
        
        if(distanceX < smallestAllowableDistance && distanceZ < smallestAllowableDistance) return true;
        if(distanceX > radius || distanceZ > radius) return false;
        if(distanceX*distanceX + distanceZ*distanceZ > radiusSquared) return false;
        return true;
    }
}
