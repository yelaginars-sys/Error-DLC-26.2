package dev.syntrix.clienttest.client.combat.meow.util;
/** Scoped interaction context retained for modules sharing native interaction hooks. */
public final class InteractionContextGuard {
    private static final ThreadLocal<Integer> depth=ThreadLocal.withInitial(()->0);
    public static void enter() { depth.set(depth.get()+1); }
    public static void exit() { if(depth.get()<=1)depth.remove();else depth.set(depth.get()-1); }
    public static boolean active() { return depth.get()>0; }
    private InteractionContextGuard() {}
}
