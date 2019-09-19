package main;

import java.io.File;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Controller
{
    // debug logger
    private static final Logger logger = Logger.getLogger(Controller.class.getName());
    @FXML
    protected Pane game;
    @FXML
    private Menu prefsMenu;
    @FXML
    private Label status_left, status_right;

    private Stage stage;

    // initialize tree view with cell factory
    public void initialize()
    {
        /*
         * String[] keys = {}; try { keys = Main.prefs.keys(); } catch (BackingStoreException e) { e.printStackTrace();
         * } for (String key : keys) { // TODO: add text for text preferences CheckBox box = new
         * CheckBox(key.replace('_', ' ')); box.setAllowIndeterminate(false); box.setSelected(Main.prefs.getBoolean(key,
         * false)); box.setOnAction(e -> Main.prefs.putBoolean(key, box.isSelected())); prefsMenu.getItems().add(new
         * CustomMenuItem(box, false)); }
         */
    }

    public void setStatus(String status)
    {
        status_right.setText(status);
    }

    @FXML
    private void loadNewFile(ActionEvent event)
    {
        logger.info("load new file");
    }

    // let user select a json file
    @FXML
    private void openFile(ActionEvent event)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Continue adventüres");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Gilbert's Adventüres file", "*.ga"));
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null)
            logger.info("open file " + selectedFile);
        else
            logger.info("open file aborted.");
    }

    // confirm and quit app
    @FXML
    private void quit(Event event)
    {
        boolean quit = true;

        /*
         * if (Main.prefs.getBoolean("Show_Exit_Dialog", true)) { // two reasons for using ButtonData.NO: // has
         * ButtonData.defaultButton = false, inheriting style data from its DialogPane // is always to left of
         * ButtonData.CANCEL_CLOSE, but without a big gap (ButtonBar BUTTON_ORDER_ constants) Alert dlg = new
         * Alert(AlertType.CONFIRMATION, null, new ButtonType("OK", ButtonData.NO), ButtonType.CANCEL);
         * dlg.setTitle("Exit confirmation"); dlg.setHeaderText("Exit WorldEdit?");
         * dlg.getDialogPane().setStyle("-fx-base: #000;");
         * 
         * dlg.showAndWait(); if (dlg.getResult() == ButtonType.CANCEL) quit = false; }
         */

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
}
