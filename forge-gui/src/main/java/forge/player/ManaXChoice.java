package forge.player;

import forge.gui.interfaces.IGuiGame;
import forge.util.Localizer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Keeps the ordinary small-number choices; only Other calculates an automatic total. */
public final class ManaXChoice {
    private ManaXChoice() { }

    public static Integer choose(final IGuiGame gui, final String title, final int min, final int max,
                                 final Supplier<Integer> maximum) {
        final int cutoff = (int) Math.min(Integer.MAX_VALUE, (long) min + 9);
        if (max <= cutoff) { return gui.getInteger(title, min, max, false); }
        final List<Serializable> choices = new ArrayList<>();
        for (int i = min; i <= cutoff; i++) { choices.add(i); }
        choices.add(Localizer.getInstance().getMessage("lblOtherInteger"));
        final Object choice = gui.oneOrNone(title, choices);
        if (choice == null || choice instanceof Integer) { return (Integer) choice; }
        return gui.getManaXInput(title, min, max, maximum.get());
    }
}
