package dev.syntrix.clienttest.client.combat;
import java.util.*;
import dev.syntrix.clienttest.client.gui.ClickGuiState;
import dev.syntrix.clienttest.client.visual.VisualCatalog;
import dev.syntrix.clienttest.client.combat.meow.combat.CrystalAura;
import dev.syntrix.clienttest.client.combat.meow.setting.*;
public final class CrystalAuraSettings {
    public static final String ID="crystal-aura";
    private static VisualCatalog.Setting number(String id,String label,double value,double min,double max,double step) {
        return new VisualCatalog.Setting(id,label,VisualCatalog.Kind.NUMBER,value,min,max,step,List.of());
    }
    private static VisualCatalog.Setting toggle(String id,String label,boolean value) {
        return new VisualCatalog.Setting(id,label,VisualCatalog.Kind.TOGGLE,value?1:0,0,1,1,List.of());
    }
    public static final VisualCatalog.Definition DEFINITION=new VisualCatalog.Definition(ID,"CrystalAura","Crystal placement, explosions and obstacle mining",List.of(
        new VisualCatalog.Setting("crystalAuraMode","Mode",VisualCatalog.Kind.CHOICE,0,0,1,1,List.of("Custom","RW")),
        number("crystalAuraTargetRange","Target range",8,4,12,.5),
        number("crystalAuraPlaceRange","Place range",4.5,3,6,.1),
        number("crystalAuraBreakRange","Break range",3,1,6,.1),
        number("crystalAuraMinDamage","Minimum damage",6,1,20,.5),
        number("crystalAuraMaxSelfDamage","Maximum self damage",8,0,20,.5),
        number("crystalAuraDelay","Action delay (ms)",0,0,500,10),
        number("crystalAuraPredictTicks","Prediction (ticks)",2,0,4,1),
        toggle("crystalAuraPlaceObsidian","Place obsidian",true),
        toggle("crystalAuraLegit","Hold crystal when attacking",false),
        toggle("crystalAuraAntiSuicide","Anti-suicide",true),
        toggle("crystalAuraAutoDig","Mine obstacles",true),
        toggle("crystalAuraSkipObsidianDig","Skip obsidian mining",false),
        toggle("crystalAuraRender","Render actions",true),
        toggle("crystalAuraLogs","Diagnostic log",false),
        toggle("crystalAuraSelfShield","Self shield",false)
    ));
    public static List<VisualCatalog.Setting> visible(ClickGuiState.Module state) {
        return state.values.getOrDefault("crystalAuraMode",0D)==1
            ? DEFINITION.settings().stream().filter(s->s.kind()!=VisualCatalog.Kind.NUMBER).toList():DEFINITION.settings();
    }
    public static void sync(CrystalAura aura,ClickGuiState.Module state) {
        for(Setting setting:aura.getSettings()) {
            if(setting instanceof ModeSetting mode) mode.setSelectedIndex((int)value(state,"crystalAuraMode"));
            else if(setting instanceof NumberSetting number) number.setValue(value(state,number.getName()));
            else if(setting instanceof MultiBooleanSetting options) {
                for(String option:options.getOptions()) {
                    if(option.equals("crystalAuraYieldWebTrap"))continue; // No WebTrap module in this client.
                    if(value(state,option)>=.5)options.enable(option);else options.disable(option);
                }
            }
        }
    }
    private static double value(ClickGuiState.Module state,String id) {
        var spec=DEFINITION.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();
        double value=state.values.getOrDefault(id,spec.initial());
        return Double.isFinite(value)?Math.clamp(value,spec.min(),spec.max()):spec.initial();
    }
    private CrystalAuraSettings() {}
}
