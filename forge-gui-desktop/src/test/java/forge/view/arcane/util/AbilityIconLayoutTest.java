package forge.view.arcane.util;

import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordCollectionView;
import forge.game.keyword.KeywordView;
import forge.localinstance.skin.FSkinProp;
import forge.util.Localizer;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;
import java.util.stream.IntStream;
import static org.mockito.Mockito.*;

public class AbilityIconLayoutTest {
    @Test public void crowdedCardsRetainEveryAbilityWithoutCoveringStats() {
        final List<FSkinProp> icons = IntStream.range(0, 9).mapToObj(i -> FSkinProp.IMG_ABILITY_FLYING).toList();
        for (final int width : new int[]{50, 80, 100, 200, 300}) {
            final int height = width * 7 / 5;
            final int statsTop = height - 24;
            final var slots = AbilityIconLayout.arrange(icons, 0, 0, width, height, statsTop);
            Assert.assertTrue(slots.size() <= 4);
            if (slots.isEmpty()) { continue; }
            Assert.assertEquals(slots.stream().mapToInt(s -> s.abilities().size()).sum(), icons.size());
            int previousBottom = 0;
            for (final var slot : slots) {
                Assert.assertTrue(slot.bounds().x >= 0 && slot.bounds().x + slot.bounds().width <= width);
                Assert.assertTrue(slot.bounds().y >= previousBottom);
                previousBottom = slot.bounds().y + slot.bounds().height;
                Assert.assertTrue(previousBottom < statsTop);
            }
            Assert.assertTrue(slots.get(slots.size() - 1).isOverflow());
        }
    }

    @Test public void hintsUseCurrentReminderTextEscapeMarkupAndRespectVisibility() {
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
        final CardStateView state = mock(CardStateView.class);
        final KeywordView keyword = mock(KeywordView.class);
        when(keyword.keyword()).thenReturn(Keyword.SHROUD);
        when(keyword.original()).thenReturn("Shroud");
        when(keyword.title()).thenReturn("Shroud");
        when(keyword.reminderText()).thenReturn("Example <rules> & text");
        final KeywordCollectionView keywords = new KeywordCollectionView(List.of(keyword));
        when(state.getKeywords()).thenReturn(keywords);
        final var icons = List.of(FSkinProp.IMG_ABILITY_SHROUD);
        Assert.assertNull(CardAbilityHints.tooltip(state, icons, false));
        verifyNoInteractions(state);
        final String tooltip = CardAbilityHints.tooltip(state, icons, true);
        Assert.assertTrue(tooltip.contains("Shroud"));
        Assert.assertTrue(tooltip.contains("Example &lt;rules&gt; &amp; text"));
    }
}
