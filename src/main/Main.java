package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import script.ScriptEvaluator;
import script.Token;
import script.Token.TokenType;

public class Main extends Application
{
    private Controller controller;
    protected Preferences prefs;

    private GALevel current_level;
    private InvalidationListener level_listener;
    private Map<String, GALevel> levels = new HashMap<>();

    protected Loader loader;
    protected ScriptEvaluator eval;

    protected GALevel get_level(String level_name)
    {
        if (!levels.containsKey(level_name)) try
        {
            levels.put(level_name,
                    GALevel.load(level_name, loader, controller.game.getWidth(), controller.game.getHeight()));
        } catch (IOException e)
        {
            e.printStackTrace();
            controller.setStatus("Error " + e.getMessage());
        }

        return levels.get(level_name);
    }

    private static final Background NULL_BG = new Background(new BackgroundFill(Color.BLACK, null, null));

    private void setBackground(Image bg_img)
    {
        if (bg_img == null)
        {
            controller.game.setBackground(NULL_BG);
            return;
        }

        BackgroundImage bg = new BackgroundImage(bg_img, null, null, null,
                new BackgroundSize(1.0, 1.0, true, true, true, true));
        // game_pane.setMaxWidth(bg_img.getWidth());
        // game_pane.setMaxHeight(bg_img.getHeight());
        controller.game.setBackground(new Background(bg));

        double scale = controller.game.getWidth() / bg_img.getWidth();
        if (scale != controller.game.getHeight() / bg_img.getHeight())
            throw new IllegalArgumentException("Background for " + current_level.name + " has wrong aspect ratio");

        controller.game.getChildren().forEach(n ->
        {
            GAObject o = (GAObject) n;
            o.setLayoutX(o.getFitWidth() * (scale - 1) / 2);
            o.setLayoutY(o.getFitHeight() * (scale - 1) / 2);
            o.setScaleX(scale);
            o.setScaleY(scale);
        });
    }

    protected void enter(String new_level)
    {
        if (current_level != null)
        {
            current_level.stateProperty().removeListener(level_listener);
            controller.game.getChildren().clear();
        }

        current_level = get_level(new_level);
        if (current_level == null) return;

        controller.game.getChildren().addAll(current_level.objects.values());
        setBackground(current_level.getBackground());

        level_listener = e -> setBackground(current_level.getBackground());
        current_level.stateProperty().addListener(level_listener);

        eval.setCurrentLevel(current_level);
    }

    void saveGame(File file)
    {
        JSONObject data = new JSONObject();
        // TODO remember to save items
        data.put("_", current_level.name);
        for (GALevel level : levels.values())
        {
            JSONObject lvl_data = new JSONObject();
            if (level.getState() != 0) lvl_data.put("state", level.getState());
            for (GAObject obj : level.objects.values())
                if (obj.isVisible() != obj.start_visible || obj.getState() != 0)
                    lvl_data.put(obj.name, List.of(obj.isVisible() ? -1 : 0, obj.getState()));
            if (!lvl_data.isEmpty()) data.put(level.name, lvl_data);
        }
        try
        {
            Files.writeString(file.toPath(), data.toString(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            controller.setStatus("Saved");
        } catch (IOException e)
        {
            e.printStackTrace();
            controller.setStatus("Could not save file");
        }
    }

    void loadGame(File file)
    {
        JSONObject data;
        try
        {
            data = new JSONObject(Files.readString(file.toPath()));
        } catch (IOException e)
        {
            e.printStackTrace();
            controller.setStatus("Could not load file");
            return;
        }

        setBackground(null);

        levels.clear();
        // TODO remember to load items
        Set<String> key_set = new HashSet<>(data.keySet());
        key_set.remove("_");
        for (String key : key_set)
        {
            GALevel level = get_level(key);
            JSONObject lvl_data = data.getJSONObject(key);
            if (lvl_data.has("state")) level.setState(lvl_data.getInt("state"));

            Set<String> obj_names = lvl_data.keySet();
            obj_names.remove("state");
            for (String obj : obj_names)
            {
                JSONArray ia = lvl_data.getJSONArray(obj);
                GAObject ob = level.getObject(obj);
                ob.setVisible(ia.getInt(-1) != 0);
                ob.setState(ia.getInt(1));
            }
        }

        enter(data.getString("_"));
    }

    @Override
    public void init() throws Exception
    {
        prefs = Preferences.userRoot().node("gilberts_adv");

        // set value if absent
        BiConsumer<String, String> put_def = (k, v) ->
        {
            if (prefs.get(k, v).equals(v)) prefs.put(k, v);
        };
        put_def.accept("Show_Exit_Dialogb", "true");
        put_def.accept("Preload_Levelsb", "false");
        put_def.accept("Saves_Dirf", System.getProperty("user.home"));

        Platform.setImplicitExit(true);
    }

    private void preload()
    {
        Queue<String> to_load = new LinkedList<>();
        to_load.add("start");

        while (!to_load.isEmpty())
        {
            GALevel level = get_level(to_load.poll());
            level.objects.values().stream().flatMap(obj -> Arrays.stream(obj.scripts)).forEach(script ->
            {
                for (int ix = 0; ix < script.size(); ++ix)
                {
                    Token t = script.get(ix), t1 = ix >= 1 ? script.get(ix - 1) : Token.EOF,
                            t2 = ix >= 2 ? script.get(ix - 2) : Token.EOF;
                    // in var: level identifiers are followed by period and preceded by non-period
                    // in built-in: level identifiers are arguments of warp()
                    if (t.getType() == TokenType.IDENTIFIER
                            && (t1.getType() != TokenType.PERIOD && script.get(ix + 1).getType() == TokenType.PERIOD
                                    || t2.getType() == TokenType.BUILTIN_FUNC && "warp".equals(t2.getValue())))
                    {
                        String next = (String) t.getValue();
                        if (!"_".equals(next) && !levels.containsKey(next)) to_load.add(next);
                    }
                }
            });
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        FXMLLoader fxml_loader = new FXMLLoader(getClass().getResource("main.fxml"));

        controller = new Controller(this);
        /*
         * eval.setWriteText(System.out::println); int result = load_script(s).eval(null); // TODO here be the normal
         * text function eval.setWriteText(System.out::println); return result;
         */
        controller.setStage(primaryStage);
        fxml_loader.setController(controller);
        Parent root = fxml_loader.load();
        root.getStylesheets().add(getClass().getResource("main.css").toString());

        eval = new ScriptEvaluator(this::get_level, this::enter, System.out::println);
        loader = new Loader(getClass().getResource("/level/"), getClass().getResource("/img/"), eval);

        if (prefs.getBoolean("Preload_Levelsb", false)) preload();
        
        primaryStage.getIcons().add(loader.loadImage("title"));

        primaryStage.setTitle("Gilbert's Adventüres");
        setUserAgentStylesheet(STYLESHEET_MODENA);
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        controller.game.requestFocus();

        enter("start");
    }

    protected void restart(Stage primaryStage)
    {
        setBackground(null);
        primaryStage.close();
        this.stop();
        levels.clear();

        try
        {
            this.start(primaryStage);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void stop()
    {
        try
        {
            prefs.flush();
        } catch (BackingStoreException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    public static boolean isValidIdentifier(String s)
    {
        if (s == null || s.isEmpty() || s.equals("_") || Arrays.binarySearch(TokenType.reserved, s) >= 0) return false;

        if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;
        for (char c : s.toCharArray())
            if (!Character.isJavaIdentifierPart(c)) return false;
        return true;
    }
}