package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import script.Script;
import script.ScriptException;

public class Controller
{
    // logger
    private static final Logger logger = Logger.getLogger(Controller.class.getName());
    @FXML
    protected Pane game;
    @FXML
    private Menu prefsMenu;
    @FXML
    private Label status_left, status_right;
    @FXML
    private MenuItem showConsole;

    private Main main;

    private Stage stage;
    private File current;
    private BooleanProperty holding_c;

    Controller(Main main)
    {
        this.main = main;

        holding_c = new SimpleBooleanProperty(false);
    }

    public void initialize()
    {
        String[] keys = {};
        try
        {
            keys = main.prefs.keys();
        } catch (BackingStoreException e)
        {
            e.printStackTrace();
        }
        for (String key : keys)
        {
            String name = key.substring(0, key.length() - 1).replace('_', ' ');
            char type = key.charAt(key.length() - 1);
            if (type == 'f')
            {
                StringProperty prop = new SimpleStringProperty(null);
                Label text = new Label(name + ": " + main.prefs.get(key, ""));
                prop.addListener((obs, o, n) -> main.prefs.put(key, n));
                prop.addListener((obs, o, n) -> text.setText(name + ": " + n));
                prop.set(main.prefs.get(key, ""));

                DirectoryChooser dirc = new DirectoryChooser();
                dirc.setTitle(name);
                dirc.setInitialDirectory(new File(main.prefs.get(key, "")));
                text.setOnMouseClicked(e -> prop.set(dirc.showDialog(stage).toString()));

                prefsMenu.getItems().add(new CustomMenuItem(text, false));
            } else if (type == 'b')
            {
                CheckBox box = new CheckBox(name);
                box.setAllowIndeterminate(false);
                box.setSelected(main.prefs.getBoolean(key, false));
                box.setOnAction(e -> main.prefs.putBoolean(key, box.isSelected()));
                prefsMenu.getItems().add(new CustomMenuItem(box, false));
            }
        }

        game.setOnKeyPressed(e ->
        {
            if ("c".equals(e.getText()) && e.isControlDown() && e.isShiftDown()) holding_c.set(true);
        });
        game.setOnKeyReleased(e ->
        {
            if ("c".equals(e.getText())) holding_c.set(false);
        });

        showConsole.visibleProperty().bind(holding_c);
    }

    public void setStatus(String status)
    {
        status_right.setText(status);
    }

    @FXML
    private void loadNewFile(ActionEvent event)
    {
        logger.info("load new file");
        current = null;
    }

    @FXML
    private void open(ActionEvent event)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Continue adventüres");
        fileChooser.setInitialDirectory(new File(main.prefs.get("Saves_Dirf", "")));
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Gilbert's Adventüres save file", "*.ga"));
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null)
        {
            logger.info("open file " + selectedFile);
            main.loadGame(selectedFile);
            current = selectedFile;
        } else
            logger.info("open file aborted.");
    }

    @FXML
    private void save(ActionEvent event)
    {
        if (current == null)
        {
            FileChooser fchoose = new FileChooser();
            fchoose.setTitle("Save adventüres");
            fchoose.setInitialDirectory(new File(main.prefs.get("Saves_Dirf", "")));
            fchoose.getExtensionFilters().add(new ExtensionFilter("Gilbert's Adventüres save file", "*.ga"));

            current = fchoose.showSaveDialog(stage);
        }

        if (current != null)
        {
            logger.info("save to file " + current);
            main.saveGame(current);
        } else
            logger.info("save to file aborted.");
    }

    @FXML
    private void saveAs(ActionEvent event)
    {
        FileChooser fchoose = new FileChooser();
        fchoose.setTitle("Save adventüres");
        fchoose.setInitialDirectory(new File(main.prefs.get("Saves_Dirf", "")));
        fchoose.getExtensionFilters().add(new ExtensionFilter("Gilbert's Adventüres save file", "*.ga"));
        current = fchoose.showSaveDialog(stage);

        if (current != null)
        {
            logger.info("save to file " + current);
            main.saveGame(current);
        } else
            logger.info("save to file aborted.");
    }

    @FXML
    private void showConsole(Event event)
    {
        holding_c.set(false);

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Script Console");

        DialogPane content = dialog.getDialogPane();
        content.setHeader(null);
        content.getButtonTypes().add(new ButtonType("Done", ButtonData.NO));
        content.getStylesheets().addAll(game.getParent().getStylesheets());

        TextArea text = new TextArea();
        text.setMinHeight(stage.getHeight());
        text.setWrapText(true);
        text.setTextFormatter(
                new TextFormatter<>(new ConsoleFilter(text, new HashMap<String, Integer>(), dialog::close)));

        content.setContent(text);

        dialog.showAndWait();
    }

    @FXML
    private void restart(Event event)
    {
        main.restart(stage);
    }

    // confirm and quit app
    @FXML
    private void quit(Event event)
    {
        boolean quit = true;

        if (main.prefs.getBoolean("Show_Exit_Dialogb", true))
        {
            // two reasons for using ButtonData.NO:
            // has ButtonData.defaultButton = false, inheriting style data from its DialogPane
            // is always to left of ButtonData.CANCEL_CLOSE, but without a big gap (ButtonBar BUTTON_ORDER_ constants)
            Alert dlg = new Alert(AlertType.CONFIRMATION, null, new ButtonType("Yes", ButtonData.NO),
                    ButtonType.CANCEL);
            dlg.setTitle("Exit confirmation");
            dlg.setHeaderText("");
            dlg.setContentText(
                    "When you exit the game, all unsaved progress will be lost.\nExit Gilbert's Adventüres?");
            dlg.getDialogPane().setStyle("-fx-base: #000;");

            dlg.showAndWait();
            if (dlg.getResult() == ButtonType.CANCEL) quit = false;
        }

        if (quit)
        {
            logger.info("closing");
            Platform.exit();
        }
    }

    private Label mkLabel(String text, Paint color, Font... font)
    {
        Label label = new Label(text);
        label.setTextFill(color);
        if (font.length > 0) label.setFont(font[0]);
        return label;
    }

    @FXML
    private void showAbout(ActionEvent event)
    {
        Alert alert = new Alert(AlertType.CONFIRMATION, "", new ButtonType("What"), new ButtonType("What?"));
        alert.setTitle("About");
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.getDialogPane().setStyle("-fx-base: #000;");

        GridPane labelpane = new GridPane();
        labelpane.setHgap(10);
        labelpane.setVgap(10);
        labelpane.add(mkLabel("And on the pedestal these words appear:", Color.GOLD), 0, 0);
        labelpane.add(
                mkLabel("'My name is Richarmandias, king of kings:\nLook upon my creation, ye Mighty, and despair!'",
                        Color.WHITE, Font.font("Copperplate Gothic Light", 14)),
                0, 1);
        alert.getDialogPane().setContent(labelpane);
        alert.getDialogPane().setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        alert.showAndWait();
    }

    public void setStage(Stage primaryStage)
    {
        this.stage = primaryStage;
        this.stage.setOnCloseRequest(e -> quit(e));
    }

    class ConsoleFilter implements UnaryOperator<Change>
    {
        int last_prompt, index;
        TextArea area;
        Map<String, Integer> context;
        List<String> lines;
        Runnable on_exit;

        ConsoleFilter(TextArea text_area, Map<String, Integer> context, Runnable on_exit)
        {
            area = text_area;
            area.appendText(" >> ");
            last_prompt = area.getLength();
            index = 0;
            lines = new ArrayList<String>();

            this.context = context;
            this.on_exit = on_exit;
        }

        public Change apply(Change c)
        {
            // arrow up -> show previous line
            if (c.getRangeStart() < last_prompt)
                if (index > 0 && c.getRangeStart() < last_prompt - 1)
                {
                    --index;
                    String line = lines.get(index);
                    c.setText(line);
                    c.setRange(last_prompt, area.getLength());
                    int pos = last_prompt + line.length();
                    c.selectRange(pos, pos);
                    return c;
                } else
                    return null;
            // double \n -> execute script
            else if (c.getControlText().endsWith("\n"))
            {
                if (c.getText().equals("\n"))
                {
                    StringBuilder text = new StringBuilder();

                    String script_text = area.getText(last_prompt, area.getLength()).trim();
                    if ("!exit".equals(script_text) || "!quit".equals(script_text) || "!done".equals(script_text))
                    {
                        on_exit.run();
                        return null;
                    }
                    lines.add(script_text);
                    Script script = main.loader.loadScript(script_text);
                    main.eval.reset(script);
                    main.eval.setCurrentObject(null);
                    Consumer<String> old_write = main.eval.setWriteText(s -> text.append(s + "\n"));

                    try
                    {
                        int res = main.eval.eval(context);
                        text.append(Integer.toString(res));
                    } catch (ScriptException e)
                    {
                        text.append("Error: " + e.getMessage());
                    }
                    main.eval.setWriteText(old_write);
                    text.append("\n >> ");

                    int pos = c.getControlText().length() + text.length();
                    last_prompt = pos;
                    c.setText(text.toString());
                    c.selectRange(pos, pos);
                }
                // after single \n -> insert inset
                else if (!c.getText().isEmpty() && c.getRangeEnd() == area.getLength())
                {
                    c.setText("    " + c.getText());
                    int pos = c.getCaretPosition();
                    c.selectRange(pos + 4, pos + 4);
                }
            }
            // after '(' insert ')' (for text, insert "('')")
            else if ("(".equals(c.getText()))
                if (area.getText().substring(0, c.getRangeStart()).endsWith("text"))
                {
                    c.setText("('')");
                    c.selectRange(c.getAnchor() + 1, c.getCaretPosition() + 1);
                } else
                    c.setText("()");
            // after '{' insert '}'
            else if ("{".equals(c.getText())) c.setText("{}");
            index = lines.size();
            return c;
        }
    }
}
