package error.mixin.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Prevents ShaderManager NullPointerExceptions when GLSL shader imports (#moj_import)
 * reference missing files or namespaces (e.g. skycore:, danq:, zenith:).
 */
@Mixin(targets = "net.minecraft.client.renderer.ShaderManager$1")
public class ShaderManagerMixin {

    @WrapMethod(method = "applyImport")
    private String safeApplyImport(boolean isRelative, String name, Operation<String> original) {
        try {
            String result = original.call(isRelative, name);
            return result != null ? result : "// Missing import: " + name + "\n";
        } catch (Throwable t) {
            System.err.println("[ErrorDLC/ShaderManager] Suppressed shader import crash for '" + name + "': " + t.getMessage());
            return "// Missing import: " + name + "\n";
        }
    }
}
