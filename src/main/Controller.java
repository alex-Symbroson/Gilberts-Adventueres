package main;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
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

public class Controller implements Initializable
{
    // debug logger
    private static final Logger logger = Logger.getLogger(Controller.class.getName());
    @FXML
    protected Pane game;
    @FXML
    private Menu prefsMenu;
    @FXML
    private Label status_left, status_right;

    private Preferences prefs;
    private Stage stage;
    private File current;

    private Consumer<File> save, open;

    Controller(Consumer<File> save, Consumer<File> open)
    {
        this.save = save;
        this.open = open;
    }

    // initialize tree view with cell factory
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        String[] keys = {};
        try
        {
            keys = prefs.keys();
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
                Label text = new Label(name + ": " + prefs.get(key, ""));
                prop.addListener((obs, o, n) -> prefs.put(key, n));
                prop.addListener((obs, o, n) -> text.setText(name + ": " + n));
                prop.set(prefs.get(key, ""));

                DirectoryChooser dirc = new DirectoryChooser();
                dirc.setTitle(name);
                dirc.setInitialDirectory(new File(prefs.get(key, "")));
                text.setOnMouseClicked(e -> prop.set(dirc.showDialog(stage).toString()));

                prefsMenu.getItems().add(new CustomMenuItem(text, false));
            } else if (type == 'b')
            {
                CheckBox box = new CheckBox(name);
                box.setAllowIndeterminate(false);
                box.setSelected(prefs.getBoolean(key, false));
                box.setOnAction(e -> prefs.putBoolean(key, box.isSelected()));
                prefsMenu.getItems().add(new CustomMenuItem(box, false));
            }
        }

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
        fileChooser.setInitialDirectory(new File(prefs.get("Saves_Dirf", "")));
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Gilbert's Adventüres save file", "*.ga"));
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null)
            logger.info("open file " + selectedFile);
        else
            logger.info("open file aborted.");

        open.accept(selectedFile);
        current = selectedFile;
    }

    @FXML
    private void save(ActionEvent event)
    {
        if (current == null)
        {
            FileChooser fchoose = new FileChooser();
            fchoose.setTitle("Save adventüres");
            fchoose.setInitialDirectory(new File(prefs.get("Saves_Dirf", "")));
            fchoose.getExtensionFilters().add(new ExtensionFilter("Gilbert's Adventüres save file", "*.ga"));
            current = fchoose.showSaveDialog(stage);
        }

        save.accept(current);
    }

    @FXML
    private void saveAs(ActionEvent event)
    {
        FileChooser fchoose = new FileChooser();
        fchoose.setTitle("Save adventüres");
        fchoose.setInitialDirectory(new File(prefs.get("Saves_Dirf", "")));
        fchoose.getExtensionFilters().add(new ExtensionFilter("Gilbert's Adventüres save file", "*.ga"));
        current = fchoose.showSaveDialog(stage);

        save.accept(current);
    }

    // confirm and quit app
    @FXML
    private void quit(Event event)
    {
        boolean quit = true;

        if (prefs.getBoolean("Show_Exit_Dialogb", true))
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
        // labelpane.add(mkLabel("Nothing besides remains. Round the decay\nOf that colossal wreck, boundless and
        // bare\nThe lone and level sands stretch far away.",
        // Color.GOLD), 0, 2);
        alert.getDialogPane().setContent(labelpane);
        alert.getDialogPane().setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        alert.showAndWait();
    }

    public void setStage(Stage primaryStage)
    {
        this.stage = primaryStage;
        this.stage.setOnCloseRequest(e -> quit(e));
    }

    public void setPrefs(Preferences prefs)
    {
        this.prefs = prefs;
    }
}
