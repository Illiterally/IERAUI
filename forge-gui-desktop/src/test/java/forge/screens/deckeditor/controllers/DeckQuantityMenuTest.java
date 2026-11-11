package forge.screens.deckeditor.controllers;

import forge.card.CardRules;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FSkin;
import forge.localinstance.skin.FSkinProp;
import forge.util.Localizer;
import org.mockito.MockedStatic;
import org.testng.annotations.*;

import javax.swing.JMenu;
import java.util.List;
import java.util.Map.Entry;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DeckQuantityMenuTest {
    private IGuiBase previousGui;
    private String previousEnabled;
    private MockedStatic<FModel> modelMock;
    private CEditorConstructed editor;
    private ItemManager<PaperCard> catalog;
    private ItemManager<PaperCard> deckManager;
    private DeckController<Deck> controller;
    private Deck deck;
    private PaperCard target;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void prepare() {
        previousEnabled = System.getProperty("forge.enhancedRulesUi");
        System.setProperty("forge.enhancedRulesUi", "true");
        previousGui = GuiBase.getInterface();
        IGuiBase platform = mock(IGuiBase.class);
        when(platform.getAssetsDir()).thenReturn("../forge-gui/");
        GuiBase.setInterface(platform);
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
        modelMock = mockStatic(FModel.class, withSettings().mockMaker("mock-maker-inline"));
        ForgePreferences preferences = mock(ForgePreferences.class);
        when(preferences.getPrefInt(FPref.DECK_DEFAULT_CARD_LIMIT)).thenReturn(4);
        modelMock.when(FModel::getPreferences).thenReturn(preferences);

        editor = mock(CEditorConstructed.class, CALLS_REAL_METHODS);
        // ItemManager's static search icon normally comes from the running desktop skin.
        try (MockedStatic<FSkin> skin = mockStatic(FSkin.class)) {
            FSkin.SkinIcon icon = mock(FSkin.SkinIcon.class);
            when(icon.resize(anyInt(), anyInt())).thenReturn(icon);
            skin.when(() -> FSkin.getIcon(any(FSkinProp.class))).thenReturn(icon);
            catalog = mock(ItemManager.class);
            deckManager = mock(ItemManager.class);
        }
        controller = mock(DeckController.class);
        deck = new Deck("Quantity regression");
        deck.getOrCreate(DeckSection.Sideboard);
        target = card("Spellskite", "Artifact Creature — Phyrexian Horror");
        editor.sectionMode = DeckSection.Main;
        doReturn(catalog).when(editor).getCatalogManager();
        doReturn(deckManager).when(editor).getDeckManager();
        doReturn(controller).when(editor).getDeckController();
        doReturn(deck).when(editor).getHumanDeck();
        doReturn(ACEditorBase.CardLimit.None).when(editor).getCardLimit();
        doNothing().when(editor).refreshQuantityStatistics();
        when(controller.getModel()).thenReturn(deck);
        when(catalog.isInfinite()).thenReturn(true);
        when(catalog.getGenericType()).thenReturn(PaperCard.class);
        when(deckManager.getGenericType()).thenReturn(PaperCard.class);
        when(deckManager.getSelectedItems()).thenReturn(List.of(target));
        when(deckManager.getItemCount(any())).thenAnswer(call -> currentPool().count(call.getArgument(0)));
        doAnswer(call -> { currentPool().addAll(call.<Iterable<Entry<PaperCard, Integer>>>getArgument(0)); return null; })
                .when(deckManager).addItems(any());
        doAnswer(call -> { currentPool().removeAll(call.<Iterable<Entry<PaperCard, Integer>>>getArgument(0)); return null; })
                .when(deckManager).removeItems(any());
    }

    @AfterMethod
    public void restore() {
        if (modelMock != null) { modelMock.close(); }
        GuiBase.setInterface(previousGui);
        if (previousEnabled == null) { System.clearProperty("forge.enhancedRulesUi"); }
        else { System.setProperty("forge.enhancedRulesUi", previousEnabled); }
    }

    private CardPool currentPool() { return deck.getOrCreate(editor.sectionMode); }

    private PaperCard card(String name, String types) {
        PaperCard card = mock(PaperCard.class);
        when(card.getName()).thenReturn(name);
        when(card.getRules()).thenReturn(CardRules.fromScript(List.of("Name:" + name, "Types:" + types,
                "ManaCost:2", "PT:0/4", "Oracle:Test card.")));
        return card;
    }

    @DataProvider
    public Object[][] quantities() { return new Object[][] {{1}, {2}, {3}, {4}}; }

    @Test(dataProvider = "quantities")
    public void addingUsesCapturedDeckCardNotCatalogOrLaterSelection(int quantity) {
        currentPool().add(target, 2);
        PaperCard unrelated = card("Lodestone Golem", "Artifact Creature — Golem");
        currentPool().add(unrelated, 1);
        when(catalog.getSelectedItems()).thenReturn(List.of(unrelated));
        List<JMenu> menus = editor.createDeckQuantityMenus();
        when(deckManager.getSelectedItems()).thenReturn(List.of(unrelated));
        menus.get(0).getItem(quantity - 1).doClick();
        assertEquals(currentPool().count(target), 2 + quantity);
        assertEquals(currentPool().count(unrelated), 1);
        verify(controller, times(1)).notifyModelChanged();
        verify(editor, times(1)).refreshQuantityStatistics();
    }

    @Test(dataProvider = "quantities")
    public void removingUsesExactQuantityAndMarksDeckDirty(int quantity) {
        currentPool().add(target, 4);
        editor.createDeckQuantityMenus().get(1).getItem(quantity - 1).doClick();
        assertEquals(currentPool().count(target), 4 - quantity);
        verify(controller, times(1)).notifyModelChanged();
        verify(editor, times(1)).refreshQuantityStatistics();
    }

    @Test
    public void allFourChoicesStayVisibleAndUnavailableCountsAreDisabled() {
        doReturn(ACEditorBase.CardLimit.Default).when(editor).getCardLimit();
        currentPool().add(target, 2);
        List<JMenu> menus = editor.createDeckQuantityMenus();
        assertEquals(menus.get(0).getText(), "Add copies");
        assertEquals(menus.get(1).getText(), "Remove copies");
        for (JMenu menu : menus) {
            assertEquals(menu.getItemCount(), 4);
            for (int i = 0; i < 4; i++) {
                assertEquals(menu.getItem(i).isEnabled(), i < 2);
            }
            menu.getItem(3).doClick();
        }
        assertEquals(currentPool().count(target), 2);
        verify(controller, never()).notifyModelChanged();
    }

    @Test
    public void singletonAndBasicLandLimitsFollowForge() {
        doReturn(ACEditorBase.CardLimit.Singleton).when(editor).getCardLimit();
        currentPool().add(target, 1);
        assertFalse(editor.createDeckQuantityMenus().get(0).getItem(0).isEnabled());
        PaperCard land = card("Island", "Basic Land — Island");
        currentPool().add(land, 10);
        when(deckManager.getSelectedItems()).thenReturn(List.of(land));
        List<JMenu> menus = editor.createDeckQuantityMenus();
        assertTrue(menus.get(0).getItem(3).isEnabled());
        assertEquals(menus.get(1).getItem(5).getText(), "Other amount...");
        menus.get(0).getItem(3).doClick();
        assertEquals(currentPool().count(land), 14);
    }

    @Test
    public void multiplePrintingsShareOneCopyLimitAcrossDeckSections() {
        doReturn(ACEditorBase.CardLimit.Default).when(editor).getCardLimit();
        PaperCard otherPrinting = card("Spellskite", "Artifact Creature — Phyrexian Horror");
        currentPool().add(target, 1);
        currentPool().add(otherPrinting, 1);
        deck.get(DeckSection.Sideboard).add(target, 1);
        when(deckManager.getSelectedItems()).thenReturn(List.of(target, otherPrinting));
        assertFalse(editor.createDeckQuantityMenus().get(0).getItem(0).isEnabled());
        deck.get(DeckSection.Sideboard).clear();
        List<JMenu> menus = editor.createDeckQuantityMenus();
        assertEquals(menus.get(0).getItem(0).getText(), "1 of each selected card");
        menus.get(0).getItem(0).doClick();
        assertEquals(currentPool().count(target), 2);
        assertEquals(currentPool().count(otherPrinting), 2);
    }

    @Test
    public void sideboardChangesStayInSideboard() {
        editor.sectionMode = DeckSection.Sideboard;
        currentPool().add(target, 3);
        editor.createDeckQuantityMenus().get(0).getItem(1).doClick();
        assertEquals(currentPool().count(target), 5);
        assertEquals(deck.getMain().count(target), 0);
        editor.createDeckQuantityMenus().get(1).getItem(3).doClick();
        assertEquals(currentPool().count(target), 1);
        verify(controller, times(2)).notifyModelChanged();
    }

    @Test
    public void staleMenuCannotOverRemoveOrEditAnotherSectionOrDeck() {
        currentPool().add(target, 4);
        List<JMenu> menus = editor.createDeckQuantityMenus();
        currentPool().remove(target, 3);
        menus.get(1).getItem(3).doClick();
        assertEquals(currentPool().count(target), 1);
        editor.sectionMode = DeckSection.Sideboard;
        menus.get(0).getItem(0).doClick();
        assertEquals(currentPool().count(target), 0);
        editor.sectionMode = DeckSection.Main;
        when(controller.getModel()).thenReturn(new Deck("Different deck"));
        menus.get(0).getItem(0).doClick();
        assertEquals(currentPool().count(target), 1);
        verify(controller, never()).notifyModelChanged();
    }

    @Test
    public void finitePoolsCommanderSlotsAndStockModeKeepOriginalMenus() {
        assertTrue(editor.supportsDeckQuantityMenu());
        when(catalog.isInfinite()).thenReturn(false);
        assertFalse(editor.supportsDeckQuantityMenu());
        when(catalog.isInfinite()).thenReturn(true);
        editor.sectionMode = DeckSection.Commander;
        assertFalse(editor.supportsDeckQuantityMenu());
        editor.sectionMode = DeckSection.Main;
        System.setProperty("forge.enhancedRulesUi", "false");
        assertFalse(editor.supportsDeckQuantityMenu());
    }
}
