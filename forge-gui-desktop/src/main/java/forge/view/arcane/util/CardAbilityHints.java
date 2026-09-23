package forge.view.arcane.util;

import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordView;
import forge.localinstance.skin.FSkinProp;
import forge.util.Localizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Uses the current card's keyword reminder text rather than a second rules dictionary. GPL-3.0-or-later. */
public final class CardAbilityHints {
    private CardAbilityHints() {}

    public static String tooltip(final CardStateView state, final List<FSkinProp> icons, final boolean visible) {
        if (!visible || state == null || icons.isEmpty()) { return null; }
        final StringBuilder html = new StringBuilder("<html><body width='300'>");
        final Set<String> seen = new LinkedHashSet<>();
        for (final FSkinProp icon : icons) {
            boolean found = false;
            for (final KeywordView keyword : state.getKeywords()) {
                if (!matches(icon, keyword)) { continue; }
                found = true;
                final String title = keyword.title();
                final String reminder = keyword.reminderText();
                final String line = "<b>" + escape(title) + "</b>"
                        + (reminder == null || reminder.isBlank() ? "" : "<br>" + escape(reminder));
                if (seen.add(line)) { html.append(line).append("<br><br>"); }
            }
            if (!found) {
                final String name = icon.name().replace("IMG_ABILITY_", "").toLowerCase(Locale.ROOT).replace('_', ' ');
                final String line = "<b>" + escape(Character.toUpperCase(name.charAt(0)) + name.substring(1)) + "</b>";
                if (seen.add(line)) { html.append(line).append("<br><br>"); }
            }
        }
        return html.append(escape(Localizer.getInstance().getMessage("lblIERAAbilityDetails"))).append("</body></html>").toString();
    }

    private static boolean matches(final FSkinProp icon, final KeywordView keyword) {
        return FSkinProp.iconFromKeyword(keyword) == icon
                || (icon.name().startsWith("IMG_ABILITY_PROTECT_") && keyword.keyword() == Keyword.PROTECTION)
                || (icon.name().startsWith("IMG_ABILITY_HEXPROOF") && keyword.keyword() == Keyword.HEXPROOF);
    }

    private static String escape(final String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}
