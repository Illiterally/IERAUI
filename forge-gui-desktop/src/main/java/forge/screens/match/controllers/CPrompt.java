/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.controllers;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.gamemodes.match.input.InputPayMana;
import forge.gamemodes.match.input.InputPassPriority;
import forge.player.PlayerControllerHuman;
import forge.gui.FThreads;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FSkin;

/**
 * Controls the prompt panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CPrompt implements ICDoc {
    private final CMatchUI matchUI;
    private final VPrompt view;
    private final PhaseInputGuard phaseGuard = new PhaseInputGuard();
    private final CCardActionPrompt cardPrompt;
    private boolean previousStackNonEmpty;
    private InputPayMana manaPayment;
    public CPrompt(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VPrompt(this);
        this.cardPrompt = forge.gui.EnhancedRulesUi.isEnabled() ? new CCardActionPrompt(this) : null;
        if (forge.gui.EnhancedRulesUi.isEnabled()) {
            final KeyAdapter keys = new KeyAdapter() {
                @Override public void keyPressed(final KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        phaseGuard.keyPressed(e.getKeyCode());
                    }
                }
                @Override public void keyReleased(final KeyEvent e) { phaseGuard.keyReleased(e.getKeyCode()); }
            };
            view.getBtnOK().addKeyListener(keys);
            view.getBtnCancel().addKeyListener(keys);
            view.getBtnManaAuto().addKeyListener(keys);
        }
    }

    public final CMatchUI getMatchUI() {
        return matchUI;
    }
    public final VPrompt getView() {
        return view;
    }

    private Component lastFocusedButton = null;

    private final ActionListener actCancel = evt -> submitAction(false, evt.getWhen());
    private final ActionListener actOK = evt -> submitAction(true, evt.getWhen());
    private final ActionListener actManaAuto = evt -> submitManaAuto(evt.getWhen());

    private InputPayMana currentManaPayment() {
        if (matchUI.getGameController() instanceof PlayerControllerHuman human
                && !human.macros().isReplaying()
                && human.getInputQueue().getInput() instanceof InputPayMana payment
                && payment.usesManualPaymentUi()) { return payment; }
        return null;
    }

    private void updateManaPayment() {
        final InputPayMana next = currentManaPayment();
        if (manaPayment != next) {
            view.getBtnManaAuto().getModel().setArmed(false);
            view.getBtnManaAuto().getModel().setPressed(false);
        }
        manaPayment = next;
        view.getBtnManaAuto().setVisible(manaPayment != null);
        view.getBtnManaAuto().setEnabled(manaPayment != null && manaPayment.canAutoPayMana());
        if (view.getBtnManaAuto().getParent() != null) {
            view.getBtnManaAuto().getParent().revalidate();
            view.getBtnManaAuto().getParent().repaint();
        }
    }

    boolean submitManaAuto(final long eventTime) {
        final InputPayMana current = currentManaPayment();
        if (matchUI.getGameView() == null || matchUI.getGameView().isGameOver()
                || !view.getBtnManaAuto().isEnabled() || current == null || current != manaPayment
                || !current.canAutoPayMana() || !acceptPhaseInput(eventTime)) { return false; }
        view.getBtnManaAuto().setEnabled(false);
        if (cardPrompt != null) { cardPrompt.blockButtons(); }
        current.autoPayMana();
        return true;
    }

    /** Both prompt surfaces use the same enabled state, phase guard and controller action. */
    boolean submitAction(final boolean primary, final long eventTime) {
        final JButton button = primary ? view.getBtnOK() : view.getBtnCancel();
        if (!button.isEnabled()) { return false; }
        if ((primary || forge.util.Localizer.getInstance().getMessage("lblEndTurn").equals(button.getText()))
                && !acceptPhaseInput(eventTime)) { return false; }
        if (cardPrompt != null) { cardPrompt.blockButtons(); }
        if (primary) { selectButtonOk(); } else { selectButtonCancel(); }
        return true;
    }

    public void disposeCardPrompt() {
        if (cardPrompt != null) { cardPrompt.dispose(); }
    }

    private boolean acceptPhaseInput(final long eventTime) {
        if (!forge.gui.EnhancedRulesUi.isEnabled()) { return true; }
        return phaseGuard.accepts(eventTime, System.currentTimeMillis());
    }

    private final WindowAdapter focusOKButtonOnDialogClose = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent evt) {
            view.getBtnOK().requestFocusInWindow();
        }
    };

    private final PropertyChangeListener focusOnEnable = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (lastFocusedButton == null || lastFocusedButton == view.getBtnOK()) {
                // Attempt to resolve sporadic button focus issues when dialogs are shown.
                Dialog activeDialog = getActiveDialog(true);
                if (activeDialog != null) {
                    // If this dialog already has our listener, remove it
                    activeDialog.removeWindowListener(focusOKButtonOnDialogClose);
                    activeDialog.addWindowListener(focusOKButtonOnDialogClose);
                }

                // Focus the OK button when it becomes enabled
                boolean isEnabled = (Boolean) evt.getNewValue();
                if (isEnabled) {
                    view.getBtnOK().requestFocusInWindow();
                }
            }
        }
    };

    private final FocusListener onFocus = new FocusAdapter() {
        @Override
        public void focusLost(final FocusEvent e) {
            phaseGuard.clearKeys();
        }

        @Override
        public void focusGained(final FocusEvent e) {
            if (null != view.getParentCell() && view == view.getParentCell().getSelected()) {
                // only record focus changes when we're showing -- otherwise it is due to a tab visibility change
                lastFocusedButton = e.getComponent();
            }
        }
    };

    private void _initButton(final JButton button, final ActionListener onClick) {
        // remove to ensure listeners don't accumulate over many initializations
        button.removeActionListener(onClick);
        button.addActionListener(onClick);
        button.removeFocusListener(onFocus);
        button.addFocusListener(onFocus);
        if (button == view.getBtnOK()) {
            button.removePropertyChangeListener("enabled", focusOnEnable);
            button.addPropertyChangeListener("enabled", focusOnEnable);
        }
    }

    @Override
    public void initialize() {
        _initButton(view.getBtnCancel(), actCancel);
        _initButton(view.getBtnManaAuto(), actManaAuto);
        _initButton(view.getBtnOK(), actOK);
    }

    private static Dialog getActiveDialog(boolean modalOnly)
    {
        Window[] windows = Window.getWindows();
        if (windows != null) {
            for (Window w : windows) {
                if (w.isShowing() && w instanceof Dialog && (!modalOnly || ((Dialog)w).isModal())) {
                    return (Dialog)w;
                }
            }
        }
        return null;
    }

    private void selectButtonOk() {
        matchUI.getGameController().selectButtonOk();
    }

    private void selectButtonCancel() {
        matchUI.getGameController().selectButtonCancel();
    }

    public void setMessage(final String s0, final CardView card) {
        view.getTarMessage().setText(FSkin.encodeSymbols(s0, false));
        if (forge.gui.EnhancedRulesUi.isEnabled()) {
            String help = null;
            if (matchUI.getGameController() instanceof PlayerControllerHuman human
                    && human.getInputQueue().getInput() instanceof InputPassPriority priority) {
                help = priority.getPriorityHelpText();
            }
            final String tooltip = help == null ? null : "<html><body width='300'>"
                    + help.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
                    + "</body></html>";
            view.getTarMessage().setToolTipText(tooltip);
            // Keep the action button clear while the player is resolving or advancing.
            // Detailed timing help remains available over the prompt text.
            view.getBtnOK().setToolTipText(null);
        }
        view.setCardView(card);
        updatePhaseContext();
        if (cardPrompt != null) { cardPrompt.setContext(s0, card); }
    }

    public void updatePhaseContext() {
        if (!forge.gui.EnhancedRulesUi.isEnabled()) { return; }
        final GameView game = matchUI.getGameView();
        final forge.util.Localizer localizer = forge.util.Localizer.getInstance();
        if (game == null || game.isGameOver() || game.getPhase() == null || game.getPlayerTurn() == null) {
            view.getLblPhase().setText(localizer.getMessage("lblGameSetup"));
            view.getLblDecision().setText("");
            phaseGuard.enter("setup", false, System.currentTimeMillis());
            phaseGuard.clearKeys();
            previousStackNonEmpty = false;
            return;
        }
        view.getLblPhase().setText(game.getPhase().nameForUi);
        view.getLblPhase().setToolTipText(game.getPhase().nameForUi);
        view.getLblPhase().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        view.getLblPhase().setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
        view.getLblPhase().setOpaque(true);
        view.getLblDecision().setText(localizer.getMessage("lblIERATurnOwner", game.getPlayerTurn().getName(), game.getTurn()));
        phaseGuard.enter(game.getId() + ":" + game.getTurn() + ":" + game.getPlayerTurn().getId() + ":" + game.getPhase(),
                game.getPhase().isMain() && matchUI.isLocalPlayer(game.getPlayerTurn()), System.currentTimeMillis());
        final boolean stackNonEmpty = game.getStack() != null && !game.getStack().isEmpty();
        if (previousStackNonEmpty && !stackNonEmpty && game.getPhase().isMain()
                && matchUI.isLocalPlayer(game.getPlayerTurn())) {
            phaseGuard.protect(System.currentTimeMillis());
        }
        previousStackNonEmpty = stackNonEmpty;
    }

    public void updateDecisionOwner(final forge.game.player.PlayerView owner, final boolean enabled) {
        if (!forge.gui.EnhancedRulesUi.isEnabled()) { return; }
        updateManaPayment();
        if (cardPrompt != null) { cardPrompt.refresh(); }
        updatePhaseContext();
        final GameView game = matchUI.getGameView();
        if (game == null || game.isGameOver() || game.getPlayerTurn() == null) { return; }
        final forge.util.Localizer localizer = forge.util.Localizer.getInstance();
        final String decision = enabled && owner != null
                ? localizer.getMessage("lblIERADecisionOwner", owner.getName()) : localizer.getMessage("lblIERAWaiting");
        view.getLblDecision().setText(localizer.getMessage("lblIERATurnAndDecision", game.getPlayerTurn().getName(), decision));
        view.getLblDecision().setToolTipText(view.getLblDecision().getText());
    }

    /**
     * Invoke a flashing animation on the prompt.
     */
    public void remind() {
        SDisplayUtil.remind(view);
    }

    public void alert() {
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_REMIND_ON_PRIORITY)) {
            SDisplayUtil.remind(view, 15, 30);
        }
    }

    @Override
    public void register() {
    }

    @Override
    public void update() {
        // set focus back to button that last had it
        if (null != lastFocusedButton) {
            lastFocusedButton.requestFocusInWindow();
        }
    }

    public void updateText() {
        FThreads.assertExecutedByEdt(true);
        final GameView game = matchUI.getGameView();
        if (game == null) {
            return;
        }
        final String text = String.format("T:%d G:%d/%d [%s]", game.getTurn(), game.getNumPlayedGamesInMatch() + 1, game.getNumGamesInMatch(), game.getGameType());
        view.getLblGames().setText(text);
        view.getLblGames().setToolTipText(String.format("%s: Game #%d of %d, turn %d", game.getGameType(), game.getNumPlayedGamesInMatch() + 1, game.getNumGamesInMatch(), game.getTurn()));
    }
}
