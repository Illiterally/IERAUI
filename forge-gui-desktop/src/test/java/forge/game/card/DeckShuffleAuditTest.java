package forge.game.card;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.testng.annotations.Test;
import java.util.HashSet;
import java.util.Random;
import static org.testng.Assert.*;

public class DeckShuffleAuditTest extends AITest {
    @Test public void ordinaryShufflePreservesAllCopiesAndCanProduceTheObservedDrought() {
        Game game = initAndCreateGame();
        Player player = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Mondrak, Glory Dominus", player, ZoneType.Library);
            addCardToZone("Anointed Procession", player, ZoneType.Library);
        }
        for (int i = 0; i < 54; i++) { addCardToZone("Plains", player, ZoneType.Library); }
        var original = new HashSet<>(player.getCardsIn(ZoneType.Library));
        Random previous = MyRandom.getRandom();
        int droughts = 0;
        try {
            MyRandom.setRandom(new Random(20261103));
            for (int sample = 0; sample < 1500; sample++) {
                player.shuffle(null);
                var library = player.getCardsIn(ZoneType.Library);
                assertEquals(library.size(), 60);
                assertEquals(new HashSet<>(library), original);
                boolean found = false;
                for (int i = 0; i < 27; i++) {
                    if (!library.get(i).getName().equals("Plains")) { found = true; break; }
                }
                if (!found) { droughts++; }
            }
        } finally { MyRandom.setRandom(previous); }
        // Fixed seed makes this reproducible; broad bounds flag missing/bunched cards.
        assertTrue(droughts > 10 && droughts < 60, "Droughts in 1500 shuffles: " + droughts);
        System.out.println("SHUFFLE_AUDIT_DROUGHTS=" + droughts + "/1500");
    }
}
