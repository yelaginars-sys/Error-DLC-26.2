package error.cosmetic;

public enum CosmeticType {
    WINGS("Крылья"),
    HAT("Шляпа"),
    MASK("Маска"),
    BACKPACK("Рюкзак"),
    PET("Питомец"),
    MODEL("3D Модель"),
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
