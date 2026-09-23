package forge.view.arcane.util;

import forge.localinstance.skin.FSkinProp;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** A bounded icon rail that leaves combat markers and creature stats unobstructed. GPL-3.0-or-later. */
public final class AbilityIconLayout {
    private AbilityIconLayout() {}
    public record Slot(Rectangle bounds, List<FSkinProp> abilities) {
        public boolean isOverflow() { return abilities.size() > 1; }
    }

    public static List<Slot> arrange(final List<FSkinProp> icons, final int x, final int y,
                                    final int width, final int height, final int statsTop) {
        if (icons.isEmpty() || width < 24 || height < 45) { return List.of(); }
        final int size = Math.max(12, Math.min(22, width / 7));
        final int top = y + (width < 200 ? 25 : 50);
        final int bottom = Math.min(y + height - Math.max(34, height / 5), statsTop - 3);
        final int count = Math.min(4, Math.max(0, (bottom - top + 2) / (size + 2)));
        if (count == 0) { return List.of(); }
        final List<Slot> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, icons.size()); i++) {
            final boolean overflow = i == count - 1 && icons.size() > count;
            result.add(new Slot(new Rectangle(x + width - size - 4, top + i * (size + 2), size, size),
                    List.copyOf(icons.subList(i, overflow ? icons.size() : i + 1))));
        }
        return result;
    }
}
