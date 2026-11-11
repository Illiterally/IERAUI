package forge.gui;

import forge.toolbox.FButton;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.util.Localizer;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.FutureTask;

/** The custom X entry shown after Other. Filling the maximum never confirms payment. */
public final class ManaXInput {
    private ManaXInput() { }

    public static Integer show(final String title, final int min, final int max, final Integer maximum) {
        final FutureTask<Integer> task = new FutureTask<>(() -> {
            final Localizer l = Localizer.getInstance();
            final FTextField input = new FTextField.Builder().build();
            final JPanel body = new JPanel(new BorderLayout(8, 0));
            body.setOpaque(false);
            body.setPreferredSize(new Dimension(400, 30));
            body.add(input, BorderLayout.CENTER);
            final FButton useMax = new FButton(l.getMessage("lblUseMaxX"));
            useMax.setEnabled(maximum != null && maximum >= min && maximum <= max);
            body.add(useMax, BorderLayout.EAST);
            final String range = max == Integer.MAX_VALUE
                    ? l.getMessage("lblEnterNumberGreaterThanOrEqualsToMin", min)
                    : l.getMessage("lblEnterNumberBetweenMinAndMax", min, max);
            final String total = maximum == null ? l.getMessage("lblMaxXUnavailable")
                    : l.getMessage("lblMaxXAvailable", maximum);
            final String message = range + "\n" + total + "\n" + l.getMessage("lblMaxXPaymentHelp");
            final FOptionPane pane = new FOptionPane(message, title, null, body,
                    List.of(l.getMessage("lblOK"), l.getMessage("lblCancel")), -1);
            pane.setDefaultFocus(input);
            pane.setButtonEnabled(0, false);
            input.getDocument().addDocumentListener(new DocumentListener() {
                private void update() { pane.setButtonEnabled(0, parse(input.getText(), min, max) != null); }
                @Override public void insertUpdate(DocumentEvent e) { update(); }
                @Override public void removeUpdate(DocumentEvent e) { update(); }
                @Override public void changedUpdate(DocumentEvent e) { update(); }
            });
            useMax.addActionListener(e -> {
                input.setText(String.valueOf(maximum));
                input.requestFocusInWindow();
                input.selectAll();
            });
            input.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER && parse(input.getText(), min, max) != null) {
                        pane.setResult(0);
                    }
                }
            });
            pane.setVisible(true);
            final Integer result = pane.getResult() == 0 ? parse(input.getText(), min, max) : null;
            pane.dispose();
            return result;
        });
        FThreads.invokeInEdtAndWait(task);
        try { return task.get(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    static Integer parse(final String text, final int min, final int max) {
        try {
            final int value = Integer.parseInt(text.trim());
            return value >= min && value <= max ? value : null;
        } catch (NumberFormatException e) { return null; }
    }
}
