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
    public FakePlayer fakePlayer;
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
    public FireworkESP fireworkESP;
    public FreeCam freeCam;
    public ItemScroller itemScroller;
    public UnHook unHook;
    public FullBright fullBright;
    public AHHelper ahHelper;
    public NoPush noPush;
    public AutoTool autoTool;
    public ProjectileHelper projectileHelper;
    public Particles particles;
    public WebTrap webTrap;
    public AimAssistant aimAssistant;
    public TriggerBot triggerBot;
    public Criticals criticals;
    public AutoAccept autoAccept;
    public UseTracker useTracker;
    public AutoEvent autoEvent;
    public AutoSell autoSell;
    public AutoMsg autoMsg;
    public FunPay funPay;
    public SpecScan specScan;
    public HoldMyItems holdMyItems;
    public Notification notification;
    public CustomModels customModels;
    public MaceHelper maceHelper;
    public MaceSounds maceSounds;
    public AutoCaptcha autoCaptcha;
    public IRC irc;
    public ExpThrow expThrow;
    public AspectRatio aspectRatio;
    public BlockOutline blockOutline;
    public RenderDemoModule renderDemoModule;
    public DiscordRPC discordRPC;
    public TargetEsp targetEsp;
    public InterpolateF5 interpolateF5;
    public ObsidianFarm obsidianFarm;
    public DeathCoords deathCoords;
    public KeyFinderTeleport keyFinderTeleport;
    public PearlTarget pearlTarget;
    public SpeedExploit speedExploit;
    public Speed speed;
    public ElytraExploit elytraExploit;
    public PlayerFakelags playerFakelags;
    public error.module.impl.movement.NoSlow noSlow;
    public error.module.impl.movement.NoWeb noWeb;
    public error.module.impl.movement.NoClip noClip;
    public error.module.impl.movement.Disabler disabler;
    public error.module.impl.movement.NoFallExploit noFallExploit;
    public error.module.impl.movement.ElytraBooster elytraBooster;
    public error.module.impl.movement.ElytraResolver elytraResolver;
    public error.module.impl.movement.ElytraSample elytraSample;

    public void init() {
        this.clickGui = new ClickGui();
        this.anInterface = new Interface();
        this.notification = new Notification();
        this.customModels = new CustomModels();
        this.holdMyItems = new HoldMyItems();
        this.maceHelper = new MaceHelper();
        this.maceSounds = new MaceSounds();
        this.autoCaptcha = new AutoCaptcha();
        this.irc = new IRC();
        this.expThrow = new ExpThrow();
        this.aspectRatio = new AspectRatio();
        this.blockOutline = new BlockOutline();
        this.sprint = new Sprint();
        this.elytraSwap = new ElytraSwap();
        this.fullBright = new FullBright();
        this.useTracker = new UseTracker();
        this.autoEvent = new AutoEvent();
        this.autoSell = new AutoSell();
        this.autoMsg = new AutoMsg();
        this.funPay = new FunPay();
        this.specScan = new SpecScan();
        this.autoAccept = new AutoAccept();
        this.triggerBot = new TriggerBot();
        this.criticals = new Criticals();
        this.crystalAura = new CrystalAura();
        this.ahHelper = new AHHelper();
        this.noPush = new NoPush();
        this.projectileHelper = new ProjectileHelper();
        this.autoTool = new AutoTool();
        this.autoExplosion = new AutoExplosion();
        this.popEffect = new PopEffect();
        this.fireworkESP = new FireworkESP();
        this.aimAssistant = new AimAssistant();
        this.itemScroller = new ItemScroller();
        this.waterSpeed = new WaterSpeed();
        this.jumpCircles = new JumpCircles();
        this.freeCam = new FreeCam();
        this.swingAnimation = new SwingAnimation();
        this.predictions = new Predictions();
        this.webTrap = new WebTrap();
        this.viewModel = new ViewModel();
        this.blockHighlight = new BlockHighlight();
        this.autoSwap = new AutoSwap();
        this.hitEffect = new HitEffect();
        this.autoTotem = new AutoTotem();
        this.noFall = new NoFall();
        this.handShader = new HandShader();
        this.betterMinecraft = new BetterMinecraft();
        this.guiWalk = new GuiWalk();
        this.removals = new Removals();
        this.noDelay = new NoDelay();
        this.arrows = new Arrows();
        this.freeLook = new FreeLook();
        this.serverHelper = new ServerHelper();
        this.clickPearl = new ClickPearl();
        this.timer = new Timer();
        this.airStuck = new AirStuck();
        this.fakePlayer = new FakePlayer();
        this.ambience = new Ambience();
        this.nameTags = new NameTags();
        this.auraModule = new AuraModule();
        this.worldParticles = new WorldParticles();
        this.particles = new Particles();
        this.clientSounds = new ClientSounds();
        this.unHook = new UnHook();
        this.renderDemoModule = new RenderDemoModule();
        this.discordRPC = DiscordRPC.getInstance();
        this.targetEsp = new TargetEsp();
        this.interpolateF5 = new InterpolateF5();
        this.obsidianFarm = new ObsidianFarm();
        this.deathCoords = new DeathCoords();
        this.keyFinderTeleport = new KeyFinderTeleport();
        this.pearlTarget = new PearlTarget();
        this.speedExploit = new SpeedExploit();
        this.speed = new Speed();
        this.elytraExploit = new ElytraExploit();
        this.playerFakelags = new PlayerFakelags();
        this.noSlow = new error.module.impl.movement.NoSlow();
        this.noWeb = new error.module.impl.movement.NoWeb();
        this.noClip = new error.module.impl.movement.NoClip();
        this.disabler = new error.module.impl.movement.Disabler();
        this.noFallExploit = new error.module.impl.movement.NoFallExploit();
        this.elytraBooster = new error.module.impl.movement.ElytraBooster();
        this.elytraResolver = new error.module.impl.movement.ElytraResolver();
        this.elytraSample = new error.module.impl.movement.ElytraSample();

        register(
                this.anInterface,
                this.notification,
                this.customModels,
                this.holdMyItems,
                this.maceHelper,
                this.maceSounds,
                this.autoCaptcha,
                this.irc,
                this.expThrow,
                this.aspectRatio,
                this.blockOutline,
                this.sprint,
                this.elytraSwap,
                this.fullBright,
                this.useTracker,
                this.autoEvent,
                this.autoSell,
                this.autoMsg,
                this.funPay,
                this.specScan,
                this.autoAccept,
                this.triggerBot,
                this.criticals,
                this.crystalAura,
                this.ahHelper,
                this.noPush,
                this.projectileHelper,
                this.autoTool,
                this.autoExplosion,
                this.popEffect,
                this.fireworkESP,
                this.aimAssistant,
                this.itemScroller,
                this.waterSpeed,
                this.jumpCircles,
                this.freeCam,
                this.swingAnimation,
                this.predictions,
                this.webTrap,
                this.viewModel,
                this.blockHighlight,
                this.autoSwap,
                this.hitEffect,
                this.autoTotem,
                this.noFall,
                this.handShader,
                this.betterMinecraft,
                this.guiWalk,
                this.removals,
                this.noDelay,
                this.arrows,
                this.freeLook,
                this.serverHelper,
                this.clickPearl,
                this.timer,
                this.airStuck,
                this.fakePlayer,
                this.ambience,
                this.nameTags,
                this.auraModule,
                this.worldParticles,
                this.particles,
                this.clientSounds,
                this.unHook,
                this.renderDemoModule,
                this.discordRPC,
                this.targetEsp,
                this.interpolateF5,
                this.obsidianFarm,
                this.deathCoords,
                this.keyFinderTeleport,
                this.pearlTarget,
                this.speedExploit,
                this.speed,
                this.elytraExploit,
                this.playerFakelags,
                this.noSlow,
                this.noWeb,
                this.noClip,
                this.disabler,
                this.noFallExploit,
                this.elytraBooster,
                this.elytraResolver,
                this.elytraSample
        );

        if (this.maceHelper != null) this.maceHelper.setState(true);
        if (this.autoSwap != null) this.autoSwap.setState(true);
        if (this.criticals != null) this.criticals.setState(true);
    }

    public void register(Module... mods) {
        for (Module mod : mods) {
            if (mod != null && !modules.contains(mod)) {
                modules.add(mod);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (module != null && module.getClass() == clazz) {
                return (T) module;
            }
        }
        return null;
    }

    public Module getModule(String name) {
        if (name == null) return null;
        for (Module module : modules) {
            if (module != null && module.getName() != null && module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream()
                .filter(m -> m != null && m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public void onKey(int key, int action) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == 0) return;
        if (error.ui.clickgui.LiquidClickGui.isOpen) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.gui.screen() != null) return;
        if (this.clickGui != null && this.clickGui.getBind() != null && this.clickGui.getBind().matches(key)) {
            if (action == GLFW.GLFW_PRESS) {
                this.clickGui.toggle();
                return;
            }
        }
        for (Module module : modules) {
            if (module != null && module.getBind() != null && module.getBind().matches(key)) {
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
        if (error.ui.clickgui.LiquidClickGui.isOpen) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.gui.screen() != null) return;
        if (this.clickGui != null && this.clickGui.getBind() != null && this.clickGui.getBind().matchesMouse(button)) {
            if (action == GLFW.GLFW_PRESS) {
                this.clickGui.toggle();
                return;
            }
        }
        for (Module module : modules) {
            if (module != null && module.getBind() != null && module.getBind().matchesMouse(button)) {
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