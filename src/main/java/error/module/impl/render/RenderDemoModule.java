package error.module.impl.render;

import error.event.EventTarget;
import error.event.list.EventDisplay;
import error.module.Category;
import error.module.Module;
import error.util.display.demo.RenderDemo;

public class RenderDemoModule extends Module {
    public static RenderDemoModule INSTANCE;

    public RenderDemoModule() {
        super("RenderDemo", "Демонстрация 2D Liquid Glass рендеринга", Category.RENDER);
        INSTANCE = this;
    }

    @EventTarget
    public void onDisplay(EventDisplay event) {
        RenderDemo.render(event.graphics());
    }
}
