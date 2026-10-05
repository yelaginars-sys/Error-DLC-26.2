package error.cosmetic;

import lombok.Getter;
import error.util.client.clients.ColorUtil;
import error.module.impl.render.CustomModels;

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
        // --- 3D PLAYER MODELS ---
        cosmetics.add(new CosmeticItem("model_rabbit", "Модель Кролика", CosmeticType.MODEL, false, ColorUtil.rgba(240, 240, 240, 255), "CustomModels"));
        cosmetics.add(new CosmeticItem("model_chicken", "Модель Курицы", CosmeticType.MODEL, false, ColorUtil.rgba(255, 215, 0, 255), "CustomModels"));
        cosmetics.add(new CosmeticItem("model_verity", "Модель Verity", CosmeticType.MODEL, false, ColorUtil.rgba(200, 100, 255, 255), "CustomModels"));
        cosmetics.add(new CosmeticItem("model_amogus", "Модель Amogus", CosmeticType.MODEL, false, ColorUtil.rgba(230, 40, 40, 255), "CustomModels"));
        cosmetics.add(new CosmeticItem("model_freddy", "Модель Фредди", CosmeticType.MODEL, false, ColorUtil.rgba(139, 69, 19, 255), "CustomModels"));
        cosmetics.add(new CosmeticItem("model_red_demon", "Красный Демон", CosmeticType.MODEL, false, ColorUtil.rgba(255, 50, 50, 255), "CustomModels"));
        cosmetics.add(new CosmeticItem("model_white_demon", "Белый Демон", CosmeticType.MODEL, false, ColorUtil.rgba(245, 245, 255, 255), "CustomModels"));

        // --- WINGS ---
        cosmetics.add(new CosmeticItem("wings_draco", "Крылья Дракона", CosmeticType.WINGS, false, ColorUtil.rgba(235, 145, 225, 255), "cosmetics/wings/draco"));
        cosmetics.add(new CosmeticItem("wings_angel", "Ангельские Крылья", CosmeticType.WINGS, false, ColorUtil.rgba(255, 255, 255, 255), "cosmetics/wings/angel"));
        cosmetics.add(new CosmeticItem("wings_archangel", "Крылья Архангела", CosmeticType.WINGS, false, ColorUtil.rgba(255, 215, 0, 255), "cosmetics/wings/archangel"));
        cosmetics.add(new CosmeticItem("wings_blackhole", "Крылья Черная Дыра", CosmeticType.WINGS, false, ColorUtil.rgba(130, 40, 220, 255), "cosmetics/wings/blackhole"));
        cosmetics.add(new CosmeticItem("wings_butterfly", "Крылья Бабочки", CosmeticType.WINGS, false, ColorUtil.rgba(255, 105, 180, 255), "cosmetics/wings/butterfly"));
        cosmetics.add(new CosmeticItem("wings_frost", "Ледяные Крылья", CosmeticType.WINGS, false, ColorUtil.rgba(100, 200, 255, 255), "cosmetics/wings/frost"));
        cosmetics.add(new CosmeticItem("wings_steampunk", "Стимпанк Крылья", CosmeticType.WINGS, false, ColorUtil.rgba(210, 140, 50, 255), "cosmetics/wings/steampunk"));
        cosmetics.add(new CosmeticItem("wings_techno", "Техно Крылья", CosmeticType.WINGS, false, ColorUtil.rgba(0, 230, 200, 255), "cosmetics/wings/techno"));
        cosmetics.add(new CosmeticItem("wings_ghoul", "Крылья Гуля", CosmeticType.WINGS, false, ColorUtil.rgba(220, 20, 60, 255), "cosmetics/wings/pulse_wings_124"));

        // --- HATS ---
        cosmetics.add(new CosmeticItem("hat_angel_halo", "Ангельский Нимб", CosmeticType.HAT, false, ColorUtil.rgba(255, 223, 0, 255), "cosmetics/hats/angel_halo"));
        cosmetics.add(new CosmeticItem("hat_bear", "Шапка Медведя", CosmeticType.HAT, false, ColorUtil.rgba(160, 82, 45, 255), "cosmetics/hats/bear"));
        cosmetics.add(new CosmeticItem("hat_frog", "Шапка Лягушки", CosmeticType.HAT, false, ColorUtil.rgba(50, 205, 50, 255), "cosmetics/hats/frog"));
        cosmetics.add(new CosmeticItem("hat_pilot", "Шлем Пилота", CosmeticType.HAT, false, ColorUtil.rgba(112, 128, 144, 255), "cosmetics/hats/pilot"));
        cosmetics.add(new CosmeticItem("hat_capybara", "Шапка Капибары", CosmeticType.HAT, false, ColorUtil.rgba(180, 120, 60, 255), "cosmetics/hats/capybara"));

        // --- MASKS ---
        cosmetics.add(new CosmeticItem("mask_angry", "Злая Маска", CosmeticType.MASK, false, ColorUtil.rgba(255, 69, 0, 255), "cosmetics/masks/angry"));
        cosmetics.add(new CosmeticItem("mask_clown", "Маска Клоуна", CosmeticType.MASK, false, ColorUtil.rgba(255, 20, 147, 255), "cosmetics/masks/clown"));
        cosmetics.add(new CosmeticItem("mask_shades", "Черные Очки", CosmeticType.MASK, false, ColorUtil.rgba(30, 30, 30, 255), "cosmetics/masks/shades"));
        cosmetics.add(new CosmeticItem("mask_wink", "Маска Смайл", CosmeticType.MASK, false, ColorUtil.rgba(255, 215, 0, 255), "cosmetics/masks/wink"));

        // --- BACKPACKS ---
        cosmetics.add(new CosmeticItem("backpack_adidas", "Рюкзак Adidas", CosmeticType.BACKPACK, false, ColorUtil.rgba(20, 20, 20, 255), "cosmetics/backpacks/adidas"));
        cosmetics.add(new CosmeticItem("backpack_gucci", "Рюкзак Gucci", CosmeticType.BACKPACK, false, ColorUtil.rgba(34, 139, 34, 255), "cosmetics/backpacks/gucci"));
        cosmetics.add(new CosmeticItem("backpack_louis_vuitton", "Рюкзак Louis Vuitton", CosmeticType.BACKPACK, false, ColorUtil.rgba(139, 69, 19, 255), "cosmetics/backpacks/lui_vuitton"));
        cosmetics.add(new CosmeticItem("backpack_nike", "Рюкзак Nike", CosmeticType.BACKPACK, false, ColorUtil.rgba(220, 20, 60, 255), "cosmetics/backpacks/nike"));
        cosmetics.add(new CosmeticItem("backpack_supreme", "Рюкзак Supreme", CosmeticType.BACKPACK, false, ColorUtil.rgba(235, 30, 30, 255), "cosmetics/backpacks/supreme"));

        // --- PETS ---
        cosmetics.add(new CosmeticItem("pet_dragon", "Дракончик", CosmeticType.PET, false, ColorUtil.rgba(148, 0, 211, 255), "cosmetics/pets/dragon"));
        cosmetics.add(new CosmeticItem("pet_capybara", "Капибара", CosmeticType.PET, false, ColorUtil.rgba(184, 115, 51, 255), "cosmetics/pets/capybara"));
        cosmetics.add(new CosmeticItem("pet_axolotl", "Аксолотль", CosmeticType.PET, false, ColorUtil.rgba(255, 182, 193, 255), "cosmetics/pets/axolotl"));
        cosmetics.add(new CosmeticItem("pet_panda", "Панда", CosmeticType.PET, false, ColorUtil.rgba(240, 240, 240, 255), "cosmetics/pets/panda"));
        cosmetics.add(new CosmeticItem("pet_patrick", "Патрик", CosmeticType.PET, false, ColorUtil.rgba(255, 105, 180, 255), "cosmetics/pets/patrick"));
        cosmetics.add(new CosmeticItem("pet_spongebob", "Губка Боб", CosmeticType.PET, false, ColorUtil.rgba(255, 255, 0, 255), "cosmetics/pets/spongebob"));
        cosmetics.add(new CosmeticItem("pet_creeper", "Крипер", CosmeticType.PET, false, ColorUtil.rgba(60, 180, 60, 255), "cosmetics/pets/creeper"));

        // --- CARS & BIKES ---
        cosmetics.add(new CosmeticItem("car_pitbike", "Питбайк KAYO", CosmeticType.CAR, false, ColorUtil.rgba(50, 205, 50, 255), "cosmetics/cars/pitbike"));
        cosmetics.add(new CosmeticItem("car_uaz", "УАЗ-452 Буханка", CosmeticType.CAR, false, ColorUtil.rgba(85, 107, 47, 255), "cosmetics/cars/uaz"));
        cosmetics.add(new CosmeticItem("car_bmw_m5", "BMW M5 CS", CosmeticType.CAR, false, ColorUtil.rgba(0, 102, 204, 255), "cosmetics/cars/bmw_m5"));
        cosmetics.add(new CosmeticItem("car_g63", "Mercedes G63 AMG", CosmeticType.CAR, false, ColorUtil.rgba(30, 30, 30, 255), "cosmetics/cars/g63"));
        cosmetics.add(new CosmeticItem("car_porsche_911", "Porsche 911 GT3", CosmeticType.CAR, false, ColorUtil.rgba(255, 165, 0, 255), "cosmetics/cars/porsche_911"));
        cosmetics.add(new CosmeticItem("car_lambo", "Lamborghini Aventador", CosmeticType.CAR, false, ColorUtil.rgba(255, 215, 0, 255), "cosmetics/cars/lambo"));
        cosmetics.add(new CosmeticItem("car_cybertruck", "Tesla Cybertruck", CosmeticType.CAR, false, ColorUtil.rgba(180, 180, 190, 255), "cosmetics/cars/cybertruck"));
        cosmetics.add(new CosmeticItem("car_bugatti", "Bugatti Chiron", CosmeticType.CAR, false, ColorUtil.rgba(40, 120, 220, 255), "cosmetics/cars/bugatti"));

        // --- CAPES & BADGES ---
        cosmetics.add(new CosmeticItem("error_cape", "Плащ Error DLC", CosmeticType.CAPE, false, ColorUtil.rgba(140, 80, 180, 255), "textures/cosmetics/cape.png"));
        cosmetics.add(new CosmeticItem("verified_badge", "Значок Верификата", CosmeticType.BADGE, false, ColorUtil.rgba(85, 255, 255, 255), "textures/cosmetics/badge.png"));
    }

    public void onToggleCosmetic(CosmeticItem item) {
        item.setEnabled(!item.isEnabled());

        if (item.isEnabled()) {
            for (CosmeticItem c : cosmetics) {
                if (c.getType() == item.getType() && c != item) {
                    c.setEnabled(false);
                }
            }
        }

        if (item.getType() == CosmeticType.MODEL) {
            if (item.isEnabled()) {
                String modelName = mapCosmeticIdToModel(item.getId());
                if (CustomModels.INSTANCE != null) {
                    CustomModels.INSTANCE.model.setValue(modelName);
                    CustomModels.INSTANCE.setEnabled(true);
                }
            } else {
                if (CustomModels.INSTANCE != null) {
                    CustomModels.INSTANCE.model.setValue(CustomModels.NONE);
                }
            }
        }
    }

    private String mapCosmeticIdToModel(String id) {
        return switch (id) {
            case "model_rabbit" -> CustomModels.RABBIT;
            case "model_chicken" -> CustomModels.CHICKEN;
            case "model_verity" -> CustomModels.VERITY;
            case "model_amogus" -> CustomModels.AMOGUS;
            case "model_freddy" -> CustomModels.FREDDY;
            case "model_red_demon" -> CustomModels.RED_DEMON;
            case "model_white_demon" -> CustomModels.WHITE_DEMON;
            default -> CustomModels.NONE;
        };
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

    public CosmeticItem getEquipped(CosmeticType type) {
        for (CosmeticItem item : cosmetics) {
            if (item.getType() == type && item.isEnabled()) {
                return item;
            }
        }
        return null;
    }

    public List<CosmeticItem> getEquippedCosmetics() {
        List<CosmeticItem> list = new ArrayList<>();
        for (CosmeticItem item : cosmetics) {
            if (item.isEnabled()) {
                list.add(item);
            }
        }
        return list;
    }
}
