package dev.liteproject.client.ui;

import dev.liteproject.client.render.ui.UiDrawList;
import dev.liteproject.client.ui.CsClickGuiModel.Module;
import dev.liteproject.client.ui.CsClickGuiModel.Setting;
import dev.liteproject.client.ui.CsClickGuiModel.Toggle;
import dev.liteproject.client.ui.CsClickGuiModel.Slider;
import dev.liteproject.client.ui.CsClickGuiModel.Choice;
import dev.liteproject.client.ui.CsClickGuiModel.Group;
import dev.liteproject.client.ui.CsClickGuiModel.ColorValue;
import dev.liteproject.client.ui.CsClickGuiModel.Key;

/** Settings rows and foreground popups for the new CsClickGui only. */
final class CsSettingsPanel {
    private static final int WHITE=0xFFF7F4EC,TEXT=0xFFD9D5C9,MUTED=0xFF898A85,PANEL=0xFF151515;
    private int ACCENT=0xFFFF8FB9;
    private Slider dragging;
    private Choice openChoice;
    private Dropdown dropdown;
    private Group openGroup;
    private GroupDropdown groupDropdown;
    private ColorValue openColor;
    private ColorPicker colorPicker;
    private ColorDrag colorDrag;

    record Bounds(float x,float y,float w,float h,float mainX,float mainW,float s){}

    boolean popupOpen(){
        return (openChoice!=null&&dropdown!=null)||(openGroup!=null&&groupDropdown!=null)||(openColor!=null&&colorPicker!=null);
    }
    void dismissOutside(int mx,int my,boolean pressed){
        if(!pressed)return;
        if(openChoice!=null&&dropdown!=null&&!dropdown.contains(mx,my))openChoice=null;
        if(openGroup!=null&&groupDropdown!=null&&!groupDropdown.contains(mx,my))openGroup=null;
        if(openColor!=null&&colorPicker!=null&&!colorPicker.contains(mx,my)){openColor=null;colorDrag=null;}
    }
    void beginFrame(int accent){
        ACCENT=accent;
        dropdown=null;
        groupDropdown=null;
        colorPicker=null;
    }
    void drawPopups(UiDrawList ui,Bounds bounds,int mx,int my,boolean down,boolean pressed){
        drawDropdown(ui,bounds,mx,my,pressed);
        drawColorPicker(ui,bounds,mx,my,down,pressed);
    }
    void closePopups(){openChoice=null;openGroup=null;openColor=null;colorDrag=null;}
    void released(){dragging=null;colorDrag=null;}
    void drawSettings(UiDrawList ui,Bounds l,Module module,float x,float y,float w,
            int mx,int my,boolean down,boolean pressed,float dt){
        ui.line(x,y-4*l.s,x+w,y-4*l.s,.6f*l.s,0xFF292929);
        for(Setting setting:module.settings){
            if(!setting.visible())continue;
            if(setting instanceof Toggle t){
                ui.strongText(x,y+9*l.s,8.2f*l.s,TEXT,t.name());
                if(!t.hint().isBlank())settingHint(ui,t.hint(),x,y+20*l.s,l.s);
                if(pressed&&hit(mx,my,x,y,w,29*l.s)){
                    t.toggle();
                }
                float p=t.animation.update(dt,20);
                ui.roundedRect(x+w-13*l.s,y+5*l.s,11*l.s,11*l.s,3*l.s,p>.5f?ACCENT:0xFF2A2A2A);
                if(p>.5f)ui.icon(x+w-7.5f*l.s,y+10.5f*l.s,7*l.s,0xFF151515,0xE5CA);
                y+=(31+hintExtra(setting.hint()))*l.s;
            }else if(setting instanceof Slider s){
                ui.strongText(x,y+8*l.s,8.2f*l.s,TEXT,s.name())
                  .rightText(x+w,y+8*l.s,7*l.s,WHITE,s.value(),true);
                if(!s.hint().isBlank())settingHint(ui,s.hint(),x,y+20*l.s,l.s);
                float tx=x+w*.48f,tw=w*.39f,ty=y+23*l.s;
                if(pressed&&hit(mx,my,tx,ty-5*l.s,tw,12*l.s)){dragging=s;}
                if(down&&dragging==s){s.model.setFromMouse(mx,tx,tw);s.sync();}
                float p=s.model.update(dt);
                ui.roundedRect(tx,ty,tw,3*l.s,1.5f*l.s,0xFF343434)
                  .roundedRect(tx,ty,Math.max(l.s,tw*p),3*l.s,1.5f*l.s,ACCENT)
                  .circle(tx+tw*p,ty+1.5f*l.s,2.7f*l.s,ACCENT);
                y+=(36+hintExtra(setting.hint()))*l.s;
            }else if(setting instanceof Choice c){
                ui.strongText(x,y+9*l.s,8.2f*l.s,TEXT,c.name());
                if(!c.hint().isBlank())settingHint(ui,c.hint(),x,y+20*l.s,l.s);
                float sw=Math.min(100*l.s,w*.44f),sx=x+w-sw,sy=y+3*l.s;
                if(pressed&&hit(mx,my,sx,sy,sw,23*l.s)){openChoice=openChoice==c?null:c;openGroup=null;openColor=null;}
                ui.roundedRect(sx,sy,sw,23*l.s,4*l.s,0xFF242424)
                  .text(sx+7*l.s,sy+11.5f*l.s,7*l.s,TEXT,c.value())
                  .icon(sx+sw-9*l.s,sy+11.5f*l.s,8*l.s,openChoice==c?ACCENT:MUTED,openChoice==c?0xE5CE:0xE5CF);
                if(openChoice==c)dropdown=new Dropdown(c,sx,sy,sw,23*l.s,19*l.s,sy+(c.values.size()*19+31)*l.s>l.y+l.h-8*l.s);
                y+=(34+hintExtra(setting.hint()))*l.s;
            }else if(setting instanceof Group g){
                ui.strongText(x,y+9*l.s,8.2f*l.s,TEXT,g.name());
                if(!g.hint().isBlank())settingHint(ui,g.hint(),x,y+20*l.s,l.s);
                float sw=Math.min(100*l.s,w*.44f),sx=x+w-sw,sy=y+3*l.s;
                if(pressed&&hit(mx,my,sx,sy,sw,23*l.s)){openGroup=openGroup==g?null:g;openChoice=null;openColor=null;}
                ui.roundedRect(sx,sy,sw,23*l.s,4*l.s,0xFF242424)
                  .text(sx+7*l.s,sy+11.5f*l.s,6.6f*l.s,TEXT,g.summary())
                  .icon(sx+sw-9*l.s,sy+11.5f*l.s,8*l.s,openGroup==g?ACCENT:MUTED,openGroup==g?0xE5CE:0xE5CF);
                if(openGroup==g)groupDropdown=new GroupDropdown(g,sx,sy,Math.max(sw,107*l.s),23*l.s,19*l.s,sy+(g.values.size()*19+31)*l.s>l.y+l.h-8*l.s);
                y+=(34+hintExtra(setting.hint()))*l.s;
            }else if(setting instanceof ColorValue c){
                ui.strongText(x,y+9*l.s,8.2f*l.s,TEXT,c.name())
                  .rightText(x+w-21*l.s,y+9*l.s,7*l.s,TEXT,c.value(),true);
                if(!c.hint().isBlank())settingHint(ui,c.hint(),x,y+20*l.s,l.s);
                float sx=x+w-15*l.s;
                if(pressed&&hit(mx,my,x,y,w,27*l.s)){openColor=openColor==c?null:c;openChoice=null;openGroup=null;colorDrag=null;}
                ui.roundedRect(sx,y+3*l.s,12*l.s,12*l.s,3*l.s,c.argb())
                  .outlineRoundedRect(sx,y+3*l.s,12*l.s,12*l.s,3*l.s,Math.max(.5f,l.s*.6f),openColor==c?WHITE:0x55FFFFFF);
                if(openColor==c){float pw=126*l.s,ph=105*l.s,px=Math.max(l.mainX,Math.min(l.mainX+l.mainW-pw,sx+12*l.s-pw)),py=Math.min(l.y+l.h-ph-5*l.s,y+20*l.s);colorPicker=new ColorPicker(c,px,py,pw,ph);}
                y+=(32+hintExtra(setting.hint()))*l.s;
            }else if(setting instanceof Key k){
                ui.strongText(x,y+9*l.s,8.2f*l.s,TEXT,k.name());
                if(!k.hint().isBlank())settingHint(ui,k.hint(),x,y+20*l.s,l.s);
                float kw=43*l.s;
                ui.roundedRect(x+w-kw,y+2*l.s,kw,21*l.s,4*l.s,0xFF242424)
                  .centeredText(x+w-kw/2,y+12*l.s,6.5f*l.s,TEXT,k.key(),true);
                y+=(30+hintExtra(setting.hint()))*l.s;
            }
        }
    }
    private void drawDropdown(UiDrawList ui,Bounds l,int mx,int my,boolean pressed){
        Dropdown d=dropdown;
        if(d==null||openChoice!=d.choice){drawGroupDropdown(ui,l,mx,my,pressed);return;}
        float listH=d.choice.values.size()*d.rowH+4*l.s;
        float listY=d.up?d.y-listH-2*l.s:d.y+d.headerH+2*l.s;
        float panelY=Math.min(d.y,listY);
        if(pressed&&hit(mx,my,d.x,listY,d.w,listH)){
            int index=(int)((my-listY-2*l.s)/d.rowH);
            if(index>=0&&index<d.choice.values.size()){
                d.choice.select(index);
                openChoice=null;
                return;
            }
        }
        for(int i=4;i>0;i--)ui.roundedRect(d.x-i*l.s,panelY-i*l.s+3*l.s,d.w+i*2*l.s,d.headerH+listH+i*2*l.s,5*l.s,0x09000000);
        ui.roundedRect(d.x-.5f*l.s,panelY-.5f*l.s,d.w+l.s,d.headerH+listH+3*l.s,4.5f*l.s,0xFF493B25)
          .roundedRect(d.x,panelY,d.w,d.headerH+listH+2*l.s,4*l.s,0xFF171717)
          .roundedRect(d.x,d.y,d.w,d.headerH,4*l.s,0xFF242424)
          .text(d.x+6*l.s,d.y+d.headerH/2,7*l.s,WHITE,d.choice.value())
          .icon(d.x+d.w-7*l.s,d.y+d.headerH/2,8*l.s,ACCENT,d.up?0xE5CF:0xE5CE);
        for(int i=0;i<d.choice.values.size();i++){
            float ry=listY+2*l.s+i*d.rowH;
            boolean selected=i==d.choice.selected,hover=hit(mx,my,d.x+2*l.s,ry,d.w-4*l.s,d.rowH);
            if(selected||hover)ui.roundedRect(d.x+2*l.s,ry,d.w-4*l.s,d.rowH,3*l.s,selected?0xFF40351E:0xFF292929);
            ui.text(d.x+7*l.s,ry+d.rowH/2,7*l.s,selected?WHITE:TEXT,d.choice.values.get(i));
            if(selected)ui.icon(d.x+d.w-8*l.s,ry+d.rowH/2,7*l.s,ACCENT,0xE5CA);
        }
        drawGroupDropdown(ui,l,mx,my,pressed);
    }
    private void drawGroupDropdown(UiDrawList ui,Bounds l,int mx,int my,boolean pressed){
        GroupDropdown d=groupDropdown;
        if(d==null||openGroup!=d.group)return;
        float listH=d.group.values.size()*d.rowH+4*l.s;
        float listY=d.up?d.y-listH-2*l.s:d.y+d.headerH+2*l.s;
        float panelY=Math.min(d.y,listY);
        if(pressed&&hit(mx,my,d.x,listY,d.w,listH)){
            int index=(int)((my-listY-2*l.s)/d.rowH);
            if(index>=0&&index<d.group.values.size()){
                Toggle option=d.group.values.get(index);
                option.toggle();
            }
        }
        for(int i=4;i>0;i--)ui.roundedRect(d.x-i*l.s,panelY-i*l.s+3*l.s,d.w+i*2*l.s,d.headerH+listH+i*2*l.s,5*l.s,0x09000000);
        ui.roundedRect(d.x-.5f*l.s,panelY-.5f*l.s,d.w+l.s,d.headerH+listH+3*l.s,4.5f*l.s,0xFF493B25)
          .roundedRect(d.x,panelY,d.w,d.headerH+listH+2*l.s,4*l.s,0xFF171717)
          .roundedRect(d.x,d.y,d.w,d.headerH,4*l.s,0xFF242424)
          .text(d.x+6*l.s,d.y+d.headerH/2,7*l.s,WHITE,d.group.name())
          .rightText(d.x+d.w-7*l.s,d.y+d.headerH/2,6*l.s,MUTED,d.group.summary(),true);
        for(int i=0;i<d.group.values.size();i++){
            Toggle option=d.group.values.get(i);
            float ry=listY+2*l.s+i*d.rowH;
            boolean hover=hit(mx,my,d.x+2*l.s,ry,d.w-4*l.s,d.rowH);
            if(hover)ui.roundedRect(d.x+2*l.s,ry,d.w-4*l.s,d.rowH,3*l.s,0xFF292929);
            ui.text(d.x+7*l.s,ry+d.rowH/2,7*l.s,option.value?WHITE:TEXT,option.name())
              .roundedRect(d.x+d.w-14*l.s,ry+5*l.s,8*l.s,8*l.s,2*l.s,option.value?ACCENT:0xFF292929);
            if(option.value)ui.icon(d.x+d.w-10*l.s,ry+9*l.s,7*l.s,WHITE,0xE5CA);
        }
    }	private void drawColorPicker(UiDrawList ui,Bounds l,int mx,int my,boolean down,boolean pressed){
		ColorPicker d=colorPicker;
		if(d==null||openColor!=d.color)return;
		float pad=7*l.s,sbX=d.x+pad,sbY=d.y+17*l.s,sbW=d.w-pad*2,sbH=46*l.s;
		float hueY=d.y+69*l.s,hueH=7*l.s,alphaY=d.y+82*l.s,alphaH=7*l.s;
		if(pressed){
			if(hit(mx,my,sbX,sbY,sbW,sbH))colorDrag=ColorDrag.SATURATION_BRIGHTNESS;
			else if(hit(mx,my,sbX,hueY,sbW,hueH))colorDrag=ColorDrag.HUE;
			else if(hit(mx,my,sbX,alphaY,sbW,alphaH))colorDrag=ColorDrag.ALPHA;
		}
		if(down&&colorDrag!=null){
			if(colorDrag==ColorDrag.SATURATION_BRIGHTNESS)d.color.setHsb(d.color.hue(),clamp01((mx-sbX)/sbW),1-clamp01((my-sbY)/sbH));
			else if(colorDrag==ColorDrag.HUE)d.color.setHsb(clamp01((mx-sbX)/sbW),d.color.saturation(),d.color.brightness());
			else d.color.setAlpha(clamp01((mx-sbX)/sbW));
		}
		for(int i=4;i>0;i--)ui.roundedRect(d.x-i*l.s,d.y-i*l.s+3*l.s,d.w+i*2*l.s,d.h+i*2*l.s,6*l.s,0x0A000000);
		ui.roundedRect(d.x-.5f*l.s,d.y-.5f*l.s,d.w+l.s,d.h+l.s,5*l.s,0xFF493B25)
			.roundedRect(d.x,d.y,d.w,d.h,4.5f*l.s,0xFF171717)
			.strongText(d.x+pad,d.y+9*l.s,6.5f*l.s,WHITE,"Color")
			.rightText(d.x+d.w-pad,d.y+9*l.s,6*l.s,TEXT,d.color.value(),true);
		int hue=0xFF000000|(java.awt.Color.HSBtoRGB(d.color.hue(),1,1)&0xFFFFFF);
		ui.roundedRect(sbX,sbY,sbW,sbH,3*l.s,hue)
			.gradientRoundedRect(sbX,sbY,sbW,sbH,3*l.s,0xFFFFFFFF,0x00FFFFFF,true)
			.gradientRoundedRect(sbX,sbY,sbW,sbH,3*l.s,0x00000000,0xFF000000,false);
		float cx=sbX+sbW*d.color.saturation(),cy=sbY+sbH*(1-d.color.brightness());
		ui.circle(cx,cy,3*l.s,WHITE).circle(cx,cy,1.8f*l.s,0xFF000000|(d.color.argb()&0xFFFFFF));
		int[] rainbow={0xFFFF4D4D,0xFFFFFF4D,0xFF4DFF70,0xFF4DE8FF,0xFF596BFF,0xFFC44DFF,0xFFFF4D7F};
		for(int i=0;i<rainbow.length-1;i++){float rx=sbX+sbW*i/(rainbow.length-1f),rw=sbW/(rainbow.length-1f)+.5f;ui.gradientRoundedRect(rx,hueY,rw,hueH,i==0||i==rainbow.length-2?3*l.s:0,rainbow[i],rainbow[i+1],true);}
		float hx=sbX+sbW*d.color.hue();ui.roundedRect(hx-1.2f*l.s,hueY-1.5f*l.s,2.4f*l.s,hueH+3*l.s,1.2f*l.s,WHITE);
		float tile=3.5f*l.s;for(int yy=0;yy<2;yy++)for(int xx=0;xx<Math.ceil(sbW/tile);xx++)ui.rect(sbX+xx*tile,alphaY+yy*tile,Math.min(tile,sbX+sbW-(sbX+xx*tile)),Math.min(tile,alphaY+alphaH-(alphaY+yy*tile)),((xx+yy)&1)==0?0xFF34363D:0xFF1E2026);
		int rgb=d.color.argb()&0xFFFFFF;ui.gradientRoundedRect(sbX,alphaY,sbW,alphaH,3*l.s,rgb,0xFF000000|rgb,true);
		float ax=sbX+sbW*d.color.alpha();ui.roundedRect(ax-1.2f*l.s,alphaY-1.5f*l.s,2.4f*l.s,alphaH+3*l.s,1.2f*l.s,WHITE)
			.text(sbX,d.y+97*l.s,5.6f*l.s,MUTED,"Opacity")
			.rightText(sbX+sbW,d.y+97*l.s,5.6f*l.s,TEXT,Math.round(d.color.alpha()*100)+"%",true);
	}
	private static float clamp01(float value){return Math.max(0,Math.min(1,value));}
    private static int hintExtra(String hint){
        return hint!=null&&hint.length()>29?10:0;
    }
    private static void settingHint(UiDrawList ui,String hint,float x,float y,float scale){
        if(hint==null||hint.isBlank())return;
        if(hint.length()<=29){ui.text(x,y,7f*scale,MUTED,hint);return;}
        int split=hint.lastIndexOf(' ',29);
        if(split<13)split=29;
        ui.text(x,y,7f*scale,MUTED,hint.substring(0,split));
        String rest=hint.substring(split).trim();
        if(rest.length()>30)rest=rest.substring(0,27)+"...";
        ui.text(x,y+10*scale,7f*scale,MUTED,rest);
    }	static float height(Module m,float s){
		float h=8*s;
		for(Setting setting:m.settings)if(setting.visible())h+=((setting instanceof Slider?36:setting instanceof Toggle?31:setting instanceof Choice||setting instanceof Group?34:setting instanceof ColorValue?32:30)+hintExtra(setting.hint()))*s;
		return h;
	}
	private static boolean hit(float mx,float my,float x,float y,float w,float h){
		return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;
	}

    private record Dropdown(Choice choice,float x,float y,float w,float headerH,float rowH,boolean up){
        float listY(){float unit=rowH/19f,h=choice.values.size()*rowH+4*unit;return up?y-h-2*unit:y+headerH+2*unit;}
        boolean contains(float mx,float my){
            float h=choice.values.size()*rowH+6*rowH/19f;
            return hit(mx,my,x,y,w,headerH)||hit(mx,my,x,listY(),w,h);
        }
    }
    private record GroupDropdown(Group group,float x,float y,float w,float headerH,float rowH,boolean up){
        float listY(){float unit=rowH/19f,h=group.values.size()*rowH+4*unit;return up?y-h-2*unit:y+headerH+2*unit;}
        boolean contains(float mx,float my){
            float h=group.values.size()*rowH+6*rowH/19f;
            return hit(mx,my,x,y,w,headerH)||hit(mx,my,x,listY(),w,h);
        }
    }	private record ColorPicker(ColorValue color,float x,float y,float w,float h){boolean contains(float mx,float my){return hit(mx,my,x,y,w,h);}}
	private enum ColorDrag{SATURATION_BRIGHTNESS,HUE,ALPHA}


}
