package error.cosmetic;

import lombok.Getter;
import error.util.client.clients.ColorUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CosmeticsManager {
    private static final CosmeticsManager INSTANCE = new CosmeticsManager();
    private final List<CosmeticItem> cosmetics = new ArrayList<>();

    private CosmeticsManager() {
        initDefaultCosmetics();
    }

    public static CosmeticsManager getInstance() {
        return INSTANCE;
    }

    private void initDefaultCosmetics() {
        cosmetics.add(new CosmeticItem("dragon_wings", "Драконьи крылья", CosmeticType.WINGS, true, ColorUtil.rgba(235, 145, 225, 255), "textures/cosmetics/wings.png"));
        cosmetics.add(new CosmeticItem("halo_hat", "Нимб", CosmeticType.HAT, false, ColorUtil.rgba(255, 215, 0, 255), "textures/cosmetics/halo.png"));
        cosmetics.add(new CosmeticItem("error_cape", "Плащ Error DLC", CosmeticType.CAPE, true, ColorUtil.rgba(140, 80, 180, 255), "textures/cosmetics/cape.png"));
        cosmetics.add(new CosmeticItem("verified_badge", "Значок Вертификата", CosmeticType.BADGE, true, ColorUtil.rgba(85, 255, 255, 255), "textures/cosmetics/badge.png"));
    }

    public CosmeticItem getCosmetic(String id) {
        for (CosmeticItem item : cosmetics) {
            if (item.getId().equalsIgnoreCase(id)) {
                return item;
            }
        }
        return null;
    }

    public boolean isCosmeticActive(CosmeticType type) {
        for (CosmeticItem item : cosmetics) {
            if (item.getType() == type && item.isEnabled()) {
                return true;
            }
        }
        return false;
    }
}
