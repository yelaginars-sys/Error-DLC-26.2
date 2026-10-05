package error.util.render.font;

public enum IconUse {
    LOGO("a"),
    FIGHT("b"),
    MOVEMENT("c"),
    RENDER("d"),
    PLAYER("e"),
    MISC("f"),
    SCRIPT("g"),
    SEARCH("h"),
    CHECK("i"),
    DOWN("j"),
    UP("k"),
    CUBE("l"),
    GLOBE("m"),
    PERSONS("n"),
    GEAR("o"),
    EXIT("p"),
    ADD("q"),
    REFRESH("r"),
    MICROSOFT("s"),
    STAR("t"),
    CROSS("u"),
    HOME("v"),
    KEYBOARD("C"),
    COMPASS("X"),
    BACK("Z"),
    INFO("A"),
    WARN("B"),
    POTION("E"),
    CLOCK("D"),
    SPUTNIK("E"),
    GROUP("F"),
    LINK("Y"),
    KEYBIND("G"),
    ADDFRIEND("H"),
    REMOVEFRIEND("I"),
    STAFF("F"),
    ONMODULE("L"),
    OFFMODULE("K"),
    WORLD("M"),
    BPS("N"),
    TPS("O"),
    FPS("Q"),
    CORD("R"),
    PING("S");
    public final String glyph;
    IconUse(String glyph) {this.glyph = glyph;}
    public String getGlyph() {return this.glyph;}
    @Override
    public String toString() {return this.glyph;}
}