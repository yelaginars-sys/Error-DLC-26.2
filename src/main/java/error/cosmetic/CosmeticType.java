package error.cosmetic;

public enum CosmeticType {
    WINGS("Крылья"),
    HAT("Шляпа"),
    CAPE("Плащ"),
    BADGE("Значок"),
    AURA("Аура");

    private final String displayName;

    CosmeticType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
