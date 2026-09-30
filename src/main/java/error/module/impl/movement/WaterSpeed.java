package error.module.impl.movement;

import error.module.Category;
import error.module.Module;

/**
 * Create by daun kvass
 */
public class WaterSpeed extends Module {
    public static WaterSpeed INSTANCE;
    public WaterSpeed(){super("WaterSpeed","Хз легитные вроде много где ще робят", Category.MOVEMENT);  INSTANCE = this;}
}
