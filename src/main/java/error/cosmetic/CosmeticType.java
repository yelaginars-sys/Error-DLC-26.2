package error.cosmetic;

public enum CosmeticType {
    ALL("Все"),
    MODEL("3D Модели"),
    WINGS("Крылья"),
    HAT("Шапки"),
    MASK("Маски"),
    BACKPACK("Рюкзаки"),
    PET("Питомцы"),
    CAR("Машины"),
    CAPE("Плащи"),
    BADGE("Значки"),
    AURA("Аура");

    private final String displayName;

    CosmeticType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
