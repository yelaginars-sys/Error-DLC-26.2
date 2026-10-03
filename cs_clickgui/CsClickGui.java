package dev.liteproject.client.ui;

import dev.liteproject.client.ClientCore;
import dev.liteproject.client.render.ui.UiDrawList;
import dev.liteproject.client.ui.CsClickGuiModel.Category;
import dev.liteproject.client.ui.CsClickGuiModel.Choice;
import dev.liteproject.client.ui.CsClickGuiModel.ColorValue;
import dev.liteproject.client.ui.CsClickGuiModel.Group;
import dev.liteproject.client.ui.CsClickGuiModel.Key;
import dev.liteproject.client.ui.CsClickGuiModel.Module;
import dev.liteproject.client.ui.CsClickGuiModel.Setting;
import dev.liteproject.client.ui.CsClickGuiModel.Slider;
import dev.liteproject.client.ui.CsClickGuiModel.Toggle;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class CsClickGui extends Screen {
	private static final int WHITE=0xFFF7F4EC, TEXT=0xFFD9D5C9, MUTED=0xFF898A85;
	private static int ACCENT=0xFFFF8FB9;
	private static int rememberedCategory;
	private static final int PANEL=0xF70F0F0F;
	private final List<Category> categories=CsClickGuiModel.create();
	private int categoryIndex=rememberedCategory;
	private final Set<Module> expandedModules=new HashSet<>();
	private final CsSettingsPanel settings=new CsSettingsPanel();
    private final CsRubberSegment categorySegment=new CsRubberSegment();
    private final CsTechTitle techTitle=new CsTechTitle();
    private final CsShapeWaves shapeWaves=new CsShapeWaves();
    private Module bindingModule;
	private boolean previousDown;
	private boolean previousRightDown;
	private boolean searchFocused;
    private boolean activeFirst;
    private boolean favoritesOnly;
    private boolean compactColumns;
    private final Set<Module> favorites=new HashSet<>();
	private String search="";
	private float scrollOffset,scrollTarget,maxScroll;
	private long lastFrame=System.nanoTime();

	public CsClickGui(){
        super(Component.literal("Lite Project CsClickGui"));
        for(Category category:categories)for(Module module:category.modules()){
            if(!module.settings.isEmpty())expandedModules.add(module);
            if(module.enabled)favorites.add(module);
        }
    }

	@Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float tickDelta){
		long now=System.nanoTime();
		float dt=Math.min(.05f,(now-lastFrame)/1_000_000_000f);
		lastFrame=now;
		boolean down=GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(),
			GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;
		boolean pressed=down&&!previousDown;
		boolean rightDown=GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(),
			GLFW.GLFW_MOUSE_BUTTON_RIGHT)==GLFW.GLFW_PRESS;
		boolean rightPressed=rightDown&&!previousRightDown;
		Layout l=Layout.create(width,height);
		boolean popupWasOpen=settings.popupOpen();
        settings.dismissOutside(mouseX,mouseY,pressed);
        boolean basePressed=pressed&&!popupWasOpen,baseDown=down&&!popupWasOpen,baseRightPressed=rightPressed&&!popupWasOpen;
        settings.beginFrame(ACCENT);
		UiDrawList ui=new UiDrawList();
		backdrop(ui,l,mouseX,mouseY,down,pressed,dt);
		sidebar(ui,l,mouseX,mouseY,baseDown,basePressed,dt);
		
		
		Category category=categories.get(categoryIndex);
        header(ui,l,category,mouseX,mouseY,basePressed);
        moduleGrid(ui,l,category,mouseX,mouseY,baseDown,basePressed,baseRightPressed,dt);
		settings.drawPopups(ui,new CsSettingsPanel.Bounds(l.x,l.y,l.w,l.h,l.mainX,l.mainW,l.s),mouseX,mouseY,down,pressed);
		ClientCore.getInstance().renderer().submit(graphics,ui);
		if(!down)settings.released();
		previousDown=down;
		previousRightDown=rightDown;
	}

    private void backdrop(UiDrawList ui,Layout l,int mx,int my,boolean down,boolean pressed,float dt){
        shapeWaves.draw(ui,width,height,mx,my,dt);
        for(int i=5;i>0;i--){
            float spread=i*2.2f*l.s;
            ui.roundedRect(l.x-spread,l.y-spread+3*l.s,l.w+spread*2,l.h+spread*2,13*l.s,0x07000000);
        }
        ui.roundedRect(l.x,l.y,l.w,l.h,9*l.s,PANEL)
          .outlineRoundedRect(l.x,l.y,l.w,l.h,9*l.s,.7f*l.s,0xFF202020)
          .line(l.x+12*l.s,l.y+52*l.s,l.x+l.w-12*l.s,l.y+52*l.s,.65f*l.s,0xFF242424);
        float cx=l.x+l.w/2;
        ui.icon(cx,l.y-60*l.s,46*l.s,ACCENT,0xE001);
        if(l.y+l.h+54*l.s<height-2*l.s)
            techTitle.draw(ui,cx,l.y+l.h+54*l.s,31*l.s,l.s,mx,my,down,pressed,dt);
        float ax=l.x-5*l.s,ay=l.y-5*l.s,bx=l.x+l.w+5*l.s,by=l.y+l.h+5*l.s;
        ui.line(ax,ay,ax+14*l.s,ay,.6f*l.s,0x458C7C5E)
          .line(ax,ay,ax,ay+14*l.s,.6f*l.s,0x458C7C5E)
          .line(bx-14*l.s,ay,bx,ay,.6f*l.s,0x458C7C5E)
          .line(bx,ay,bx,ay+14*l.s,.6f*l.s,0x458C7C5E)
          .line(ax,by-14*l.s,ax,by,.6f*l.s,0x458C7C5E)
          .line(ax,by,ax+14*l.s,by,.6f*l.s,0x458C7C5E)
          .line(bx-14*l.s,by,bx,by,.6f*l.s,0x458C7C5E)
          .line(bx,by-14*l.s,bx,by,.6f*l.s,0x458C7C5E);
    }

    private void sidebar(UiDrawList ui,Layout l,int mx,int my,boolean down,boolean pressed,float dt){
        float cy=l.y+27*l.s;
        float logoX=l.x+14*l.s;
        ui.roundedRect(logoX,l.y+13*l.s,27*l.s,27*l.s,6*l.s,ACCENT)
          .icon(logoX+13.5f*l.s,cy,15*l.s,0xFF291923,0xE001);

        float right=l.x+l.w-14*l.s;
        float themeX=right-27*l.s;
        float quickX=themeX-88*l.s;
        float availableLeft=logoX+36*l.s;
        float availableRight=quickX-11*l.s;
        float trackWidth=CsRubberSegment.width(categories.size(),l.s);
        float trackX=availableLeft+Math.max(0,availableRight-availableLeft-trackWidth)*.5f;
        float innerX=trackX+2.5f*l.s;
        int next=categorySegment.draw(ui,categories,categoryIndex,innerX,cy,l.s,ACCENT,mx,my,down,pressed,dt);
        if(next!=categoryIndex){
            categoryIndex=next;rememberedCategory=next;settings.released();
            settings.closePopups();scrollOffset=scrollTarget=0;
        }

        if(pressed&&hit(mx,my,themeX,cy-13*l.s,27*l.s,27*l.s))ACCENT=ACCENT==0xFFFF8FB9?0xFFFFBDD4:0xFFFF8FB9;
        ui.roundedRect(themeX,cy-13*l.s,27*l.s,27*l.s,6*l.s,ACCENT)
          .icon(themeX+13.5f*l.s,cy,11*l.s,0xFF291923,0xE51C);
        for(int i=0;i<3;i++)ui.roundedRect(quickX+i*28*l.s,cy-13*l.s,25*l.s,27*l.s,5*l.s,0xFF171717)
          .outlineRoundedRect(quickX+i*28*l.s,cy-13*l.s,25*l.s,27*l.s,5*l.s,.5f*l.s,0xFF242424);
        if(pressed&&hit(mx,my,quickX,cy-13*l.s,25*l.s,27*l.s))favoritesOnly=!favoritesOnly;
        if(pressed&&hit(mx,my,quickX+28*l.s,cy-13*l.s,25*l.s,27*l.s))compactColumns=!compactColumns;
        if(pressed&&hit(mx,my,quickX+56*l.s,cy-13*l.s,25*l.s,27*l.s))searchFocused=true;
        ui.icon(quickX+12.5f*l.s,cy,10*l.s,favoritesOnly?ACCENT:MUTED,0xE838)
          .icon(quickX+40.5f*l.s,cy,10*l.s,compactColumns?ACCENT:MUTED,0xE871)
          .icon(quickX+68.5f*l.s,cy,10*l.s,searchFocused?ACCENT:MUTED,0xE8B6);
    }
    private void header(UiDrawList ui,Layout l,Category category,int mx,int my,boolean pressed){
        float cy=l.y+79*l.s;
        ui.icon(l.mainX+12*l.s,cy,21*l.s,ACCENT,category.icon());
        ui.strongText(l.mainX+31*l.s,cy-2*l.s,11*l.s,WHITE,category.name());
        long enabled=category.modules().stream().filter(m->m.enabled).count();
        float badgeX=l.mainX+31*l.s+Math.min(56,category.name().length()*7+8)*l.s;
        ui.roundedRect(badgeX,cy-10*l.s,42*l.s,13*l.s,4*l.s,0xFF292318)
          .centeredText(badgeX+21*l.s,cy-3.5f*l.s,7*l.s,ACCENT,enabled+" Active",true);
        ui.roundedRect(badgeX+45*l.s,cy-10*l.s,37*l.s,13*l.s,4*l.s,0xFF242424)
          .centeredText(badgeX+63.5f*l.s,cy-3.5f*l.s,7*l.s,TEXT,category.modules().size()+" Total",false);
        ui.text(l.mainX+31*l.s,cy+12*l.s,7.6f*l.s,MUTED,category.description());
        float searchW=160*l.s,searchX=l.x+l.w-16*l.s-searchW,searchY=l.y+80*l.s;
        if(pressed&&my>l.y+52*l.s)searchFocused=hit(mx,my,searchX,searchY-12*l.s,searchW,24*l.s);
        ui.roundedRect(searchX,searchY-12*l.s,searchW,24*l.s,5*l.s,0xFF181818)
          .outlineRoundedRect(searchX,searchY-12*l.s,searchW,24*l.s,5*l.s,.6f*l.s,searchFocused?ACCENT:0xFF272727)
          .icon(searchX+searchW-11*l.s,searchY,10*l.s,MUTED,0xE8B6);
        String shown=search.isEmpty()?"Search for modules...":search;
        if(searchFocused&&(System.currentTimeMillis()/500)%2==0)shown+="_";
        ui.text(searchX+9*l.s,searchY,7.7f*l.s,search.isEmpty()?MUTED:TEXT,shown);
        float sortX=searchX-104*l.s,sortW=98*l.s;
        if(pressed&&hit(mx,my,sortX,searchY-12*l.s,sortW,24*l.s))activeFirst=!activeFirst;
        ui.roundedRect(sortX,searchY-12*l.s,sortW,24*l.s,5*l.s,0xFF181818)
          .outlineRoundedRect(sortX,searchY-12*l.s,sortW,24*l.s,5*l.s,.6f*l.s,0xFF272727)
          .text(sortX+9*l.s,searchY,7.7f*l.s,TEXT,activeFirst?"Active first":"Default order")
          .icon(sortX+sortW-10*l.s,searchY,8*l.s,MUTED,0xE5CF);
        float allX=sortX-59*l.s;
        if(pressed&&hit(mx,my,allX,searchY-12*l.s,24*l.s,24*l.s)){
            boolean anyCollapsed=category.modules().stream().anyMatch(m->!m.settings.isEmpty()&&!expandedModules.contains(m));
            for(Module m:category.modules())if(!m.settings.isEmpty()){
                if(anyCollapsed)expandedModules.add(m);else expandedModules.remove(m);
            }
        }
        if(pressed&&hit(mx,my,allX+29*l.s,searchY-12*l.s,24*l.s,24*l.s))favoritesOnly=!favoritesOnly;
        ui.roundedRect(allX,searchY-12*l.s,24*l.s,24*l.s,5*l.s,ACCENT)
          .icon(allX+12*l.s,searchY,10*l.s,0xFF221B11,0xE8B8)
          .roundedRect(allX+29*l.s,searchY-12*l.s,24*l.s,24*l.s,5*l.s,0xFF181818)
          .outlineRoundedRect(allX+29*l.s,searchY-12*l.s,24*l.s,24*l.s,5*l.s,.6f*l.s,0xFF272727)
          .icon(allX+41*l.s,searchY,10*l.s,favoritesOnly?ACCENT:MUTED,0xE838);
    }
    private void moduleGrid(UiDrawList ui,Layout l,Category category,int mx,int my,boolean down,boolean pressed,boolean rightPressed,float dt){
        List<Module> visible=new ArrayList<>();
        String query=search.toLowerCase(Locale.ROOT).trim();
        if(query.isEmpty())visible.addAll(category.modules());
        else for(Category source:categories)for(Module m:source.modules())
            if(m.name.toLowerCase(Locale.ROOT).contains(query)||m.description.toLowerCase(Locale.ROOT).contains(query))visible.add(m);
        if(favoritesOnly)visible.removeIf(m->!favorites.contains(m));
        if(activeFirst)visible.sort((a,b)->Boolean.compare(b.enabled,a.enabled));
        int columns=compactColumns?2:3;
        float gap=7*l.s,columnW=(l.mainW-gap*(columns-1))/columns;
        scrollOffset+=(scrollTarget-scrollOffset)*Math.min(1,dt*18);
        float viewportTop=l.y+119*l.s,viewportBottom=l.y+l.h-12*l.s;
        float[] ys=new float[columns];
        for(int i=0;i<columns;i++)ys[i]=viewportTop+5*l.s-scrollOffset;
        ui.pushScissor(l.mainX,l.y+119*l.s,l.mainW,l.h-131*l.s);
        
        for(int i=0;i<visible.size();i++){
            Module m=visible.get(i);int column=i%columns;
            float x=l.mainX+column*(columnW+gap),y=ys[column];
            float baseH=CsCardRenderer.baseHeight(m,l.s),settingsH=expandedModules.contains(m)?CsSettingsPanel.height(m,l.s):0;
            float cardH=baseH+settingsH;
            boolean pointerInView=hit(mx,my,l.mainX,viewportTop,l.mainW,viewportBottom-viewportTop);
            boolean over=pointerInView&&hit(mx,my,x,y,columnW,33*l.s);
            boolean star=pressed&&over&&hit(mx,my,x+7*l.s,y+6*l.s,19*l.s,20*l.s);
            boolean settingsButton=pressed&&over&&hit(mx,my,x+columnW-45*l.s,y+5*l.s,17*l.s,22*l.s);
            boolean bindButton=pressed&&pointerInView&&hit(mx,my,x+columnW-44*l.s,y+42*l.s,34*l.s,17*l.s);
            if(bindButton){bindingModule=m;searchFocused=false;}
            if(star){
                if(!favorites.add(m))favorites.remove(m);
            }else if(settingsButton){
                if(!expandedModules.add(m))expandedModules.remove(m);
                settings.closePopups();
            }else if(pressed&&over){
                m.toggle();
            }
            if(rightPressed&&over){
                if(!expandedModules.add(m))expandedModules.remove(m);
                settings.closePopups();
            }
            CsCardRenderer.draw(ui,m,x,y,columnW,cardH,l.s,ACCENT,favorites.contains(m),expandedModules.contains(m),over,
                CsKeybindManager.get().label(m.id()),bindingModule==m,dt);
            if(expandedModules.contains(m))settings.drawSettings(ui,new CsSettingsPanel.Bounds(l.x,l.y,l.w,l.h,l.mainX,l.mainW,l.s),m,x+10*l.s,y+baseH+4*l.s,columnW-20*l.s,
                    pointerInView?mx:-100000,pointerInView?my:-100000,down&&pointerInView,pressed&&pointerInView,dt);
            ys[column]+=cardH+7*l.s;
        }
        float contentBottom=ys[0];for(int i=1;i<columns;i++)contentBottom=Math.max(contentBottom,ys[i]);
        maxScroll=Math.max(0,contentBottom+scrollOffset-viewportBottom);
        scrollTarget=Math.max(0,Math.min(maxScroll,scrollTarget));
        if(visible.isEmpty())ui.centeredText(l.mainX+l.mainW/2,l.y+l.h/2,10*l.s,MUTED,
            favoritesOnly?"No starred modules":"No modules found",false);
        ui.popScissor();
        if(maxScroll>1){
            float trackH=viewportBottom-viewportTop;
            float thumbH=Math.max(18*l.s,trackH*trackH/(trackH+maxScroll));
            float barX=l.mainX+l.mainW+5*l.s;
            ui.roundedRect(barX,viewportTop,2.3f*l.s,trackH,1.15f*l.s,0xFF313333);
            ui.roundedRect(barX,viewportTop+(trackH-thumbH)*scrollOffset/maxScroll,
                2.3f*l.s,thumbH,1.15f*l.s,0xFFB4B7B6);
        }    }

private static boolean hit(float mx,float my,float x,float y,float w,float h){
        return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;
    }
	@Override public boolean charTyped(CharacterEvent event){
		if(searchFocused&&event.isAllowedChatCharacter()&&search.length()<28){
			search+=event.codepointAsString();scrollOffset=scrollTarget=0;
			return true;
		}
		return super.charTyped(event);
	}
	@Override public boolean keyPressed(KeyEvent event){
        if(bindingModule!=null){
            Module target=bindingModule;
            bindingModule=null;
            if(event.key()==GLFW.GLFW_KEY_ESCAPE)return true;
            CsKeybindManager manager=CsKeybindManager.get();
            int previous=manager.binding(target.id());
            int key=event.key()==GLFW.GLFW_KEY_BACKSPACE||event.key()==GLFW.GLFW_KEY_DELETE
                ?GLFW.GLFW_KEY_UNKNOWN:event.key();
            manager.bind(target.id(),key);
            int current=manager.binding(target.id());
            return true;
        }		if(searchFocused){
			if(event.key()==GLFW.GLFW_KEY_BACKSPACE){
				if(!search.isEmpty())search=search.substring(0,search.offsetByCodePoints(search.length(),-1));scrollOffset=scrollTarget=0;
				return true;
			}
			if(event.key()==GLFW.GLFW_KEY_ENTER){searchFocused=false;return true;}
			if(event.key()==GLFW.GLFW_KEY_ESCAPE){searchFocused=false;return true;}
		}
		return super.keyPressed(event);
	}
	@Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){
		Layout l=Layout.create(width,height);
		if(hit((float)mouseX,(float)mouseY,l.mainX,l.y+119*l.s,l.mainW,l.h-131*l.s)){
			scrollTarget=Math.max(0,Math.min(maxScroll,scrollTarget-(float)verticalAmount*36*l.s));
			settings.closePopups();
			return true;
		}
		return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
	}
	@Override public boolean isPauseScreen(){return false;}

	private record Layout(float x,float y,float w,float h,float s,float side,float mainX,float mainW){
		static Layout create(int sw,int sh){
			float designW=900,designH=550,margin=18;
			float fitScale=Math.min((sw-margin*2)/designW,(sh-margin*2)/designH);
			float s=Math.max(.10f,Math.min(.62f,fitScale));
			float w=designW*s,h=designH*s;
			float x=(sw-w)/2,y=(sh-h)/2,side=0,mainX=x+17*s;
			return new Layout(x,y,w,h,s,side,mainX,x+w-mainX-17*s);
		}
	}
}
