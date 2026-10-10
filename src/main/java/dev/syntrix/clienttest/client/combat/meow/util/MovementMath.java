package dev.syntrix.clienttest.client.combat.meow.util;
public final class MovementMath {
    // Recovered operation001: heading of the eight discrete movement directions.
    public static double direction(float yaw,float forward,float sideways) {
        if(forward<0)yaw+=180;
        float factor=forward<0?-.5F:forward>0?.5F:1;
        if(sideways>0)yaw-=90*factor;
        if(sideways<0)yaw+=90*factor;
        return Math.toRadians(yaw);
    }
}
