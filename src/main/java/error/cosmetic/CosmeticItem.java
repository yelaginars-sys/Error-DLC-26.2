package error.cosmetic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CosmeticItem {
    private String id;
    private String name;
    private CosmeticType type;
    private boolean enabled;
    private int color;
    private String texturePath;
}
