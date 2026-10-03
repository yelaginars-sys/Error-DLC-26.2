package dev.liteproject.client.ui;

import dev.liteproject.client.ClientCore;
import dev.liteproject.client.render.ui.UiDrawList;
import dev.liteproject.client.ui.CsClickGuiModel.Category;
import java.util.List;

/** Animated, draggable segmented category selector for CsClickGui. */
final class CsRubberSegment {
    private static final int TRACK=0xFF1A1A1A, TEXT=0xFFD9D5C9, ACTIVE=0xFF291923;
    private float left,right,targetLeft,targetRight;
    private boolean ready,held,live,onThumb;
    private int pressedSlot;
    private float pressX,dragOffset,dragWidth,lastX,velocity;
    private long lastSample;

    static float width(int count,float scale){return (count*80+5)*scale;}

    int draw(UiDrawList ui,List<Category> categories,int selected,float x,float cy,float scale,int accent,
             int mx,int my,boolean down,boolean pressed,float dt) {
        int count=categories.size();
        if(count==0)return selected;
        float slot=80*scale, inset=2.5f*scale, trackX=x-inset,trackY=cy-15*scale;
        float trackW=count*slot+2*inset,trackH=30*scale;
        float min=x,max=x+count*slot;
        if(!ready){left=x+selected*slot;right=left+slot;targetLeft=left;targetRight=right;ready=true;}
        if(pressed&&hit(mx,my,trackX,trackY,trackW,trackH)){
            held=true;live=false;pressedSlot=clamp((int)((mx-x)/slot),0,count-1);
            pressX=mx;onThumb=mx>=left&&mx<=right;
            dragOffset=mx-left;dragWidth=right-left;lastX=mx;velocity=0;lastSample=System.nanoTime();
        }
        if(held&&down&&onThumb){
            long now=System.nanoTime();
            float elapsed=(now-lastSample)/1_000_000_000f;
            if(elapsed>.008f){velocity=(mx-lastX)/elapsed;lastX=mx;lastSample=now;}
            if(Math.abs(mx-pressX)>4*scale)live=true;
            if(live){
                float raw=mx-dragOffset;
                float maxLeft=max-dragWidth;
                if(raw<min){left=min;right=min+dragWidth-rubber(min-raw,dragWidth);}
                else if(raw>maxLeft){right=max;left=maxLeft+rubber(raw-maxLeft,dragWidth);}
                else{left=raw;right=raw+dragWidth;}
            }
        }
        if(held&&!down){
            held=false;
            int next=selected;
            if(live){
                float projected=(left+right)*.5f+clamp(velocity*.075f,-slot*1.5f,slot*1.5f);
                next=clamp(Math.round((projected-x-slot*.5f)/slot),0,count-1);
            }else if(Math.abs(mx-pressX)<=10*scale)next=pressedSlot;
            targetLeft=x+next*slot;targetRight=targetLeft+slot;
            selected=next;live=false;
        }
        if(!held){
            targetLeft=x+selected*slot;targetRight=targetLeft+slot;
            boolean rightward=targetLeft>left;
            float leftRate=rightward?10:19,rightRate=rightward?19:10;
            left+= (targetLeft-left)*(1-(float)Math.exp(-leftRate*dt));
            right+=(targetRight-right)*(1-(float)Math.exp(-rightRate*dt));
            if(Math.abs(left-targetLeft)<.06f){left=targetLeft;}
            if(Math.abs(right-targetRight)<.06f){right=targetRight;}
        }
        ui.roundedRect(trackX,trackY,trackW,trackH,10*scale,TRACK);
        for(int i=0;i<count;i++){
            Category category=categories.get(i);
            float sx=x+i*slot;
            int ink=hit(mx,my,sx,trackY,slot,trackH)?0xFFF7F4EC:TEXT;
            float textWidth=ClientCore.getInstance().renderer().textWidth(category.name(),8*scale,false);
            float start=sx+(slot-textWidth-15*scale)*.5f;
            ui.icon(start+4.5f*scale,cy,9*scale,ink,category.icon())
              .text(start+15*scale,cy,8*scale,ink,category.name());
        }
        float thumbY=trackY+inset,thumbH=trackH-2*inset;
        ui.roundedRect(left,thumbY,Math.max(scale,right-left),thumbH,6*scale,accent);
        Category activeCategory=categories.get(selected);
        float activeWidth=ClientCore.getInstance().renderer().textWidth(activeCategory.name(),8*scale,false);
        float activeStart=(left+right-activeWidth-15*scale)*.5f;
        ui.icon(activeStart+4.5f*scale,cy,9*scale,ACTIVE,activeCategory.icon())
          .text(activeStart+15*scale,cy,8*scale,ACTIVE,activeCategory.name());
        return selected;
    }

    private static float rubber(float over,float dim){return over*dim*.55f/(dim+.55f*Math.abs(over));}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private static boolean hit(float mx,float my,float x,float y,float w,float h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
}
