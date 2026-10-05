package error.module.impl.player;

import error.event.EventTarget;
import error.event.list.EventTravel;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

public class AirStuck extends Module {
    public static AirStuck INSTANCE;

    private static final int CHESTPLATE_SLOT = 6;
    private static final double STOP_GROUND_OFFSET = 0.1D;
    private static final double MAX_RAY_DISTANCE = 8.0D;
    private static final float ROTATION_EPSILON = 0.001F;

    public final BindSetting toGround = bind("До земли", GLFW.GLFW_KEY_UNKNOWN);

    private Vec3 freezePos = Vec3.ZERO;
    private Vec3 freezeVelocity = Vec3.ZERO;
    private float freezeYaw = 0.0F;
    private float freezePitch = 0.0F;
    private boolean isFrozen = false;
    private boolean isFallingToGround = false;
    private boolean stoppedNearGround = false;
    private boolean snapOnNextMove = false;
    private boolean isSendingRotationPacket = false;

    private static long sneakSuppressUntil = 0L;

    public AirStuck() {
        super("AirStuck", "Зависает в воздухе", Category.PLAYER);
        INSTANCE = this;
    }

    public static void suppressSneak(long millis) {
        long target = System.currentTimeMillis() + millis;
        if (target > sneakSuppressUntil) {
            sneakSuppressUntil = target;
        }
    }

    public static boolean isSneakSuppressed() {
        return System.currentTimeMillis() < sneakSuppressUntil;
    }

    public boolean isFrozenState() {
        return this.isEnabled() && this.isFrozen;
    }

    public boolean isFrozen() {
        return isFrozenState();
    }

    @Override
    public void onEnable() {
        this.isFrozen = false;
        this.isFallingToGround = false;
        this.stoppedNearGround = false;
        this.snapOnNextMove = false;
        suppressSneak(500L);

        LocalPlayer player = mc.player;
        if (player != null) {
            this.swapChestplateIfElytra();
            this.freezeVelocity = player.getDeltaMovement();
            this.freezeYaw = player.getYRot();
            this.freezePitch = player.getXRot();
            this.freezePos = player.position();
            this.isFrozen = true;
            this.freezePlayer(player);
        }
    }

    @Override
    public void onDisable() {
        boolean wasFrozen = this.isFrozen;
        this.isFrozen = false;
        this.isFallingToGround = false;
        this.stoppedNearGround = false;
        this.snapOnNextMove = false;
        suppressSneak(500L);

        LocalPlayer player = mc.player;
        if (player != null) {
            player.noPhysics = false;
            if (wasFrozen && this.freezePos != null) {
                player.setPos(this.freezePos.x, this.freezePos.y, this.freezePos.z);
                player.setDeltaMovement(this.freezeVelocity);
            }

            float newYaw = this.freezeYaw + 0.012F;
            float newPitch = clampPitch(this.freezePitch + 0.012F);
            player.setYRot(newYaw);
            player.setXRot(newPitch);
            player.yRotO = newYaw;
            player.xRotO = newPitch;
        }
    }

    private float clampPitch(float pitch) {
        if (pitch > 90.0F) {
            return 90.0F;
        } else if (pitch < -90.0F) {
            return -90.0F;
        }
        return pitch;
    }

    public boolean sendRotation(float yaw, float pitch) {
        LocalPlayer player = mc.player;
        if (this.isFrozen && player != null && player.connection != null) {
            float clamped = clampPitch(pitch);
            if (Math.abs(yaw - this.freezeYaw) < ROTATION_EPSILON && Math.abs(clamped - this.freezePitch) < ROTATION_EPSILON) {
                return false;
            }

            this.isSendingRotationPacket = true;
            try {
                player.connection.send(new ServerboundMovePlayerPacket.Rot(
                        yaw,
                        clamped,
                        player.onGround(),
                        player.horizontalCollision
                ));
            } finally {
                this.isSendingRotationPacket = false;
            }

            this.freezeYaw = yaw;
            this.freezePitch = clamped;
            return true;
        }
        return false;
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() == 1 && mc.player != null && screen() == null) {
            if (this.toGround.matches(event.getKey())) {
                this.triggerToGround();
            }
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() == 1 && mc.player != null && screen() == null) {
            if (this.toGround.matchesMouse(event.getButton())) {
                this.triggerToGround();
            }
        }
    }

    private void triggerToGround() {
        LocalPlayer player = mc.player;
        if (player != null) {
            if (!this.isFallingToGround && !this.stoppedNearGround && this.isFrozen) {
                if (this.calculateDistanceToGround(player) <= STOP_GROUND_OFFSET) {
                    this.stoppedNearGround = true;
                    this.freezePlayer(player);
                } else {
                    this.isFrozen = false;
                    this.isFallingToGround = true;
                    player.noPhysics = false;
                    player.setDeltaMovement(this.freezeVelocity);
                }
            }
        }
    }

    private double calculateDistanceToGround(LocalPlayer player) {
        if (mc.level == null) {
            return MAX_RAY_DISTANCE;
        }
        AABB box = player.getBoundingBox();
        Iterable<VoxelShape> collisions = mc.level.getBlockCollisions(player, box.expandTowards(0.0D, -MAX_RAY_DISTANCE, 0.0D));
        double maxOffset = Shapes.collide(Direction.Axis.Y, box, collisions, -MAX_RAY_DISTANCE);
        return -maxOffset;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        LocalPlayer player = mc.player;
        if (player != null) {
            suppressSneak(500L);
            if (mc.options.keyShift.isDown()) {
                mc.options.keyShift.setDown(false);
            }

            if (this.snapOnNextMove) {
                this.snapOnNextMove = false;
                this.isFallingToGround = false;
                this.stoppedNearGround = true;
                this.freezePos = player.position();
                this.freezeYaw = player.getYRot();
                this.freezePitch = player.getXRot();
            }

            if (!this.isFrozen) {
                this.freezeVelocity = player.getDeltaMovement();
            }

            if (this.isFrozen) {
                this.freezePlayer(player);
            }
        }
    }

    @EventTarget
    public void onTravel(EventTravel event) {
        LocalPlayer player = mc.player;
        if (player != null) {
            if (this.isFrozen) {
                if (this.snapOnNextMove) {
                    event.setMovement(Vec3.ZERO);
                    event.cancel();
                } else {
                    this.freezePlayer(player);
                    event.setMovement(Vec3.ZERO);
                    event.cancel();
                }
            } else if (this.isFallingToGround) {
                Vec3 movement = event.getMovement();
                if (movement != null && movement.y < 0.0D) {
                    double dist = Math.max(0.0D, this.calculateDistanceToGround(player) - STOP_GROUND_OFFSET);
                    if (-movement.y >= dist) {
                        event.setMovement(new Vec3(movement.x, -dist, movement.z));
                        this.isFrozen = true;
                        this.snapOnNextMove = true;
                    }
                }
            }
        }
    }

    private void freezePlayer(LocalPlayer player) {
        player.noPhysics = true;
        if (this.freezePos != null) {
            player.setPos(this.freezePos.x, this.freezePos.y, this.freezePos.z);
        }
        player.setDeltaMovement(Vec3.ZERO);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (event.isSend()) {
            if (this.isFrozen && !this.isSendingRotationPacket && packet instanceof ServerboundMovePlayerPacket) {
                event.cancel();
            }
        } else {
            if (packet instanceof ClientboundRespawnPacket || packet instanceof ClientboundLoginPacket) {
                this.setState(false);
            }
        }
    }

    public void swapChestplateIfElytra() {
        LocalPlayer player = mc.player;
        if (player != null && mc.gameMode != null) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.is(Items.ELYTRA)) {
                int bestSlot = this.findBestChestplate(player);
                if (bestSlot != -1) {
                    this.swapSlot(bestSlot);
                }
            }
        }
    }

    private void swapSlot(int slot) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        int containerId = player.inventoryMenu.containerId;
        if (slot >= 0 && slot < 9) {
            mc.gameMode.handleContainerInput(containerId, CHESTPLATE_SLOT, slot, ContainerInput.SWAP, player);
        } else {
            mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.SWAP, player);
            mc.gameMode.handleContainerInput(containerId, CHESTPLATE_SLOT, 0, ContainerInput.SWAP, player);
            mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.SWAP, player);
        }
        if (player.connection != null) {
            player.connection.send(new ServerboundContainerClosePacket(containerId));
        }
    }

    private int findBestChestplate(LocalPlayer player) {
        int bestSlot = -1;
        double bestScore = -1.0D;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot() == EquipmentSlot.CHEST) {
                Item item = stack.getItem();
                    int prot = getEnchantmentLevel(stack, Enchantments.PROTECTION);
                    int unb = getEnchantmentLevel(stack, Enchantments.UNBREAKING);
                    int mend = getEnchantmentLevel(stack, Enchantments.MENDING);
                    int tier = getArmorTier(item);
                    int maxDmg = stack.getMaxDamage();
                    int dmg = stack.getDamageValue();
                    double durabilityRatio = maxDmg == 0 ? 1.0D : (double) (maxDmg - dmg) / (double) maxDmg;
                    double score = (double) tier * 10000.0D + (double) prot * 100.0D + (double) unb * 10.0D + (mend > 0 ? 1.0D : 0.0D) + durabilityRatio * 10.0D;
                    if (score > bestScore) {
                        bestScore = score;
                        bestSlot = i;
                    }
                }
            }
        return bestSlot;
    }

    private int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(enchantmentKey)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private int getArmorTier(Item item) {
        if (item == Items.NETHERITE_CHESTPLATE) return 5;
        if (item == Items.DIAMOND_CHESTPLATE) return 4;
        if (item == Items.IRON_CHESTPLATE) return 3;
        if (item == Items.GOLDEN_CHESTPLATE) return 2;
        if (item == Items.CHAINMAIL_CHESTPLATE) return 2;
        if (item == Items.LEATHER_CHESTPLATE) return 1;
        return 0;
    }
}