package forge.gui;

/** Opt-in presentation features for Illiterally's Enhanced Rules Analysis UI. */
public final class EnhancedRulesUi {
    private EnhancedRulesUi() { }

    public static boolean isEnabled() {
        return Boolean.getBoolean("forge.enhancedRulesUi");
    }
}
