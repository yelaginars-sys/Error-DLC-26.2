package dev.liteproject.client.ui;

import dev.liteproject.client.ClientCore;
import dev.liteproject.client.module.ModuleManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Independent, persistent module binds used by CsClickGui and the Keybinds HUD. */
public final class CsKeybindManager {
    private static final CsKeybindManager INSTANCE=new CsKeybindManager();
    private final Map<String,Integer> bindings=new LinkedHashMap<>();
    private final Map<Integer,Boolean> keyWasDown=new HashMap<>();
    private Path file;
    private boolean loaded;

    public static CsKeybindManager get(){return INSTANCE;}

    private void load(){
        if(loaded)return;
        loaded=true;
        file=Minecraft.getInstance().gameDirectory.toPath().resolve("liteproject").resolve("cs-binds.properties");
        if(!Files.isRegularFile(file))return;
        Properties values=new Properties();
        try(InputStream in=Files.newInputStream(file)){
            values.load(in);
            for(String id:values.stringPropertyNames()){
                try{
                    int key=Integer.parseInt(values.getProperty(id));
                    if(key>0&&key<=GLFW.GLFW_KEY_LAST)bindings.put(id,key);
                }catch(NumberFormatException ignored){}
            }
        }catch(IOException error){
            ClientCore.LOGGER.warn("Could not read CsClickGui binds",error);
        }
    }

    public int binding(String id){load();return bindings.getOrDefault(id,GLFW.GLFW_KEY_UNKNOWN);}
    public void bind(String id,int key){
        load();
        if(key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_RIGHT_SHIFT)return;
        if(key<=0||key>GLFW.GLFW_KEY_LAST)bindings.remove(id);
        else{
            bindings.entrySet().removeIf(entry->entry.getValue()==key&&!entry.getKey().equals(id));
            bindings.put(id,key);
        }
        keyWasDown.clear();
        save();
    }

    public void tick(Minecraft client,ModuleManager modules){
        load();
        if(client.getWindow()==null)return;
        long window=client.getWindow().handle();
        for(Map.Entry<String,Integer> entry:bindings.entrySet()){
            int key=entry.getValue();
            boolean down=GLFW.glfwGetKey(window,key)==GLFW.GLFW_PRESS;
            boolean was=keyWasDown.getOrDefault(key,false);
            keyWasDown.put(key,down);
            if(down&&!was&&client.screen==null)modules.find(entry.getKey()).ifPresent(module->module.toggle());
        }
    }

    public String label(String id){return keyName(binding(id));}
    public static String keyName(int key){
        if(key<=0)return "BIND";
        String name=GLFW.glfwGetKeyName(key,0);
        if(name!=null&&!name.isBlank())return name.toUpperCase(java.util.Locale.ROOT);
        if(key>=GLFW.GLFW_KEY_F1&&key<=GLFW.GLFW_KEY_F25)return "F"+(key-GLFW.GLFW_KEY_F1+1);
        return switch(key){
            case GLFW.GLFW_KEY_LEFT_SHIFT->"L SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT->"R SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL->"L CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL->"R CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT->"L ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT->"R ALT";
            case GLFW.GLFW_KEY_SPACE->"SPACE";
            case GLFW.GLFW_KEY_TAB->"TAB";
            case GLFW.GLFW_KEY_ENTER->"ENTER";
            case GLFW.GLFW_KEY_DELETE->"DELETE";
            case GLFW.GLFW_KEY_UP->"UP";
            case GLFW.GLFW_KEY_DOWN->"DOWN";
            case GLFW.GLFW_KEY_LEFT->"LEFT";
            case GLFW.GLFW_KEY_RIGHT->"RIGHT";
            default->"KEY "+key;
        };
    }

    private void save(){
        if(file==null)return;
        Properties values=new Properties();
        bindings.forEach((id,key)->values.setProperty(id,Integer.toString(key)));
        try{
            Files.createDirectories(file.getParent());
            try(OutputStream out=Files.newOutputStream(file)){values.store(out,"Lite Project CsClickGui module binds");}
        }catch(IOException error){
            ClientCore.LOGGER.warn("Could not save CsClickGui binds",error);
        }
    }

    private CsKeybindManager(){}
}