package dev.syntrix.clienttest.client.visual;

import java.util.List;

public final class VisualCatalog {
    public enum Kind { TOGGLE, NUMBER, CHOICE }
    public record Setting(String id, String label, Kind kind, double initial, double min, double max,
                          double step, List<String> choices) {
        public boolean valid(double value) {
            return Double.isFinite(value) && value >= min && value <= max
                    && (kind == Kind.NUMBER || value == Math.rint(value));
        }
    }
    public record Definition(String id, String name, String description, List<Setting> settings) {}
    private VisualCatalog() {}
}
