package error.module;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import error.setting.BindMode;
import error.module.impl.combat.*;
import error.module.impl.misc.*;
import error.module.impl.movement.*;
import error.module.impl.player.*;
import error.module.impl.render.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@Getter
public class Modules {
    private final List<Module> modules = new ArrayList<>();
    public ClickGui clickGui;
    public ClientSounds clientSounds;
    public Sprint sprint;
    public AuraModule auraModule;
    public NameTags nameTags;
    public Interface anInterface;
    public GuiWalk guiWalk;
    public AutoExplosion autoExplosion;
    public Removals removals;
    public WorldParticles worldParticles;
    public Ambience ambience;
    public Arrows arrows;
    public ElytraBooster elytraBooster;
    public FakePlayer fakePlayer;
    public ElytraMotion elytraMotion;
    public AirStuck airStuck;
    public NoDelay noDelay;
    public ClickPearl clickPearl;
    public ElytraSwap elytraSwap;
    public ServerHelper serverHelper;
    public WaterSpeed waterSpeed;
    public NoFall noFall;
    public AutoSwap autoSwap;
    public Timer timer;
    public BetterMinecraft betterMinecraft;
    public FreeLook freeLook;
    public HandShader handShader;
    public HitEffect hitEffect;
    public ViewModel viewModel;
    public SwingAnimation swingAnimation;
    public PopEffect popEffect;
    public AutoTotem autoTotem;
    public CrystalAura crystalAura;
    public BlockHighlight blockHighlight;
    public JumpCircles jumpCircles;
    public Predictions predictions;
    public ClickFriend clickFriend;
    public FireworkESP fireworkESP;
    public FreeCam freeCam;
    public ItemScroller itemScroller;
    public UnHook unHook;
    public FullBright fullBright;
    public AHHelper ahHelper;
    public AntiPush antiPush;
    public AutoTool autoTool;
    public ProjectileHelper projectileHelper;
    public Particles particles;
    public WebTrap webTrap;
    public AimAssistant aimAssistant;
    public TriggerBot triggerBot;
    public Criticals criticals;
    public AutoAccept autoAccept;
    public PotionTracker potionTracker;
    public UseTracker useTracker;
    public AutoEvent autoEvent;
    public AutoSell autoSell;
    public HoldMyItems holdMyItems;
    public Notification notification;
    public CustomModels customModels;
    public MaceHelper maceHelper;
    public WindCharge windCharge;
    public AutoCaptcha autoCaptcha;
    public IRC irc;
    public ExpThrow expThrow;
    public AspectRatio aspectRatio;
    public BlockOutline blockOutline;

    public void init() {
        register(
                this.clickGui = new ClickGui(),
                this.notification = new Notification(),
                this.customModels = new CustomModels(),
                this.holdMyItems = new HoldMyItems(),
                this.maceHelper = new MaceHelper(),
                this.windCharge = new WindCharge(),
                this.autoCaptcha = new AutoCaptcha(),
                this.irc = new IRC(),
                this.expThrow = new ExpThrow(),
                this.aspectRatio = new AspectRatio(),
                this.blockOutline = new BlockOutline(),
                this.sprint = new Sprint(),
                this.elytraSwap = new ElytraSwap(),
                this.fullBright = new FullBright(),
                this.potionTracker = new PotionTracker(),
                this.useTracker = new UseTracker(),
                this.autoEvent = new AutoEvent(),
                this.autoSell = new AutoSell(),
                this.autoAccept = new AutoAccept(),
                this.triggerBot = new TriggerBot(),
                this.criticals = new Criticals(),
                this.crystalAura = new CrystalAura(),
                this.ahHelper = new AHHelper(),
                this.antiPush = new AntiPush(),
                this.projectileHelper = new ProjectileHelper(),
                this.autoTool = new AutoTool(),
                this.clickFriend = new ClickFriend(),
                this.autoExplosion = new AutoExplosion(),
                this.popEffect = new PopEffect(),
                this.fireworkESP = new FireworkESP(),
                this.aimAssistant = new AimAssistant(),
                this.itemScroller = new ItemScroller(),
                this.waterSpeed = new WaterSpeed(),
                this.jumpCircles = new JumpCircles(),
                this.freeCam = new FreeCam(),
                this.swingAnimation =new SwingAnimation(),
                this.predictions = new Predictions(),
                this.webTrap = new WebTrap(),
                this.viewModel = new ViewModel(),
                this.blockHighlight = new BlockHighlight(),
                this.autoSwap = new AutoSwap(),
                this.hitEffect = new HitEffect(),
                this.autoTotem = new AutoTotem(),
                this.noFall = new NoFall(),
                this.handShader = new HandShader(),
                this.betterMinecraft = new BetterMinecraft(),
                this.guiWalk = new GuiWalk(),
                this.removals = new Removals(),
                this.noDelay = new NoDelay(),
                this.arrows = new Arrows(),
                this.freeLook = new FreeLook(),
                this.serverHelper = new ServerHelper(),
                this.clickPearl = new ClickPearl(),
                this.timer = new Timer(),
                this.elytraBooster = new ElytraBooster(),
                this.airStuck = new AirStuck(),
                this.fakePlayer = new FakePlayer(),
                this.elytraMotion = new ElytraMotion(),
                this.ambience = new Ambience(),
                this.anInterface = new Interface(),
                this.nameTags = new NameTags(),
                this.auraModule = new AuraModule(),
                this.worldParticles = new WorldParticles(),
                this.particles = new Particles(),
                this.clientSounds = new ClientSounds(),
                this.unHook = new UnHook()
        );
    }

    public void register(Module... mods) {
        for (Module mod : mods) {
            modules.add(mod);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (module.getClass() == clazz) {
                return (T) module;
            }
        }
        return null;
    }

    public Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public void onKey(int key, int action) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == 0) return;
        for (Module module : modules) {
            if (module.getBind().matches(key)) {
                BindMode mode = module.getBind().getMode(0, BindMode.TOGGLE);
                if (mode == BindMode.HOLD) {
                    if (action == GLFW.GLFW_PRESS) {
                        module.setState(true);
                    } else if (action == GLFW.GLFW_RELEASE) {
                        module.setState(false);
                    }
                } else {
                    if (action == GLFW.GLFW_PRESS) {
                        module.toggle();
                    }
                }
            }
        }
    }

    public void onMouse(int button, int action) {
        for (Module module : modules) {
            if (module.getBind().matchesMouse(button)) {
                BindMode mode = module.getBind().getMode(0, BindMode.TOGGLE);
                if (mode == BindMode.HOLD) {
                    if (action == GLFW.GLFW_PRESS) {
                        module.setState(true);
                    } else if (action == GLFW.GLFW_RELEASE) {
                        module.setState(false);
                    }
                } else {
                    if (action == GLFW.GLFW_PRESS) {
                        module.toggle();
                    }
                }
            }
        }
    }
}