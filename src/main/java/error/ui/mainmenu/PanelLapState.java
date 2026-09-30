package error.ui.mainmenu;

import lombok.Getter;
import lombok.Setter;
import error.ui.mainmenu.popup.Modal;
import error.module.Category;
import error.module.Module;
import error.util.math.Animation;
import error.util.render.font.Fonts;

/**
 * Create by daun kvass
 */
@Getter
@Setter
public final class PanelLapState {
    public enum Tab { CATEGORY, BINDS, ACCOUNTS }

    private final Animation openAnimation = new Animation(0.0F, 0.16F);
    private final Animation categoryAnim = new Animation(1.0F, 0.35F);
    private final Animation searchWidthAnim = new Animation(0.0F, 0.18F);
    private final Animation settingsPopupAnim = new Animation(0.0F, 0.16F);
    private final Animation descAnim = new Animation(0.0F, 0.16F);
    private final Animation accountAddAnim = new Animation(0.0F, 0.22F);
    private final Animation holdAnim = new Animation(0.0F, 0.18F);

    private boolean open;
    private boolean closing;
    private boolean holdActive;
    private boolean settingsPopupOpen;

    private Tab currentTab = Tab.CATEGORY;
    private Category currentCategory = Category.COMBAT;
    private Category targetCategory = Category.COMBAT;
    private int categoryDirection = 1;

    private boolean bindsShowBound = true;
    private boolean bindsShowUnbound = true;

    private boolean searchFocused;
    private String searchQuery = "";

    private boolean accountAddOpen;
    private String accountAddQuery = "";

    private Module hoveredModule;
    private String lastDescription = "";

    private Modal activeModal;

    private float panelX = 140;
    private float panelY = 90;
    private float panelWidth = 535;
    private float panelHeight = 320;
    private boolean positionInitialized;

    private float scrollOffset = 0.0F;
    private float targetScroll = 0.0F;
    private float maxScroll = 0.0F;

    public void update() {
        openAnimation.setTarget(open && !closing ? 1.0F : 0.0F);
        openAnimation.update();

        holdAnim.setTarget(holdActive ? 1.0F : 0.0F);
        holdAnim.update();

        if (closing && openAnimation.getValue() <= 0.01F) {
            open = false;
            closing = false;
            holdActive = false;
            holdAnim.setValue(0.0F);
            searchFocused = false;
            settingsPopupOpen = false;
            accountAddOpen = false;
            accountAddQuery = "";
            hoveredModule = null;
            activeModal = null;
            resetScroll();
        }

        categoryAnim.update();

        float targetW = 0.0F;
        float textWidth = Fonts.SF_MEDIUM.getWidth(searchQuery, 8.5F);
        if (searchFocused) {
            targetW = Math.max(105.0F, textWidth + 34.0F);
        } else if (!searchQuery.isEmpty()) {
            targetW = textWidth + 30.0F;
        }

        searchWidthAnim.setTarget(targetW);
        searchWidthAnim.update();

        accountAddAnim.setTarget(accountAddOpen ? 1.0F : 0.0F);
        accountAddAnim.update();

        settingsPopupAnim.setTarget(settingsPopupOpen ? 1.0F : 0.0F);
        settingsPopupAnim.update();

        boolean showDesc = hoveredModule != null && hoveredModule.hasDescription() && !holdActive;
        if (showDesc) {
            lastDescription = hoveredModule.getDescription();
        }
        descAnim.setTarget(showDesc ? 1.0F : 0.0F);
        descAnim.update();

        targetScroll = Math.max(0.0F, Math.min(targetScroll, maxScroll));
        scrollOffset += (targetScroll - scrollOffset) * 0.3F;
        if (Math.abs(targetScroll - scrollOffset) < 0.2F) {
            scrollOffset = targetScroll;
        }
    }

    public void scroll(float delta) {
        this.targetScroll -= delta;
        this.targetScroll = Math.max(0.0F, Math.min(this.targetScroll, maxScroll));
    }

    public void resetScroll() {
        this.scrollOffset = 0.0F;
        this.targetScroll = 0.0F;
    }

    public void switchCategory(Category cat) {
        this.currentTab = Tab.CATEGORY;
        if (this.currentCategory == cat) return;
        this.categoryDirection = cat.ordinal() >= currentCategory.ordinal() ? 1 : -1;
        this.currentCategory = cat;
        this.targetCategory = cat;
        this.categoryAnim.setValue(0.7F);
        this.categoryAnim.setTarget(1.0F);
        this.accountAddOpen = false;
        resetScroll();
    }

    public void openBindsTab() {
        this.currentTab = Tab.BINDS;
        this.categoryDirection = 1;
        this.categoryAnim.setValue(0.7F);
        this.categoryAnim.setTarget(1.0F);
        this.accountAddOpen = false;
        resetScroll();
    }

    public void openAccountsTab() {
        this.currentTab = Tab.ACCOUNTS;
        this.categoryDirection = 1;
        this.categoryAnim.setValue(0.7F);
        this.categoryAnim.setTarget(1.0F);
        resetScroll();
    }

    public void toggleSettingsPopup() {
        this.settingsPopupOpen = !this.settingsPopupOpen;
    }

    public void closeSettingsPopup() {
        this.settingsPopupOpen = false;
    }

    public void open() {
        this.open = true;
        this.closing = false;
        this.openAnimation.setTarget(1.0F);
    }

    public void beginClose() {
        if (!this.open || this.closing) return;
        this.closing = true;
        this.openAnimation.setTarget(0.0F);
        this.settingsPopupOpen = false;
        this.accountAddOpen = false;
        this.hoveredModule = null;
    }

    public boolean isInteractive() {
        return this.open && !this.closing;
    }
}