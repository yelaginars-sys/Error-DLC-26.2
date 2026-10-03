package error.util.client.persiki;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 */
public final class KeyUtil {
    public static final int UNBOUND = -1;
    public static final int MOUSE_FLAG = 1 << 30;

    public static int fromMouseButton(int button) {
        return MOUSE_FLAG | button;
    }

    public static boolean isMouseButton(int code) {
        return code != UNBOUND && (code & MOUSE_FLAG) != 0;
    }

    public static int toMouseButton(int code) {
        return code & ~MOUSE_FLAG;
    }

    public static boolean isKeyDown(int code) {
        if (code == UNBOUND || code == 0 || code == GLFW.GLFW_KEY_UNKNOWN) return false;
        long window = Minecraft.getInstance().getWindow().handle();

        if (isMouseButton(code)) {
            int btn = toMouseButton(code);
            return GLFW.glfwGetMouseButton(window, btn) == GLFW.GLFW_PRESS;
        } else {
            return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
        }
    }

    public static String getKeyName(int code) {
        if (code == UNBOUND || code == 0 || code == GLFW.GLFW_KEY_UNKNOWN) return "NONE";

        if (isMouseButton(code)) {
            int btn = toMouseButton(code);
            return switch (btn) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "M1";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "M2";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "M3";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
                case GLFW.GLFW_MOUSE_BUTTON_6 -> "M6";
                case GLFW.GLFW_MOUSE_BUTTON_7 -> "M7";
                case GLFW.GLFW_MOUSE_BUTTON_8 -> "M8";
                default -> "M" + (btn + 1);
            };
        }

        if (code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (code - GLFW.GLFW_KEY_A)));
        }
        if (code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (code - GLFW.GLFW_KEY_0)));
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) {
            return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        }
        if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) {
            return "NUM" + (code - GLFW.GLFW_KEY_KP_0);
        }

        return switch (code) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BSPC";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "`";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "]";
            case GLFW.GLFW_KEY_BACKSLASH -> "\\";
            case GLFW.GLFW_KEY_SEMICOLON -> ";";
            case GLFW.GLFW_KEY_APOSTROPHE -> "'";
            case GLFW.GLFW_KEY_COMMA -> ",";
            case GLFW.GLFW_KEY_PERIOD -> ".";
            case GLFW.GLFW_KEY_SLASH -> "/";
            default -> {
                String name = GLFW.glfwGetKeyName(code, 0);
                if (name != null && !name.isBlank()) {
                    name = name.toUpperCase();
                    // Replace Cyrillic letters with English equivalents if returned by OS
                    if ("Ф".equals(name)) yield "A";
                    if ("И".equals(name)) yield "B";
                    if ("С".equals(name)) yield "C";
                    if ("В".equals(name)) yield "D";
                    if ("У".equals(name)) yield "E";
                    if ("А".equals(name)) yield "F";
                    if ("П".equals(name)) yield "G";
                    if ("Р".equals(name)) yield "H";
                    if ("Ш".equals(name)) yield "I";
                    if ("О".equals(name)) yield "J";
                    if ("Л".equals(name)) yield "K";
                    if ("Д".equals(name)) yield "L";
                    if ("Ь".equals(name)) yield "M";
                    if ("Т".equals(name)) yield "N";
                    if ("Щ".equals(name)) yield "O";
                    if ("З".equals(name)) yield "P";
                    if ("Й".equals(name)) yield "Q";
                    if ("К".equals(name)) yield "R";
                    if ("Ы".equals(name)) yield "S";
                    if ("Е".equals(name)) yield "T";
                    if ("Г".equals(name)) yield "U";
                    if ("М".equals(name)) yield "V";
                    if ("Ц".equals(name)) yield "W";
                    if ("Ч".equals(name)) yield "X";
                    if ("Н".equals(name)) yield "Y";
                    if ("Я".equals(name)) yield "Z";
                    yield name;
                }
                yield "KEY " + code;
            }
        };
    }
}