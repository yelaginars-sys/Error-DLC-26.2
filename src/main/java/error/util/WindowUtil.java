package error.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.lwjgl.glfw.GLFWNativeWin32;

public class WindowUtil {

    private interface DwmLib extends Library {
        DwmLib INSTANCE = Native.load("dwmapi", DwmLib.class);
        int DwmSetWindowAttribute(Pointer hwnd, int dwAttribute, IntByReference pvAttribute, int cbAttribute);
    }

    private static boolean darkTitleBarApplied = false;

    public static void applyDarkTitleBar(long windowHandle) {
        if (darkTitleBarApplied || windowHandle == 0) return;
        darkTitleBarApplied = true;

        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                long hwndPtr = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
                if (hwndPtr != 0) {
                    Pointer hwnd = new Pointer(hwndPtr);
                    IntByReference pvAttribute = new IntByReference(1);
                    DwmLib.INSTANCE.DwmSetWindowAttribute(hwnd, 20, pvAttribute, 4);
                    DwmLib.INSTANCE.DwmSetWindowAttribute(hwnd, 19, pvAttribute, 4);
                }
            } catch (Throwable t) {
                // Fallback
            }
        }
    }
}
