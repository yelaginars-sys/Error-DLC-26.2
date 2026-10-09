package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.HeaderSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.RotationHandler;
import error.util.player.MoveUtility;
import error.util.render.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CrystalAura with complete logic:
 * - Precise explosion damage computation (armor, toughness, EPF, resistance, difficulty, exposure raycasting)
 * - Auto-Obsidian placement
 * - Anti-Weakness weapon swapping
 * - Anti-Suicide safety checks
 * - Auto-Dig for blocked / burrow targets
 * - Self-Save / Shield protection
 * - Friend ignore check via FriendManager
 * - Legit mode with rotation convergence
 * - Fading 3D box visualizer
 */
public final class CrystalAura extends Module {

    public final SliderSetting targetRange = slider("Радиус цели", 8.0f, 4.0f, 12.0f, 0.5f);
    public final SliderSetting placeRange = slider("Дальность установки", 4.5f, 3.0f, 6.0f, 0.1f);
    public final SliderSetting breakRange = slider("Дальность взрыва", 4.5f, 1.0f, 6.0f, 0.1f);
    public final SliderSetting minTargetDamage = slider("Мин. урон врагу", 6.0f, 1.0f, 20.0f, 0.5f);
    public final SliderSetting minWebDamage = slider("Мин. урон в паутине", 3.0f, 1.0f, 10.0f, 0.5f);
    public final SliderSetting faceplaceHp = slider("Фейсплейс здоровье", 8.0f, 0.0f, 20.0f, 0.5f);
    public final SliderSetting maxSelfDamage = slider("Макс. урон себе", 8.0f, 0.0f, 20.0f, 0.5f);
    public final SliderSetting delay = slider("Задержка", 100.0f, 0.0f, 500.0f, 10.0f);
    public final SliderSetting predictTicks = slider("Предикт тиков", 2.0f, 0.0f, 4.0f, 1.0f);

    public final CheckBox autoObsidian = checkbox("Ставить обсидиан", true);
    public final CheckBox legit = checkbox("Легитный", false);
    public final CheckBox antiWeakness = checkbox("Анти-слабость", true);
    public final CheckBox antiSuicide = checkbox("Анти-суицид", true);
    public final CheckBox autoDig = checkbox("Авто подкоп", true);
    public final CheckBox selfSave = checkbox("Сейвить себя", false);
    public final CheckBox ignoreFriends = checkbox("Игнорировать друзей", true);

    private final HeaderSetting renderHeader = header("Визуализация");
    public final CheckBox render = checkbox("Визуализация", true);
    public final ModeSetting renderType = mode("Режим рендера", "Куб", "Куб", "Плитка").visible(render::getValue);

    private long lastActionTime = 0L;
    private int lastSentSlot = -1;
    private int originalSlot = -1;

    // Digging state
    private BlockPos miningPos = null;
    private BlockPos miningBaseTarget = null;
    private int miningTicks = 0;

    // Shield state
    private BlockPos shieldPos = null;
    private int shieldTicks = 0;

    // Active targets for rendering
    private EndCrystal targetCrystal = null;
    private BlockPos targetActionPos = null;
    private final List<RenderAction> renderActions = new CopyOnWriteArrayList<>();

    public CrystalAura() {
        super("CrystalAura", "Автоматически ставит и взрывает кристаллы", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        resetState();
        if (mc.player != null) {
            lastSentSlot = mc.player.getInventory().getSelectedSlot();
            originalSlot = lastSentSlot;
        }
    }

    @Override
    protected void onDisable() {
        RotationHandler.disengage("Smooth");
        restoreSlot();
        resetState();
    }

    @EventTarget
    public void onPlayerInput(PlayerInputEvent event) {
        if (!inGame() || player() == null) return;
        if (RotationHandler.isActive()) {
            MoveUtility.fixMovement(event, RotationHandler.getFreeYaw());
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (!inGame() || player() == null || mc.level == null || mc.gameMode == null || mc.getConnection() == null) {
            resetState();
            return;
        }

        if (AutoTotem.isSwapping()) {
            resetRotation();
            return;
        }

        if (shieldPos != null && ++shieldTicks > 10) {
            shieldPos = null;
            shieldTicks = 0;
        }

        cleanRenderActions();

        long now = System.currentTimeMillis();
        boolean canAct = (now - lastActionTime) >= delay.getValue().longValue();

        Player target = findTarget();
        if (target == null) {
            resetRotation();
            cancelMining();
            targetCrystal = null;
            targetActionPos = null;
            return;
        }

        // 1. Self-Save / Shield check
        if (selfSave.getValue() && canAct && shieldPos == null) {
            if (trySelfShield(target)) {
                lastActionTime = now;
                return;
            }
        }

        // 2. Explode existing crystal (highest priority)
        EndCrystal bestCrystal = findBestCrystal(target);
        if (bestCrystal != null) {
            targetCrystal = bestCrystal;
            Vec3 crystalVec = bestCrystal.position().add(0, 0.5, 0);
            if (!syncRotation(crystalVec)) return;

            if (canAct) {
                explodeCrystal(bestCrystal);
                lastActionTime = now;
            }
            return;
        } else {
            targetCrystal = null;
        }

        // 3. Auto-Dig if target is blocked / burrowed
        if (autoDig.getValue()) {
            if (handleAutoDig(target, canAct, now)) {
                return;
            }
        }

        // 4. Place crystal on existing obsidian/bedrock
        if (canAct && findCrystalSlot() != -1) {
            PlacementSpot readySpot = findBestPlacement(target, false);
            if (readySpot != null) {
                targetActionPos = readySpot.pos();
                if (!syncRotation(readySpot.lookVec())) return;

                placeCrystal(readySpot);
                lastActionTime = now;
                return;
            }
        }

        // 5. Auto-Obsidian placement
        if (autoObsidian.getValue() && canAct && findObsidianSlot() != -1 && findCrystalSlot() != -1) {
            PlacementSpot obbySpot = findBestPlacement(target, true);
            if (obbySpot != null) {
                targetActionPos = obbySpot.pos();
                if (!syncRotation(obbySpot.lookVec())) return;

                placeObsidian(obbySpot);
                lastActionTime = now;
                return;
            }
        }

        resetRotation();
    }

    private boolean trySelfShield(Player enemy) {
        if (findObsidianSlot() == -1) return false;
        Vec3 diff = enemy.position().subtract(mc.player.position());
        if (Math.abs(diff.y) > 1.2 || diff.horizontalDistance() > (placeRange.getValue() + 1.2)) return false;

        Direction dir = Math.abs(diff.x) > Math.abs(diff.z)
                ? (diff.x > 0 ? Direction.EAST : Direction.WEST)
                : (diff.z > 0 ? Direction.SOUTH : Direction.NORTH);

        BlockPos shieldTarget = mc.player.blockPosition().relative(dir);
        if (!canPlaceBlockAt(shieldTarget)) return false;

        BlockHitResult hit = getPlacementHitResult(shieldTarget);
        if (hit == null) return false;

        if (!syncRotation(hit.getLocation())) return false;

        int obbySlot = findObsidianSlot();
        placeBlock(hit, obbySlot, true);
        shieldPos = shieldTarget;
        shieldTicks = 0;
        return true;
    }

    private boolean handleAutoDig(Player target, boolean canAct, long now) {
        if (miningPos != null) {
            BlockState state = mc.level.getBlockState(miningPos);
            if (state.isAir() || !isBlockInReach(miningPos, placeRange.getValue()) || ++miningTicks > 120) {
                cancelMining();
            } else {
                Vec3 center = Vec3.atCenterOf(miningPos);
                if (!syncRotation(center)) return true;

                int pickSlot = findPickaxeSlot();
                if (pickSlot != -1) switchToSlot(pickSlot);

                mc.gameMode.continueDestroyBlock(miningPos, Direction.UP);
                mc.player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }

        if (canAct && findPickaxeSlot() != -1) {
            BlockPos blockingPos = findObstructingBlock(target);
            if (blockingPos != null && isBlockInReach(blockingPos, placeRange.getValue())) {
                miningPos = blockingPos;
                miningTicks = 0;
                miningBaseTarget = target.blockPosition();

                Vec3 center = Vec3.atCenterOf(miningPos);
                if (!syncRotation(center)) return true;

                int pickSlot = findPickaxeSlot();
                if (pickSlot != -1) switchToSlot(pickSlot);

                mc.gameMode.startDestroyBlock(miningPos, Direction.UP);
                mc.player.swing(InteractionHand.MAIN_HAND);
                lastActionTime = now;
                return true;
            }
        }

        return false;
    }

    private BlockPos findObstructingBlock(Player target) {
        BlockPos head = target.blockPosition().above();
        BlockState headState = mc.level.getBlockState(head);
        if (!headState.isAir() && headState.getDestroySpeed(mc.level, head) >= 0) {
            return head;
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = target.blockPosition().relative(dir);
            BlockState sideState = mc.level.getBlockState(side);
            if (sideState.is(Blocks.COBWEB) || (!sideState.isAir() && sideState.getDestroySpeed(mc.level, side) >= 0 && sideState.getDestroySpeed(mc.level, side) <= 50.0f)) {
                return side;
            }
        }
        return null;
    }

    private void cancelMining() {
        if (miningPos != null && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        miningPos = null;
        miningBaseTarget = null;
        miningTicks = 0;
    }

    private EndCrystal findBestCrystal(Player target) {
        EndCrystal best = null;
        float maxDmg = 0f;
        double maxDistSq = breakRange.getValue() * breakRange.getValue();

        Vec3 eye = mc.player.getEyePosition();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive() || crystal.isRemoved()) continue;
            if (crystal.distanceToSqr(mc.player) > maxDistSq) continue;

            Vec3 crystalPos = crystal.position();
            float targetDmg = calculateCrystalDamage(crystalPos, target, null, null);
            float selfDmg = calculateCrystalDamage(crystalPos, mc.player, null, null);

            if (isGoodDamage(targetDmg, selfDmg, target) && targetDmg > maxDmg) {
                maxDmg = targetDmg;
                best = crystal;
            }
        }
        return best;
    }

    private void explodeCrystal(EndCrystal crystal) {
        int antiWeakSlot = findAntiWeaknessSlot();
        if (antiWeakSlot != -1 && antiWeakSlot != mc.player.getInventory().getSelectedSlot()) {
            switchToSlot(antiWeakSlot);
        }

        mc.gameMode.attack(mc.player, crystal);
        mc.player.swing(InteractionHand.MAIN_HAND);
        renderActions.add(new RenderAction(crystal.blockPosition(), false, System.currentTimeMillis()));
    }

    private PlacementSpot findBestPlacement(Player target, boolean needsObsidianPlacement) {
        List<BlockPos> spots = getTacticalSpots(target);
        PlacementSpot best = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        Vec3 predPos = predictEntityPosition(target, predictTicks.getValue());
        AABB predBox = target.getBoundingBox().move(predPos.subtract(target.position()));

        for (BlockPos pos : spots) {
            if (!isBlockInReach(pos, placeRange.getValue())) continue;

            if (needsObsidianPlacement) {
                if (!canPlaceBlockAt(pos)) continue;
                if (!isCrystalPlacementClear(pos)) continue;

                BlockHitResult hit = getPlacementHitResult(pos);
                if (hit == null) continue;

                Vec3 crystalVec = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                float curDmg = calculateCrystalDamage(crystalVec, target, pos, null);
                float predDmg = calculateCrystalDamage(crystalVec, target, predBox, pos, null);
                float targetDmg = Math.max(curDmg, predDmg);
                float selfDmg = calculateCrystalDamage(crystalVec, mc.player, pos, null);

                if (isGoodDamage(targetDmg, selfDmg, target)) {
                    float distToTarget = (float) pos.distToCenterSqr(target.position());
                    float score = targetDmg - (selfDmg * 2.0f) - (distToTarget * 0.1f) - 2.5f;

                    if (score > bestScore) {
                        bestScore = score;
                        best = new PlacementSpot(pos, hit, hit.getLocation(), targetDmg, selfDmg, true);
                    }
                }
            } else {
                BlockState base = mc.level.getBlockState(pos);
                if (!base.is(Blocks.OBSIDIAN) && !base.is(Blocks.BEDROCK)) continue;
                if (!isCrystalPlacementClear(pos)) continue;

                BlockHitResult hit = getVisibleHitResult(pos);
                if (hit == null) continue;

                Vec3 crystalVec = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                float curDmg = calculateCrystalDamage(crystalVec, target, null, null);
                float predDmg = calculateCrystalDamage(crystalVec, target, predBox, null, null);
                float targetDmg = Math.max(curDmg, predDmg);
                float selfDmg = calculateCrystalDamage(crystalVec, mc.player, null, null);

                if (isGoodDamage(targetDmg, selfDmg, target)) {
                    float distToTarget = (float) pos.distToCenterSqr(target.position());
                    float score = targetDmg - (selfDmg * 2.0f) - (distToTarget * 0.1f);

                    if (score > bestScore) {
                        bestScore = score;
                        best = new PlacementSpot(pos, hit, hit.getLocation(), targetDmg, selfDmg, false);
                    }
                }
            }
        }

        return best;
    }

    private void placeCrystal(PlacementSpot spot) {
        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1) return;

        InteractionHand hand = (crystalSlot == 40) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (hand == InteractionHand.MAIN_HAND) {
            switchToSlot(crystalSlot);
        }

        mc.gameMode.useItemOn(mc.player, hand, spot.hit());
        mc.player.swing(hand);
        renderActions.add(new RenderAction(spot.pos(), false, System.currentTimeMillis()));
    }

    private void placeObsidian(PlacementSpot spot) {
        int obbySlot = findObsidianSlot();
        if (obbySlot == -1) return;

        placeBlock(spot.hit(), obbySlot, true);
        renderActions.add(new RenderAction(spot.pos(), true, System.currentTimeMillis()));
    }

    private void placeBlock(BlockHitResult hit, int slot, boolean swing) {
        if (slot == -1 || hit == null) return;

        InteractionHand hand = (slot == 40) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (hand == InteractionHand.MAIN_HAND) {
            switchToSlot(slot);
        }

        mc.gameMode.useItemOn(mc.player, hand, hit);
        if (swing) {
            mc.player.swing(hand);
        }
    }

    private boolean isCrystalPlacementClear(BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = mc.level.getBlockState(above);
        if (!aboveState.isAir() && !aboveState.canBeReplaced()) return false;

        AABB crystalBox = new AABB(pos.getX(), pos.getY() + 1.0, pos.getZ(), pos.getX() + 1.0, pos.getY() + 3.0, pos.getZ() + 1.0);
        return mc.level.getEntities((Entity) null, crystalBox, e -> e.isAlive() && !e.isRemoved()
                && !(e instanceof ItemEntity) && !(e instanceof ExperienceOrb) && !(e instanceof Projectile)).isEmpty();
    }

    private List<BlockPos> getTacticalSpots(Player target) {
        List<BlockPos> spots = new ArrayList<>();
        Set<BlockPos> set = new LinkedHashSet<>();

        BlockPos feet = target.blockPosition();
        Vec3 pred = predictEntityPosition(target, predictTicks.getValue());
        BlockPos predFeet = BlockPos.containing(pred);

        for (BlockPos center : List.of(feet, predFeet)) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        if (dx * dx + dz * dz > 10) continue;
                        set.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }

        for (BlockPos p : set) {
            if (isBlockInReach(p, placeRange.getValue())) {
                spots.add(p);
            }
        }

        spots.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(target.position())));
        return spots;
    }

    // =========================================================================
    // Precise Damage Calculation
    // =========================================================================

    public float calculateCrystalDamage(Vec3 explosionCenter, LivingEntity target, BlockPos placedObsidian, BlockPos ignoredBlock) {
        if (explosionCenter == null || target == null || mc.level == null) return 0.0f;
        return calculateCrystalDamage(explosionCenter, target, target.getBoundingBox(), placedObsidian, ignoredBlock);
    }

    public float calculateCrystalDamage(Vec3 explosionCenter, LivingEntity target, AABB targetBox, BlockPos placedObsidian, BlockPos ignoredBlock) {
        if (explosionCenter == null || target == null || mc.level == null || targetBox == null) return 0.0f;

        Vec3 center = new Vec3((targetBox.minX + targetBox.maxX) * 0.5, targetBox.minY, (targetBox.minZ + targetBox.maxZ) * 0.5);
        double dist = center.distanceTo(explosionCenter) / 12.0;
        if (dist > 1.0) return 0.0f;

        double exposure = calculateExposure(explosionCenter, target, targetBox, placedObsidian, ignoredBlock);
        if (exposure <= 0.0) return 0.0f;

        double impact = (1.0 - dist) * exposure;
        float damage = (float) ((impact * impact + impact) * 42.0 + 1.0);

        if (target instanceof Player) {
            Difficulty diff = mc.level.getDifficulty();
            damage = switch (diff) {
                case PEACEFUL -> 0.0f;
                case EASY -> Math.min(damage * 0.5f + 1.0f, damage);
                case HARD -> damage * 1.5f;
                case NORMAL -> damage;
            };
        }

        // Armor & Toughness reduction
        float armor = target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float effectiveArmor = Mth.clamp(armor - damage / (2.0f + toughness / 4.0f), armor * 0.2f, 20.0f);
        damage *= (1.0f - effectiveArmor / 25.0f);

        // Protection & Blast Protection reduction
        int epf = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = target.getItemBySlot(slot);
            epf += getEnchantmentLevel(target, stack, Enchantments.PROTECTION);
            epf += getEnchantmentLevel(target, stack, Enchantments.BLAST_PROTECTION) * 2;
        }
        epf = Mth.clamp(epf, 0, 20);
        damage *= (1.0f - epf / 25.0f);

        // Resistance effect reduction
        MobEffectInstance res = target.getEffect(MobEffects.RESISTANCE);
        if (res != null) {
            float resReduction = Mth.clamp((res.getAmplifier() + 1) * 0.2f, 0.0f, 1.0f);
            damage *= (1.0f - resReduction);
        }

        return Math.max(0.0f, damage);
    }

    private double calculateExposure(Vec3 explosionCenter, LivingEntity target, AABB box, BlockPos placedObsidian, BlockPos ignoredBlock) {
        double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
        double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;

        int visible = 0;
        int total = 0;

        for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
                for (double z = 0.0; z <= 1.0; z += stepZ) {
                    Vec3 point = new Vec3(
                            Mth.lerp(x, box.minX, box.maxX) + offsetX,
                            Mth.lerp(y, box.minY, box.maxY),
                            Mth.lerp(z, box.minZ, box.maxZ) + offsetZ
                    );

                    if (canSeeRay(point, explosionCenter, target, placedObsidian, ignoredBlock)) {
                        visible++;
                    }
                    total++;
                }
            }
        }

        return total == 0 ? 0.0 : (double) visible / total;
    }

    private boolean canSeeRay(Vec3 from, Vec3 to, LivingEntity entity, BlockPos placedObsidian, BlockPos ignoredBlock) {
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        BlockHitResult hit = entity.level().clip(context);
        if (hit.getType() == HitResult.Type.MISS) return true;

        if (ignoredBlock != null && hit.getBlockPos().equals(ignoredBlock)) {
            Vec3 dir = to.subtract(from).normalize();
            Vec3 next = hit.getLocation().add(dir.scale(0.1));
            if (next.distanceToSqr(from) >= to.distanceToSqr(from)) return true;
            return canSeeRay(next, to, entity, placedObsidian, null);
        }

        return false;
    }

    private int getEnchantmentLevel(LivingEntity entity, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty() || entity == null || entity.level() == null) return 0;
        try {
            var opt = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
            if (opt.isPresent()) {
                return EnchantmentHelper.getItemEnchantmentLevel(opt.get(), stack);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // =========================================================================
    // Decision & Safety Checks
    // =========================================================================

    private boolean isGoodDamage(float targetDamage, float selfDamage, Player target) {
        if (target == null) return false;

        float targetHp = target.getHealth() + target.getAbsorptionAmount();
        float myHp = (mc.player != null) ? mc.player.getHealth() + mc.player.getAbsorptionAmount() : 20.0f;

        // Faceplace logic
        float requiredDamage = minTargetDamage.getValue();
        if (targetHp <= faceplaceHp.getValue()) {
            requiredDamage = 1.0f;
        } else if (isTargetInCobweb(target)) {
            requiredDamage = minWebDamage.getValue();
        }

        if (targetDamage < requiredDamage) return false;

        if (antiSuicide.getValue()) {
            if (selfDamage > maxSelfDamage.getValue()) return false;

            // Lethal target bypass
            if (targetDamage >= targetHp) {
                return selfDamage < (myHp - 0.5f);
            }

            if (selfDamage >= targetDamage && selfDamage > 2.0f) {
                return false;
            }
        }

        return true;
    }

    private boolean isTargetInCobweb(Player target) {
        AABB bb = target.getBoundingBox();
        int minX = Mth.floor(bb.minX);
        int maxX = Mth.floor(bb.maxX);
        int minY = Mth.floor(bb.minY);
        int maxY = Mth.floor(bb.maxY);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.floor(bb.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (mc.level.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBWEB)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Player findTarget() {
        return mc.level.players().stream()
                .filter(p -> p != mc.player && p.isAlive() && !p.isSpectator())
                .filter(p -> mc.player.distanceTo(p) <= targetRange.getValue())
                .filter(p -> !ignoreFriends.getValue() || !FriendManager.getInstance().isFriend(p))
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);
    }

    private Vec3 predictEntityPosition(LivingEntity entity, double ticks) {
        if (entity == null || ticks <= 0.0) return entity != null ? entity.position() : Vec3.ZERO;
        Vec3 vel = entity.getDeltaMovement();
        double vy = entity.onGround() ? 0.0 : Mth.clamp(vel.y, -1.0, 1.0);
        return entity.position().add(
                Mth.clamp(vel.x, -0.6, 0.6) * ticks,
                vy * ticks,
                Mth.clamp(vel.z, -0.6, 0.6) * ticks
        );
    }

    // =========================================================================
    // Rotations & Timing
    // =========================================================================

    private boolean syncRotation(Vec3 vec) {
        if (vec == null || mc.player == null) return false;
        Vec3 eye = mc.player.getEyePosition();
        double dx = vec.x - eye.x;
        double dy = vec.y - eye.y;
        double dz = vec.z - eye.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        float targetYaw = Mth.wrapDegrees(yaw);
        float targetPitch = Mth.clamp(pitch, -90.0f, 90.0f);

        RotationHandler.setRotation(targetYaw, targetPitch);

        if (!legit.getValue()) return true;

        float curYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : mc.player.getYRot();
        float curPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : mc.player.getXRot();
        float diffYaw = Math.abs(Mth.wrapDegrees(targetYaw - curYaw));
        float diffPitch = Math.abs(targetPitch - curPitch);

        return diffYaw <= 35.0f && diffPitch <= 35.0f;
    }

    private void resetRotation() {
        RotationHandler.disengage("Smooth");
    }

    // =========================================================================
    // Slot Management
    // =========================================================================

    private int findCrystalSlot() {
        if (mc.player.getOffhandItem().is(Items.END_CRYSTAL)) return 40;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.END_CRYSTAL)) return i;
        }
        return -1;
    }

    private int findObsidianSlot() {
        if (mc.player.getOffhandItem().is(Blocks.OBSIDIAN.asItem())) return 40;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Blocks.OBSIDIAN.asItem())) return i;
        }
        return -1;
    }

    private int findPickaxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.PICKAXES)) {
                return i;
            }
        }
        return -1;
    }

    private int findAntiWeaknessSlot() {
        int cur = mc.player.getInventory().getSelectedSlot();
        MobEffectInstance weakness = mc.player.getEffect(MobEffects.WEAKNESS);
        if (!antiWeakness.getValue() || weakness == null) return cur;

        MobEffectInstance strength = mc.player.getEffect(MobEffects.STRENGTH);
        double strBonus = (strength == null ? 0.0 : (strength.getAmplifier() + 1) * 3.0);
        double reqDamage = (weakness.getAmplifier() + 1) * 4.0 - strBonus;

        if (getItemAttackDamage(mc.player.getMainHandItem()) > reqDamage) return cur;

        int bestSlot = -1;
        double bestDmg = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            double dmg = getItemAttackDamage(stack);
            if (dmg > reqDamage && dmg > bestDmg) {
                bestDmg = dmg;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private double getItemAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        if (stack.is(Items.NETHERITE_SWORD)) return 8.0;
        if (stack.is(Items.DIAMOND_SWORD)) return 7.0;
        if (stack.is(Items.IRON_SWORD)) return 6.0;
        if (stack.is(Items.STONE_SWORD)) return 5.0;
        if (stack.is(Items.GOLDEN_SWORD) || stack.is(Items.WOODEN_SWORD)) return 4.0;
        if (stack.is(Items.NETHERITE_AXE)) return 10.0;
        if (stack.is(Items.DIAMOND_AXE)) return 9.0;
        if (stack.is(Items.IRON_AXE)) return 9.0;
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) return 5.0;
        return 0.0;
    }

    private void switchToSlot(int slot) {
        if (slot < 0 || slot >= 9) return;
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
        }
        if (lastSentSlot != slot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            lastSentSlot = slot;
        }
    }

    private void restoreSlot() {
        if (originalSlot != -1 && mc.player != null && mc.player.getInventory().getSelectedSlot() != originalSlot) {
            switchToSlot(originalSlot);
        }
    }

    // =========================================================================
    // Geometry & Reach Helpers
    // =========================================================================

    private boolean isBlockInReach(BlockPos pos, float maxRange) {
        if (pos == null || mc.player == null) return false;
        Vec3 eye = mc.player.getEyePosition();
        double cx = Mth.clamp(eye.x, pos.getX(), pos.getX() + 1.0);
        double cy = Mth.clamp(eye.y, pos.getY(), pos.getY() + 1.0);
        double cz = Mth.clamp(eye.z, pos.getZ(), pos.getZ() + 1.0);
        return eye.distanceTo(new Vec3(cx, cy, cz)) <= maxRange;
    }

    private boolean canPlaceBlockAt(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) return false;

        AABB box = new AABB(pos);
        if (mc.player.getBoundingBox().intersects(box)) return false;

        return mc.level.getEntities((Entity) null, box, e -> e.isAlive() && !e.isRemoved()
                && !(e instanceof ItemEntity) && !(e instanceof ExperienceOrb) && !(e instanceof Projectile)).isEmpty();
    }

    private BlockHitResult getVisibleHitResult(BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        Direction[] faces = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};
        BlockHitResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction face : faces) {
            Vec3 test = new Vec3(
                    pos.getX() + 0.5 + face.getStepX() * 0.48,
                    pos.getY() + 0.5 + face.getStepY() * 0.48,
                    pos.getZ() + 0.5 + face.getStepZ() * 0.48
            );

            BlockHitResult hit = mc.level.clip(new ClipContext(eye, test, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                double dist = eye.distanceToSqr(hit.getLocation());
                if (dist < bestDist && dist <= (placeRange.getValue() * placeRange.getValue())) {
                    bestDist = dist;
                    best = hit;
                }
            }
        }
        return best != null ? best : new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false);
    }

    private BlockHitResult getPlacementHitResult(BlockPos targetPos) {
        Vec3 eye = mc.player.getEyePosition();
        Direction[] sides = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        BlockHitResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction side : sides) {
            BlockPos neighbor = targetPos.relative(side);
            BlockState state = mc.level.getBlockState(neighbor);
            if (state.isAir() || state.canBeReplaced() || !state.isSolid()) continue;

            Direction clickFace = side.getOpposite();
            Vec3 test = new Vec3(
                    neighbor.getX() + 0.5 + clickFace.getStepX() * 0.48,
                    neighbor.getY() + 0.5 + clickFace.getStepY() * 0.48,
                    neighbor.getZ() + 0.5 + clickFace.getStepZ() * 0.48
            );

            BlockHitResult hit = mc.level.clip(new ClipContext(eye, test, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(neighbor)) {
                double dist = eye.distanceToSqr(hit.getLocation());
                if (dist < bestDist && dist <= (placeRange.getValue() * placeRange.getValue())) {
                    bestDist = dist;
                    best = hit;
                }
            }
        }
        return best;
    }

    private void resetState() {
        lastActionTime = 0L;
        lastSentSlot = -1;
        originalSlot = -1;
        cancelMining();
        shieldPos = null;
        shieldTicks = 0;
        targetCrystal = null;
        targetActionPos = null;
        renderActions.clear();
        resetRotation();
    }

    private void cleanRenderActions() {
        long now = System.currentTimeMillis();
        renderActions.removeIf(a -> (now - a.time()) > 350L);
    }

    // =========================================================================
    // 3D Rendering (Modern Blaze3d Pipeline via Render3D)
    // =========================================================================

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!render.getValue()) return;

        long now = System.currentTimeMillis();

        // 1. Render action boxes (fading out)
        for (RenderAction action : renderActions) {
            float progress = (float) (now - action.time()) / 350.0f;
            if (progress >= 1.0f) continue;

            float alpha = 1.0f - progress;
            Color fill = action.isObsidian()
                    ? new Color(140, 40, 255, (int) (alpha * 60))
                    : new Color(255, 50, 110, (int) (alpha * 70));
            Color outline = action.isObsidian()
                    ? new Color(160, 60, 255, (int) (alpha * 220))
                    : new Color(255, 60, 120, (int) (alpha * 240));

            if (renderType.is("Куб")) {
                Render3D.drawBox(action.pos(), fill, outline, true, true, true);
            } else {
                Render3D.drawTile(action.pos(), fill, outline, true, true, true);
            }
        }

        // 2. Render target crystal if attacking
        if (targetCrystal != null && targetCrystal.isAlive()) {
            Color fill = new Color(255, 0, 50, 60);
            Color outline = new Color(255, 0, 50, 240);
            Render3D.drawBox(targetCrystal.getBoundingBox(), fill, outline, true, true, true);
        }

        // 3. Render auto-dig block
        if (miningPos != null) {
            float breakProgress = Math.min(1.0f, miningTicks / 100.0f);
            Color fill = new Color(255, 180, 0, (int) (50 + breakProgress * 80));
            Color outline = new Color(255, 200, 0, 240);
            AABB shrinkBox = new AABB(miningPos).deflate((1.0 - breakProgress) * 0.2);
            Render3D.drawBox(shrinkBox, fill, outline, true, true, true);
        }
    }

    public boolean isBusy() {
        return isEnabled() && (targetCrystal != null || miningPos != null || targetActionPos != null);
    }

    private record PlacementSpot(
            BlockPos pos, BlockHitResult hit, Vec3 lookVec, float targetDamage, float selfDamage, boolean needsObsidian
    ) {}

    private record RenderAction(BlockPos pos, boolean isObsidian, long time) {}
}