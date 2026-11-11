package forge.gamemodes.match.input;

import java.util.List;
import java.util.Map;
import java.util.Set;
import forge.ai.ComputerUtilMana;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.zone.ZoneType;

import forge.game.Game;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardView;
import forge.game.ability.ApiType;
import forge.game.cost.Cost;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.Match;
import forge.game.zone.PlayerZone;
import forge.game.mana.ManaPool;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.actions.PassPriorityAction;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.StackItemView;
import forge.game.spellability.TargetChoices;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.MagicStack;
import forge.gamemodes.match.DeclineScope;
import forge.gamemodes.match.YieldController;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IMacroSystem;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.Localizer;
import forge.util.ITriggerEvent;
import forge.util.collect.FCollection;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PromptInteractionTest {
    private String previousFlag;
    private IGuiBase previousGui;
    private MockedStatic<FModel> model;
    private IGuiGame gui;
    private PlayerControllerHuman controller;
    private Player player;
    private PlayerView owner;
    private Game game;
    private PhaseHandler phases;
    private MagicStack stack;
    private CardView source;

    @BeforeMethod
    public void prepare() {
        previousFlag = System.getProperty("forge.enhancedRulesUi");
        System.setProperty("forge.enhancedRulesUi", "true");
        previousGui = GuiBase.getInterface();
        final IGuiBase platform = mock(IGuiBase.class);
        when(platform.getAssetsDir()).thenReturn("../forge-gui/");
        GuiBase.setInterface(platform);
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
        if (forge.util.Lang.getInstance() == null) forge.util.Lang.createInstance("en-US");
        model = mockStatic(FModel.class, withSettings().mockMaker("mock-maker-inline"));
        final ForgePreferences preferences = mock(ForgePreferences.class);
        model.when(FModel::getPreferences).thenReturn(preferences);

        gui = mock(IGuiGame.class);
        controller = mock(PlayerControllerHuman.class);
        player = mock(Player.class);
        owner = mock(PlayerView.class);
        game = mock(Game.class);
        phases = mock(PhaseHandler.class);
        stack = mock(MagicStack.class);
        when(controller.getPlayer()).thenReturn(player);
        when(player.getView()).thenReturn(owner);
        when(player.getGame()).thenReturn(game);
        final ManaPool pool = mock(ManaPool.class);
        when(pool.isEmpty()).thenReturn(true);
        when(player.getManaPool()).thenReturn(pool);
        when(player.getCreaturesInPlay()).thenReturn(new CardCollection());
        when(controller.getGui()).thenReturn(gui);
        when(controller.getGame()).thenReturn(game);
        when(controller.getInputQueue()).thenReturn(mock(InputQueue.class));
        when(controller.macros()).thenReturn(mock(IMacroSystem.class));
        final YieldController yield = mock(YieldController.class);
        when(controller.getYieldController()).thenReturn(yield);
        when(yield.getDeclineScope(any())).thenReturn(DeclineScope.NEVER);
        when(game.getView()).thenReturn(mock(GameView.class));
        when(game.isNeitherDayNorNight()).thenReturn(true);
        when(game.getPhaseHandler()).thenReturn(phases);
        when(game.getStack()).thenReturn(stack);
        when(phases.getPlayerTurn()).thenReturn(player);
        when(phases.getPriorityPlayer()).thenReturn(player);
        when(phases.getPhase()).thenReturn(PhaseType.MAIN1);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.size()).thenReturn(1);
        final SpellAbilityStackInstance top = mock(SpellAbilityStackInstance.class);
        final StackItemView item = mock(StackItemView.class);
        source = mock(CardView.class);
        when(source.getName()).thenReturn("Savageborn Hydra");
        when(source.canBeShownTo(owner)).thenReturn(true);
        when(item.getSourceCard()).thenReturn(source);
        when(top.getView()).thenReturn(item);
        when(stack.peek()).thenReturn(top);
    }

    @AfterMethod
    public void restore() {
        if (model != null) model.close();
        GuiBase.setInterface(previousGui);
        if (previousFlag == null) System.clearProperty("forge.enhancedRulesUi");
        else System.setProperty("forge.enhancedRulesUi", previousFlag);
    }

    @Test
    public void resolveNamesPendingSpellButStillOnlyPassesPriority() {
        final InputPassPriority input = new InputPassPriority(controller);
        input.showMessage();
        verify(gui).updateButtons(owner, "Resolve", "End Turn", true, true, true);
        verify(gui).showPromptMessage(eq(owner), contains("Next to resolve: Savageborn Hydra"), eq(source));
        Assert.assertTrue(input.getPriorityHelpText().contains("Other players may respond"));
        verify(gui, never()).showPromptMessage(eq(owner), contains("Priority:"), eq(source));
        input.selectButtonOK();
        verify(controller.macros()).addRememberedAction(any(PassPriorityAction.class));
        verify(gui, atLeastOnce()).setSelection(argThat(selection -> selection.actionable().isEmpty()));
        verify(controller, never()).autoPassUntilEndOfTurn();
        verify(stack, never()).resolveStack();
    }

    @Test
    public void severalItemsResolveOneAtATimeAndKeepUndoAvailable() {
        when(stack.size()).thenReturn(3);
        when(phases.getPhase()).thenReturn(PhaseType.MAIN2);
        when(controller.canUndoLastAction()).thenReturn(true);
        final forge.game.undo.UndoManager undo = mock(forge.game.undo.UndoManager.class);
        when(game.getUndoManager()).thenReturn(undo);
        when(undo.undosLeft()).thenReturn(1);
        new InputPassPriority(controller).showMessage();
        verify(gui).updateButtons(owner, "Resolve next", "Undo (1)", true, true, true, true);
        verify(stack, never()).resolveStack();
    }

    @Test
    public void pendingSourceDoesNotExposeFaceDownOrHiddenIdentity() {
        when(source.isFaceDown()).thenReturn(true);
        Assert.assertFalse(PriorityPrompt.forGame(game, owner).explanation().contains("Savageborn"));
        Assert.assertFalse(PriorityPrompt.forGame(game, owner).help().contains("Savageborn"));
        Assert.assertNull(PriorityPrompt.forGame(game, owner).sourceCard());
        when(source.isFaceDown()).thenReturn(false);
        when(source.canBeShownTo(owner)).thenReturn(false);
        Assert.assertFalse(PriorityPrompt.forGame(game, owner).explanation().contains("Savageborn"));
        Assert.assertNull(PriorityPrompt.forGame(game, owner).sourceCard());
    }

    @DataProvider(name = "normalPhases")
    public Object[][] normalPhases() {
        return new Object[][] {
            {PhaseType.UPKEEP, "Draw"},
            {PhaseType.DRAW, "Main phase 1"},
            {PhaseType.MAIN1, "Combat"},
            {PhaseType.COMBAT_BEGIN, "Attackers"},
            {PhaseType.COMBAT_DECLARE_ATTACKERS, "Blockers"},
            {PhaseType.COMBAT_FIRST_STRIKE_DAMAGE, "Combat damage"},
            {PhaseType.COMBAT_DAMAGE, "End combat"},
            {PhaseType.COMBAT_END, "Main phase 2"},
            {PhaseType.MAIN2, "End step"},
            {PhaseType.END_OF_TURN, "Cleanup"}
        };
    }

    @Test(dataProvider = "normalPhases")
    public void emptyStackLabelsTheScheduledDestination(final PhaseType current, final String caption) {
        when(stack.isEmpty()).thenReturn(true);
        when(phases.getPhase()).thenReturn(current);
        final InputPassPriority input = new InputPassPriority(controller);
        input.showMessage();
        verify(gui).updateButtons(owner, caption, "End Turn", true, true, true);
        Assert.assertTrue(input.getPriorityHelpText().contains("Effects can change or skip steps"));
        verify(gui, never()).showPromptMessage(eq(owner), contains("Phase:"), isNull());
    }

    @Test
    public void blockersAdvanceTowardDamageWithoutPromisingAFirstStrikeStep() {
        when(stack.isEmpty()).thenReturn(true);
        when(phases.getPhase()).thenReturn(PhaseType.COMBAT_DECLARE_BLOCKERS);
        final PriorityPrompt prompt = PriorityPrompt.forGame(game, owner);
        Assert.assertEquals(prompt.label(), "Combat damage");
        Assert.assertTrue(prompt.help().contains("first-strike step if applicable"));
        Assert.assertTrue(prompt.explanation().contains("Blocks confirmed"));
    }

    @Test
    public void extraCombatAndReversedPhasesUseEngineSchedule() {
        when(stack.isEmpty()).thenReturn(true);
        when(phases.getPhase()).thenReturn(PhaseType.MAIN2);
        when(phases.hasExtraPhaseAfter(PhaseType.MAIN2, PhaseType.COMBAT_BEGIN)).thenReturn(true);
        Assert.assertEquals(PriorityPrompt.forGame(game, owner).label(), "Combat");
        when(phases.hasExtraPhaseAfter(PhaseType.MAIN2, PhaseType.COMBAT_BEGIN)).thenReturn(false);
        when(player.isPhasesReversed()).thenReturn(true);
        Assert.assertEquals(PriorityPrompt.forGame(game, owner).label(), "Combat");
        when(phases.getPhase()).thenReturn(PhaseType.CLEANUP);
        Assert.assertEquals(PriorityPrompt.forGame(game, owner).label(), "Next turn");
    }

    @Test
    public void originalPriorityAndCustomConfirmChoicesRemainIntact() {
        System.clearProperty("forge.enhancedRulesUi");
        new InputPassPriority(controller).showMessage();
        verify(gui).updateButtons(owner, "OK", "End Turn", true, true, true);
        System.setProperty("forge.enhancedRulesUi", "true");
        final InputConfirm confirm = new InputConfirm(controller, "Where?", "Top of library", "Bottom of library", true);
        confirm.showMessage();
        verify(gui).updateButtons(owner, "Top of library", "Bottom of library", true, true, true);
        confirm.selectButtonCancel();
        Assert.assertFalse(confirm.getResult());
    }

    @Test public void compactPriorityRetainsDayStormAndMacroState() {
        when(stack.isEmpty()).thenReturn(true);
        when(game.isNeitherDayNorNight()).thenReturn(false);
        when(game.isDay()).thenReturn(true);
        when(FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_SHOW_STORM_COUNT_IN_PROMPT)).thenReturn(true);
        when(game.getView().getStormCount()).thenReturn(3);
        when(controller.macros().isRecording()).thenReturn(true);
        when(controller.macros().playbackText()).thenReturn("1 of 2");
        new InputPassPriority(controller).showMessage();
        verify(gui).showPromptMessage(eq(owner), contains("Day"), isNull());
        verify(gui).showPromptMessage(eq(owner), contains(Localizer.getInstance().getMessage("lblStormCount") + ": 3"), isNull());
        verify(gui).showPromptMessage(eq(owner), contains("Macro Recording -- 1 of 2"), isNull());
    }

    @Test
    public void selectionMinimumAndOptionalEmptyChoiceKeepTheirMeaning() {
        final Card card = mock(Card.class);
        when(card.getView()).thenReturn(mock(CardView.class));
        final FCollection<Card> choices = new FCollection<>(List.of(card));
        final InputSelectEntitiesFromList<Card> required = new InputSelectEntitiesFromList<>(controller, 1, 1, choices);
        required.showMessage();
        verify(gui).updateButtons(owner, "Confirm selection", "Cancel", false, false, true);
        final InputSelectEntitiesFromList<Card> optional = new InputSelectEntitiesFromList<>(controller, 0, 1, choices);
        optional.showMessage();
        verify(gui).updateButtons(owner, "Choose none", "Cancel", true, false, true);
        optional.selectButtonOK();
        Assert.assertTrue(optional.getSelected().isEmpty());
        Assert.assertFalse(optional.hasCancelled());
    }

    @Test
    public void massSelectionRefreshesConfirmLabelAfterSelectingAndClearing() {
        final Card card = mock(Card.class);
        when(card.getView()).thenReturn(source);
        when(card.getController()).thenReturn(player);
        final SpellAbility sa = mock(SpellAbility.class);
        when(sa.getView()).thenReturn(mock(SpellAbilityView.class));
        when(sa.getPayCosts()).thenReturn(mock(Cost.class));
        when(sa.getApi()).thenReturn(ApiType.Proliferate);
        final InputSelectEntitiesFromList<Card> input = new InputSelectEntitiesFromList<>(
                controller, 0, 1, new FCollection<>(List.of(card)), sa);
        input.showMessage();
        clearInvocations(gui);
        input.selectButtonCancel(); // Mine
        Assert.assertEquals(input.getSelected().size(), 1);
        verify(gui).updateButtons(eq(owner), eq("Confirm selection"), anyString(), eq(true), eq(false), eq(true));
        input.selectButtonCancel(); // All
        clearInvocations(gui);
        input.selectButtonCancel(); // Clear all
        Assert.assertTrue(input.getSelected().isEmpty());
        verify(gui).updateButtons(eq(owner), eq("Choose none"), anyString(), eq(true), eq(false), eq(true));
    }

    @Test
    public void targetsStillEnforceMandatoryChoicesAndAllowLegalZeroTargets() {
        final SpellAbility sa = mock(SpellAbility.class);
        final TargetRestrictions restrictions = mock(TargetRestrictions.class);
        final TargetChoices targets = new TargetChoices();
        final Card host = mock(Card.class);
        when(host.getView()).thenReturn(source);
        when(host.getTranslatedName()).thenReturn("Test ability");
        when(sa.getHostCard()).thenReturn(host);
        when(sa.getTargetRestrictions()).thenReturn(restrictions);
        when(sa.getTargets()).thenReturn(targets);
        when(sa.getView()).thenReturn(mock(SpellAbilityView.class));
        when(sa.getMaxTargets()).thenReturn(1);
        when(sa.getMinTargets()).thenReturn(1);
        when(restrictions.getVTSelection()).thenReturn("Select a target");
        when(restrictions.hasCandidates(sa)).thenReturn(true);
        final InputSelectTargets input = new InputSelectTargets(controller, List.of(), sa, true, null, null, null, false);
        input.showMessage();
        verify(gui).updateButtons(owner, "Confirm targets", "Cancel", false, false, false);
        when(sa.getMinTargets()).thenReturn(0);
        when(sa.isMinTargetChosen()).thenReturn(true);
        input.showMessage();
        verify(gui).updateButtons(owner, "Continue without targets", "Cancel", true, false, true);
        input.selectButtonOK();
        Assert.assertTrue(input.hasPressedOk());
        Assert.assertFalse(input.hasCancelled());
    }

    @Test
    public void paymentNamesTapManaAndRetainsMandatoryCancelRestriction() {
        final InputPayMana input = new InputPayMana(controller, mock(SpellAbility.class), player, false) {
            @Override protected void done() { }
            @Override protected String getMessage() { return "Pay mana"; }
        };
        input.updateButtons();
        verify(gui).updateButtons(owner, "Tap Mana", "Cancel", true, true, true);
        input.mandatory = true;
        input.updateButtons();
        verify(gui).updateButtons(owner, "Tap Mana", "Cancel", true, false, true);
    }

    private InputPayMana paymentInput(final SpellAbility paidFor, final Map<Card, List<SpellAbility>> abilities) {
        when(player.getCardsIn(any(ZoneType.class))).thenReturn(new CardCollection());
        when(player.getCardsIn(ZoneType.Battlefield)).thenReturn(new CardCollection(abilities.keySet()));
        when(player.getManaPool().canPayForShardWithColor(any(), anyByte())).thenAnswer(call ->
                call.getArgument(0) == ManaCostShard.GENERIC ||
                        (call.getArgument(0) == ManaCostShard.GREEN && (byte)call.getArgument(1) == MagicColor.GREEN));
        final SpellAbilityView view = mock(SpellAbilityView.class);
        when(view.getHostCard()).thenReturn(source);
        when(paidFor.getView()).thenReturn(view);
        return new InputPayMana(controller, paidFor, player, false) {
            { manaCost = new ManaCostBeingPaid(new ManaCost("1 G")); }
            @Override protected void done() { }
            @Override protected String getMessage() { return "Pay Mana Cost: " + manaCost; }
            @Override protected void runAsAi(Runnable task) { task.run(); }
            @Override protected List<SpellAbility> getAllManaAbilities(Card card) { return abilities.getOrDefault(card, List.of()); }
        };
    }

    private Card manaSource() {
        final Card card = mock(Card.class);
        when(card.getController()).thenReturn(player);
        when(card.getView()).thenReturn(mock(CardView.class));
        return card;
    }

    @Test
    public void tapManaHighlightsAllUsableChoicesWithoutPayingOrNeedingAnAutoPlan() {
        final SpellAbility paidFor = mock(SpellAbility.class);
        final Card first = manaSource(), second = manaSource(), restricted = manaSource();
        final SpellAbility usable = mock(SpellAbility.class), blocked = mock(SpellAbility.class);
        when(usable.isManaAbilityFor(eq(paidFor), anyByte())).thenReturn(true);
        final InputPayMana input = paymentInput(paidFor, Map.of(first, List.of(usable), second, List.of(usable), restricted, List.of(blocked)));
        try (MockedStatic<ComputerUtilMana> mana = mockStatic(ComputerUtilMana.class, withSettings().mockMaker("mock-maker-inline"))) {
            input.selectButtonOK();
            verify(gui).setSelection(argThat(selection ->
                    Set.copyOf(selection.actionable()).equals(Set.of(first.getView(), second.getView()))));
            verify(gui).showPromptMessage(eq(owner), contains("Click blue sources"), eq(source));
            Assert.assertFalse(input.canAutoPayMana());
            Assert.assertFalse(input.isPaid());
            verify(game, never()).getAction();
            mana.verify(() -> ComputerUtilMana.payManaCost(any(ManaCostBeingPaid.class), any(), any(), anyBoolean()), never());
            input.stop();
            verify(gui, atLeastOnce()).setSelection(argThat(selection -> selection.actionable().isEmpty()));
        }
    }

    @Test
    public void manualSourceCuesRefreshForRemainingColorAndSpentSources() {
        final SpellAbility paidFor = mock(SpellAbility.class);
        final Card green = manaSource(), red = manaSource();
        final SpellAbility greenAbility = mock(SpellAbility.class), redAbility = mock(SpellAbility.class);
        when(greenAbility.isManaAbilityFor(eq(paidFor), anyByte())).thenReturn(true);
        when(redAbility.isManaAbilityFor(eq(paidFor), anyByte())).thenAnswer(call ->
                ((byte)call.getArgument(1) & MagicColor.RED) != 0);
        final InputPayMana input = paymentInput(paidFor, Map.of(green, List.of(greenAbility), red, List.of(redAbility)));
        Assert.assertEquals(input.getManualManaSources(), Set.of(green.getView(), red.getView()));
        input.manaCost.decreaseShard(ManaCostShard.GENERIC, 1);
        Assert.assertEquals(input.getManualManaSources(), Set.of(green.getView()));
        when(greenAbility.isManaAbilityFor(eq(paidFor), anyByte())).thenReturn(false);
        Assert.assertTrue(input.getManualManaSources().isEmpty());
    }

    @Test
    public void separateAutoAvailabilityRechecksTheRemainingPayment() {
        final SpellAbility paidFor = mock(SpellAbility.class);
        final Card green = manaSource();
        final SpellAbility ability = mock(SpellAbility.class);
        when(ability.isManaAbilityFor(eq(paidFor), anyByte())).thenReturn(true);
        final InputPayMana input = paymentInput(paidFor, Map.of(green, List.of(ability)));
        try (MockedStatic<ComputerUtilMana> mana = mockStatic(ComputerUtilMana.class, withSettings().mockMaker("mock-maker-inline"))) {
            mana.when(() -> ComputerUtilMana.getManaSourcesToPayCost(any(), eq(paidFor), eq(player), eq(false)))
                    .thenReturn(new CardCollection(green)).thenReturn(null);
            input.updateMessage();
            Assert.assertTrue(input.canAutoPayMana());
            input.updateMessage();
            Assert.assertFalse(input.canAutoPayMana());
            verify(game, never()).getAction();
        }
    }

    @Test
    public void legacyAndNetworkPaymentRetainAutoPrimary() {
        final InputPayMana input = paymentInput(mock(SpellAbility.class), Map.of());
        when(gui.isNetGame()).thenReturn(true);
        input.updateButtons();
        verify(gui).updateButtons(owner, "Auto", "Cancel", false, true, false);
        Assert.assertFalse(input.usesManualPaymentUi());
    }

    private Card combatTapCreature(final SpellAbility ability) {
        final Card creature = mock(Card.class);
        when(creature.getView()).thenReturn(mock(CardView.class));
        when(creature.getDisplayName()).thenReturn("Stuffy Doll");
        when(creature.isCreature()).thenReturn(true);
        when(creature.isInZone(ZoneType.Battlefield)).thenReturn(true);
        when(creature.getController()).thenReturn(player);
        when(creature.getAllPossibleAbilities(eq(player), anyBoolean())).thenReturn(List.of(ability));
        when(ability.getPayCosts()).thenReturn(new Cost("T", true));
        when(ability.canPlay(true)).thenReturn(true);
        when(ability.toUnsuppressedString()).thenReturn("Tap: Stuffy Doll deals 1 damage to itself.");
        when(phases.getPlayerTurn()).thenReturn(mock(Player.class));
        when(phases.getPhase()).thenReturn(PhaseType.COMBAT_DECLARE_ATTACKERS);
        return creature;
    }

    @Test
    public void cancellingCombatTapConsumesClickWithoutActivatingOrAssigningBlocks() {
        final SpellAbility ability = mock(SpellAbility.class);
        final Card doll = combatTapCreature(ability);
        final InputPassPriority input = new InputPassPriority(controller);
        when(controller.getInputQueue().getInput()).thenReturn(input);
        Assert.assertTrue(input.onCardSelected(doll, null, mock(ITriggerEvent.class)));
        verify(gui).confirm(eq(doll.getView()), contains("can prevent declaring it as a blocker"),
                eq(false), eq(List.of("Activate", "Cancel")));
        Assert.assertNull(input.getChosenSa());
        verify(controller, never()).getAbilityToPlay(any(), anyList(), any());
    }

    @Test
    public void explicitCombatActivationStillWorksAndExplainsClosedBlockAssignments() {
        final SpellAbility ability = mock(SpellAbility.class);
        final Card doll = combatTapCreature(ability);
        when(phases.getPhase()).thenReturn(PhaseType.COMBAT_DECLARE_BLOCKERS);
        final InputPassPriority input = new InputPassPriority(controller);
        when(controller.getInputQueue().getInput()).thenReturn(input);
        when(gui.confirm(any(), anyString(), eq(false), anyList())).thenReturn(true);
        when(controller.getAbilityToPlay(eq(doll), anyList(), any())).thenReturn(ability);
        Assert.assertTrue(input.onCardSelected(doll, null, mock(ITriggerEvent.class)));
        verify(gui).confirm(eq(doll.getView()), contains("Blockers have already been confirmed"), eq(false), anyList());
        Assert.assertEquals(input.getChosenSa(), List.of(ability));
    }

    @Test
    public void changedInputDuringConfirmationCannotActivateAbility() {
        final SpellAbility ability = mock(SpellAbility.class);
        final Card doll = combatTapCreature(ability);
        final InputPassPriority input = new InputPassPriority(controller);
        when(gui.confirm(any(), anyString(), eq(false), anyList())).thenReturn(true);
        when(controller.getInputQueue().getInput()).thenReturn(new InputPassPriority(controller));
        Assert.assertTrue(input.onCardSelected(doll, null, mock(ITriggerEvent.class)));
        verify(controller, never()).getAbilityToPlay(any(), anyList(), any());
        Assert.assertNull(input.getChosenSa());
    }

    @Test
    public void combatTapGuardLeavesManaExistingMenusAndOtherInputPathsAlone() {
        final SpellAbility ability = mock(SpellAbility.class);
        final Card doll = combatTapCreature(ability);
        final ITriggerEvent click = mock(ITriggerEvent.class);
        when(ability.isManaAbility()).thenReturn(true);
        new InputPassPriority(controller).onCardSelected(doll, null, click);
        when(ability.isManaAbility()).thenReturn(false);
        when(ability.promptIfOnlyPossibleAbility()).thenReturn(true);
        new InputPassPriority(controller).onCardSelected(doll, null, click);
        when(ability.promptIfOnlyPossibleAbility()).thenReturn(false);
        when(phases.getPhase()).thenReturn(PhaseType.MAIN1);
        new InputPassPriority(controller).onCardSelected(doll, null, click);
        when(phases.getPhase()).thenReturn(PhaseType.COMBAT_DECLARE_ATTACKERS);
        when(phases.getPlayerTurn()).thenReturn(player);
        new InputPassPriority(controller).onCardSelected(doll, null, click);
        when(phases.getPlayerTurn()).thenReturn(mock(Player.class));
        new InputPassPriority(controller).onCardSelected(doll, null, null);
        when(gui.isNetGame()).thenReturn(true);
        new InputPassPriority(controller).onCardSelected(doll, null, click);
        verify(gui, never()).confirm(any(), anyString(), anyBoolean(), anyList());
        verify(controller, times(6)).getAbilityToPlay(eq(doll), anyList(), any());
    }

    @Test
    public void blockerSelectionAssignsStuffyDollWithoutInvokingItsAbility() {
        final SpellAbility ability = mock(SpellAbility.class);
        final Card doll = combatTapCreature(ability);
        when(doll.getGame()).thenReturn(game);
        when(game.getMatch()).thenReturn(mock(Match.class));
        final PlayerZone battlefield = mock(PlayerZone.class);
        when(player.getZone(ZoneType.Battlefield)).thenReturn(battlefield);
        when(battlefield.contains(doll)).thenReturn(true);
        final Card attacker = mock(Card.class);
        when(attacker.getView()).thenReturn(mock(CardView.class));
        when(attacker.getController()).thenReturn(player);
        when(attacker.getGame()).thenReturn(game);
        when(attacker.getDisplayName()).thenReturn("Stonecoil Serpent");
        final Combat combat = mock(Combat.class);
        when(combat.getAttackers()).thenReturn(new CardCollection());
        when(combat.getAllBlockers()).thenReturn(new CardCollection());
        when(combat.isAttacking(attacker)).thenReturn(true);
        try (MockedStatic<CombatUtil> rules = mockStatic(CombatUtil.class, withSettings().mockMaker("mock-maker-inline"))) {
            rules.when(() -> CombatUtil.canBlock(attacker, doll, combat)).thenReturn(true);
            final InputBlock input = new InputBlock(controller, player, combat);
            Assert.assertTrue(input.onCardSelected(attacker, null, null));
            Assert.assertTrue(input.onCardSelected(doll, null, null));
            verify(combat).addBlocker(attacker, doll);
            verify(controller, never()).getAbilityToPlay(any(), anyList(), any());
            verify(doll, never()).getAllPossibleAbilities(any(), anyBoolean());
            verify(gui, never()).confirm(any(), anyString(), anyBoolean(), anyList());
        }
    }

    @Test
    public void combatNamesTheCurrentChoiceAndCountsAttackers() {
        final Combat combat = mock(Combat.class);
        when(combat.getDefenders()).thenReturn(new FCollection<>());
        when(combat.getAttackers()).thenReturn(new CardCollection());
        when(combat.getAllBlockers()).thenReturn(new CardCollection());
        final InputAttack attack = new InputAttack(controller, player, combat);
        attack.showMessage();
        verify(gui, atLeastOnce()).updateButtons(owner, "No attacks", "Select all attackers", true, true, true);
        final Card attacker = mock(Card.class);
        when(attacker.getView()).thenReturn(mock(CardView.class));
        when(attacker.getController()).thenReturn(player);
        when(combat.getAttackers()).thenReturn(new CardCollection(attacker));
        attack.showMessage();
        verify(gui, atLeastOnce()).updateButtons(owner, "Confirm attackers (1)", "Clear attackers", true, true, true);
        final InputBlock block = new InputBlock(controller, player, combat);
        block.showMessage();
        verify(gui).updateButtons(owner, "No blocks", "Cancel", true, false, true);
        final Card blocker = mock(Card.class);
        when(blocker.getView()).thenReturn(mock(CardView.class));
        when(blocker.getController()).thenReturn(player);
        when(combat.getAllBlockers()).thenReturn(new CardCollection(blocker));
        block.showMessage();
        verify(gui).updateButtons(owner, "Confirm blocks", "Cancel", true, false, true);
    }
}
