package error.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 */

@Getter
@RequiredArgsConstructor
public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    MISC("Misc"),
    CONFIGS("Configs"),
    FRIENDS("Friends");

    private final String displayName;
}