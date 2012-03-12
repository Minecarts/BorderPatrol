package com.minecarts.borderpatrol;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.permissions.Permissible;

public class PermissibleBoundingCircle extends BoundingCircle {
    protected final String permission;
    
    public PermissibleBoundingCircle(String permission, int radius) {
        super(radius);
        this.permission = permission;
    }
    
    public PermissibleBoundingCircle(String permission, int radius, int x, int z) {
        super(radius, x, z);
        this.permission = permission;
    }
    
    public boolean canContain(Entity entity) {
        if(entity instanceof Permissible) {
            if(!((Permissible) entity).hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean contains(Entity entity) {
        return contains(entity, entity.getLocation());
    }
    
    public boolean contains(Entity entity, Location location) {
        return canContain(entity) && contains(location);
    }
}
