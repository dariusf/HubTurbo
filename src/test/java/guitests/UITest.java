package guitests;

import static com.google.common.io.Files.getFileExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.scene.control.*;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.loadui.testfx.GuiTest;
import org.loadui.testfx.exceptions.NoNodesFoundException;
import org.loadui.testfx.exceptions.NoNodesVisibleException;
import org.loadui.testfx.utils.KeyCodeUtils;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;

import com.google.common.util.concurrent.SettableFuture;

import backend.interfaces.RepoStore;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import prefs.Preferences;
import ui.IdGenerator;
import ui.MenuControl;
import ui.TestController;
import ui.UI;
import ui.components.FilterTextField;
import ui.listpanel.ListPanel;
import ui.listpanel.ListPanelCell;
import util.PlatformEx;
import util.PlatformSpecific;

public class UITest extends FxRobot {
    
    private static final Logger logger = LogManager.getLogger(UITest.class.getName());
    protected static final SettableFuture<Stage> STAGE_FUTURE = SettableFuture.create();
    private static final Map<Character, KeyCode> specialCharsMap = getSpecialCharsMap();
    
    // Sets property to run tests headless
    static {
        if (Boolean.getBoolean("headless")) {
            System.setProperty("java.awt.robot", "true");
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
        }
    }

    protected static class TestUI extends UI {
        public TestUI() {
            super();
        }

        @Override
        public void start(Stage primaryStage) {
            super.start(primaryStage);
            STAGE_FUTURE.set(primaryStage);
        }
    }

    @BeforeClass
    public static void setupSpec() {
        try {
            FxToolkit.registerPrimaryStage();
            FxToolkit.hideStage();
        } catch (TimeoutException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Before
    public void setStage() throws TimeoutException {
        FxToolkit.registerStage(() -> new Stage());
        FxToolkit.setupStage(this::handleSetupStage);
    }

    @Before
    public void setup() throws Exception {
        FxToolkit.setupApplication(TestUI.class, "--test=true", "--bypasslogin=true");
    }
    
    @After
    public void teardown() throws TimeoutException {
        FxToolkit.hideStage();
    }

    public Stage getStage() {
        return FxToolkit.toolkitContext().getRegisteredStage();
    }

    protected void handleSetupStage(Stage stage) {
        // delete test.json if it exists
        File testConfig = new File(Preferences.DIRECTORY, Preferences.TEST_CONFIG_FILE);
        if (testConfig.exists() && testConfig.isFile()) {
            assert testConfig.delete();
        }
        clearTestFolder();
        beforeStageStarts();
        stage.show();
    }

    public static void clearTestFolder() {
        try {
            if (Files.exists(Paths.get(RepoStore.TEST_DIRECTORY))) {
                Files.walk(Paths.get(RepoStore.TEST_DIRECTORY), 1)
                    .filter(Files::isRegularFile)
                    .filter(p ->
                        getFileExtension(String.valueOf(p.getFileName())).equalsIgnoreCase("json") ||
                            getFileExtension(String.valueOf(p.getFileName())).equalsIgnoreCase("json-err")
                    )
                    .forEach(p -> new File(p.toAbsolutePath().toString()).delete());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<Character, KeyCode> getSpecialCharsMap() {
        Map<Character, KeyCode> specialChars = new HashMap<Character, KeyCode>();
        specialChars.put('~', KeyCode.BACK_QUOTE);
        specialChars.put('!', KeyCode.DIGIT1);
        specialChars.put('@', KeyCode.DIGIT2);
        specialChars.put('#', KeyCode.DIGIT3);
        specialChars.put('$', KeyCode.DIGIT4);
        specialChars.put('%', KeyCode.DIGIT5);
        specialChars.put('^', KeyCode.DIGIT6);
        specialChars.put('&', KeyCode.DIGIT7);
        specialChars.put('*', KeyCode.DIGIT8);
        specialChars.put('(', KeyCode.DIGIT9);
        specialChars.put(')', KeyCode.DIGIT0);
        specialChars.put('_', KeyCode.MINUS);
        specialChars.put('+', KeyCode.EQUALS);
        specialChars.put('{', KeyCode.OPEN_BRACKET);
        specialChars.put('}', KeyCode.CLOSE_BRACKET);
        specialChars.put(':', KeyCode.SEMICOLON);
        specialChars.put('"', KeyCode.QUOTE);
        specialChars.put('<', KeyCode.COMMA);
        specialChars.put('>', KeyCode.PERIOD);
        specialChars.put('?', KeyCode.SLASH);
        return Collections.unmodifiableMap(specialChars);
    }

    protected void beforeStageStarts() {
        // method to be overridden if anything needs to be done (e.g. to the json) before the stage starts
    }
    
    // ================
    // Waiting routines
    // ================

    public void waitUntilNodeAppears(Node node) {
        GuiTest.waitUntil(node, n -> n.isVisible() && n.getParent() != null);
    }

    public void waitUntilNodeDisappears(Node node) {
        GuiTest.waitUntil(node, n -> !n.isVisible() || n.getParent() == null);
    }

    public void waitUntilNodeAppears(String selector) {
        awaitCondition(() -> existsQuiet(selector));
    }

    public void waitUntilNodeDisappears(String selector) {
        awaitCondition(() -> !existsQuiet(selector));
    }

    public void waitUntilNodeAppears(Matcher<Object> matcher) {
        // We use find because there's no `exists` for matchers
        awaitCondition(() -> findQuiet(matcher).isPresent());
    }

    public void waitUntilNodeDisappears(Matcher<Object> matcher) {
        // We use find because there's no `exists` for matchers
        awaitCondition(() -> !findQuiet(matcher).isPresent());
    }

    public <T extends Node> void waitUntil(String selector, Predicate<T> condition) {
        awaitCondition(() -> condition.test(GuiTest.find(selector)));
    }

    /**
     * Waits for the result of a function, then asserts that it is equal to some value.
     */
    public <T> void waitAndAssertEquals(T expected, Supplier<T> actual) {
        awaitCondition(() -> expected.equals(actual.get()));
    }

    public <T> void waitForValue(ComboBoxBase<T> comboBoxBase) {
        GuiTest.waitUntil(comboBoxBase, c -> c.getValue() != null);
    }

    public <T extends Node> T findOrWaitFor(String selector) {
        waitUntilNodeAppears(selector);
        return GuiTest.find(selector);
    }

    public <T extends Node> Optional<T> findQuiet(String selectorOrText) {
        try {
            return Optional.of(GuiTest.find(selectorOrText));
        } catch (NoNodesFoundException | NoNodesVisibleException e) {
            return Optional.empty();
        }
    }

    private <T extends Node> Optional<T> findQuiet(Matcher<Object> matcher) {
        try {
            return Optional.ofNullable(GuiTest.find(matcher));
        } catch (NoNodesFoundException | NoNodesVisibleException e) {
            return Optional.empty();
        }

    }

    public boolean existsQuiet(String selector) {
        try {
            return GuiTest.exists(selector);
        } catch (NoNodesFoundException | NoNodesVisibleException e) {
            return false;
        }
    }

    /**
     * Allows test threads to busy-wait on some condition.
     * <p>
     * Taken from org.loadui.testfx.utils, but modified to synchronise with
     * the JavaFX Application Thread, with a lower frequency.
     * The additional synchronisation prevents bugs where
     * <p>
     * awaitCondition(a);
     * awaitCondition(b);
     * <p>
     * sometimes may not be equivalent to
     * <p>
     * awaitCondition(a && b);
     * <p>
     * The lower frequency is a bit more efficient, since a frequency of 10 ms
     * just isn't necessary for GUI interactions, and we're bottlenecked by the FX
     * thread anyway.
     */
    public void awaitCondition(Callable<Boolean> condition) {
        awaitCondition(condition, 5);
    }

    private void awaitCondition(Callable<Boolean> condition, int timeoutInSeconds) {
        long timeout = System.currentTimeMillis() + timeoutInSeconds * 1000;
        try {
            while (!condition.call()) {
                Thread.sleep(100);
                PlatformEx.waitOnFxThread();
                if (System.currentTimeMillis() > timeout) {
                    throw new TimeoutException();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // ====================
    // Interaction routines
    // ====================

    /**
     * Like drag(from).to(to), but does not relocate the mouse if the target moves.
     */
    public void dragUnconditionally(Node from, Node to) {
        moveTo(from);
        press(MouseButton.PRIMARY);
        moveTo(to);
        release(MouseButton.PRIMARY);
    }

    /**
     * Automate menu traversal by clicking them in order of input parameter
     *
     * @param menuNames array of strings of menu item names in sequence of traversal
     */
    public void clickMenu(String... menuNames) {
        for (String menuName : menuNames) {
            clickOn(menuName);
        }
    }

    /**
     * Clicks the repository selector's ComboBox
     */
    public void clickRepositorySelector() {
        click(IdGenerator.getRepositorySelectorIdReference());
    }

    /**
     * Gets the repository selector's ComboBox
     */
    public ComboBox getRepositorySelector() {
        return find(IdGenerator.getRepositorySelectorIdReference());
    }

    /**
     * Clicks the label picker's TextField
     */
    public void clickLabelPickerTextField() {
        click(IdGenerator.getLabelPickerTextFieldIdReference());
    }

    /**
     * Gets the label picker's TextField
     */
    public TextField getLabelPickerTextField() {
        return find(IdGenerator.getLabelPickerTextFieldIdReference());
    }

    /**
     * Clicks the FilterTextField of the panel at {@code panelIndex}
     * @param panelIndex
     */
    public void clickFilterTextFieldAtPanel(int panelIndex) {
        click(IdGenerator.getPanelFilterTextFieldIdReference(panelIndex));
    }

    /**
     * Gets the FilterTextField of the panel at {@code panelIndex}
     * @param panelIndex
     */
    public FilterTextField getFilterTextFieldAtPanel(int panelIndex) {
        return find(IdGenerator.getPanelFilterTextFieldIdReference(panelIndex));
    }

    /**
     * Clicks the issue with id {@code issueId} at panel {@code panelIndex}
     * @param panelIndex
     * @param issueId
     */
    public void clickIssue(int panelIndex, int issueId) {
        click(IdGenerator.getPanelCellIdReference(panelIndex, issueId));
    }

    /**
     * Right clicks the issue with id {@code issueId} at panel {@code panelIndex}
     * @param panelIndex
     * @param issueId
     */
    public void rightClickIssue(int panelIndex, int issueId) {
        rightClick(IdGenerator.getPanelCellIdReference(panelIndex, issueId));
    }

    /**
     * Clicks the panel {@code panelIndex}
     * @param panelIndex
     */
    public void clickPanel(int panelIndex) {
        click(IdGenerator.getPanelIdReference(panelIndex));
    }

    /**
     * Right clicks the panel {@code panelIndex}
     * @param panelIndex
     */
    public void rightClickPanel(int panelIndex) {
        rightClick(IdGenerator.getPanelIdReference(panelIndex));
    }

    /**
     * Gets the panel {@code panelIndex}
     * @param panelIndex
     */
    public ListPanel getPanel(int panelIndex) {
        return find(IdGenerator.getPanelIdReference(panelIndex));
    }

    /**
     * Gets the issue cell of issue {@code issueId} at panel {@code panelIndex}
     * @param panelIndex
     * @param issueId
     */
    public ListPanelCell getIssueCell(int panelIndex, int issueId) {
        return find(IdGenerator.getPanelCellIdReference(panelIndex, issueId));
    }

    /**
     * Sets a text field with given text. Does not simulate clicking and typing
     * in the text field.
     *
     * @param fieldId
     * @param text
     */
    public void setTextField(String fieldId, String text) {
        waitUntilNodeAppears(fieldId);
        ((TextField) GuiTest.find(fieldId)).setText(text);
    }

    /**
     * Traverses HubTurbo's menu, looking for a chain of nodes with the
     * given names and triggering their associated action.
     * <p>
     * This is a more reliable method of triggering menu items than
     * {@link #clickMenu}, especially when dealing with nested menu items.
     * It is a drop-in replacement in most cases.
     * <p>
     * Caveats: ensure that adequate synchronisation is used after this method
     * if it is called from a thread other than the UI thread.
     *
     * @param names the chain of menu nodes to visit
     */
    public void traverseMenu(String... names) {
        assert names.length > 0 : "traverseMenu called with no arguments";

        Platform.runLater(() -> {
            MenuControl root = TestController.getUI().getMenuControl();
            MenuItem current = root.getMenus().stream()
                    .filter(m -> m.getText().equals(names[0]))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(String.format("%s is not a valid menu item", names[0])));

            for (int i = 1; i < names.length; i++) {
                final int j = i;
                if (!(current instanceof Menu)) {
                    throw new IllegalArgumentException(
                            String.format("Menu %s is not as nested as arguments require", names[0]));
                }
                current = ((Menu) current).getItems().stream()
                        .filter(m -> m.getText().equals(names[j]))
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(String.format("%s is not a valid menu item", names[j])));
            }

            current.getOnAction().handle(new ActionEvent());
        });
    }

    private List<KeyCode> getKeyCodes(KeyCodeCombination combination) {
        List<KeyCode> keys = new ArrayList<>();
        if (combination.getAlt() == KeyCombination.ModifierValue.DOWN) {
            keys.add(KeyCode.ALT);
        }
        if (combination.getShift() == KeyCombination.ModifierValue.DOWN) {
            keys.add(KeyCode.SHIFT);
        }
        if (combination.getMeta() == KeyCombination.ModifierValue.DOWN) {
            keys.add(KeyCode.META);
        }
        if (combination.getControl() == KeyCombination.ModifierValue.DOWN) {
            keys.add(KeyCode.CONTROL);
        }
        if (combination.getShortcut() == KeyCombination.ModifierValue.DOWN) {
            // Fix bug with internal method not having a proper code for SHORTCUT.
            // Dispatch manually based on platform.
            if (PlatformSpecific.isOnMac()) {
                keys.add(KeyCode.META);
            } else {
                keys.add(KeyCode.CONTROL);
            }
        }
        keys.add(combination.getCode());
        return keys;
    }

    public void push(KeyCode keyCode, int times) {
        assert times > 0;
        for (int i = 0; i < times; i++) {
            push(keyCode);
        }
    }

    public void pushKeys(KeyCodeCombination combination) {
        pushKeys(getKeyCodes(combination));
    }

    public void pushKeys(KeyCode... keys) {
        pushKeys(Arrays.asList(keys));
    }

    private void pushKeys(List<KeyCode> keys) {
        keys.forEach(this::press);
        for (int i = keys.size() - 1; i >= 0; i--) {
            release(keys.get(i));
        }
        PlatformEx.waitOnFxThread();
    }

    public void press(KeyCodeCombination combination) {
        press(getKeyCodes(combination));
    }

    private void press(List<KeyCode> keys) {
        keys.forEach(this::press);
        for (int i = keys.size() - 1; i >= 0; i--) {
            release(keys.get(i));
        }
        PlatformEx.waitOnFxThread();
    }
    /**
     * Used to select the whole filter text so that it can be replaced
     */
    public void selectAll() {
        pushKeys(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
    }

    public FxRobot type(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (specialCharsMap.containsKey(text.charAt(i))) {
                press(KeyCode.SHIFT).press(specialCharsMap.get(text.charAt(i)))
                        .release(specialCharsMap.get(text.charAt(i))).release(KeyCode.SHIFT);

            } else {
                char typed = text.charAt(i);
                KeyCode identified = KeyCodeUtils.findKeyCode(typed);
                if (Character.isUpperCase(typed)) {
                    press(KeyCode.SHIFT).type(identified).release(KeyCode.SHIFT);
                } else {
                    type(identified);
                }
            }
        }
        return this;
    }
    /**
     * Performs UI login on the login dialog box.
     * @param owner The owner of the repo.
     * @param repoName The repository name
     * @param username The Github username
     * @param password The Github password
     */
    public void login(String owner, String repoName, String username, String password){
        selectAll();
        type(owner).push(KeyCode.TAB);
        type(repoName).push(KeyCode.TAB);
        type(username).push(KeyCode.TAB);
        type(password);
        clickOn("Sign in");
    }

    /**
     * Performs logout from File -> Logout on HubTurbo's pView.
     */
    public void logout(){
        clickMenu("File", "Logout");
    }
}
