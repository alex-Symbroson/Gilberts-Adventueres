package main;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
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
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import script.Script;
import script.ScriptEvaluator;
import script.ScriptException;
import script.ScriptLexer;
import script.Token;
import script.Token.TokenType;

public class Main extends Application
{
    // debug logger
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private Controller controller;
    private Pane game_pane;
    private Preferences prefs;

    private GALevel current_level;
    private InvalidationListener level_listener;
    private Map<String, GALevel> levels = new HashMap<>();

    private URL res_levels, res_img;
    private ScriptLexer lexer;
    private ScriptEvaluator eval;

    protected Script load_script(String script_string)
    {
        lexer.yyreset(new StringReader(script_string));

        Script script = new Script(eval);
        try
        {
            Token t;
            do
            {
                script.add(t = lexer.yylex());
            } while (t.getType() != TokenType.EOF);
        } catch (IOException e)
        {
            e.printStackTrace();
            script.add(Token.EOF);
        }

        logger.info(script.toString());

        if (!script.testValid())
        {
            controller.setStatus("Script Error");
            throw new ScriptException("Script lexed from \"" + script_string + "\" not valid");
        }

        return script;
    }

    protected GAObject load_object(JSONObject json, String level)
    {
        if (!json.has("name") || !json.has("x") || !json.has("y") || !json.has("script")) return null;

        String name = json.getString("name");
        boolean active = json.has("active") ? json.getBoolean("active") : true;
        int state_count = json.has("states") ? json.getInt("states") : 1;

        List<String> sprite_list = new ArrayList<>();
        if (json.has("sprite"))
        {
            Object sp = json.get("sprite");
            if (sp instanceof JSONArray)
                for (Object o : (JSONArray) sp)
                    sprite_list.add((String) o);
            else if (sp instanceof String)
                sprite_list.add((String) sp);
            else
                return null;
            if (sprite_list.size() != state_count) return null;
        } else if (state_count > 1)
            for (int i = 1; i <= state_count; ++i)
                sprite_list.add(level + "." + name + "_" + i);
        else
            sprite_list.add(level + "." + name);

        List<Image> sprite_list_img = new ArrayList<>();
        for (String file : sprite_list)
            try
            {
                sprite_list_img.add(Loader.loadSprite(new URL(res_img, file + ".png")));
            } catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }

        List<Script> script_list = new ArrayList<>();
        Object script_entry = json.get("script");
        if (script_entry instanceof JSONArray)
        {
            for (Object o : (JSONArray) script_entry)
                script_list.add(load_script((String) o));
            if (script_list.size() < state_count)
            {
                script_list.addAll(Collections.nCopies(state_count - script_list.size(), script_list.get(0)));
                logger.warning(String.format("Amended script list of %s.%s by %d scripts", current_level.name, name,
                        state_count - script_list.size()));
            }
        } else
            script_list.addAll(Collections.nCopies(state_count, load_script((String) script_entry)));

        return new GAObject(name, active, state_count, sprite_list_img.toArray(new Image[0]),
                script_list.toArray(new Script[0]), json.getDouble("x") * game_pane.getWidth(),
                json.getDouble("y") * game_pane.getHeight());
    }

    // error codes:
    // 1 -> level file not found
    // 2 -> object does not have required structure
    // 3 -> background list does not have required structure
    // 4 -> could not read background image(s)
    // 5 -> problem reading objects
    protected int load_level(String level_name)
    {
        if (levels.containsKey(level_name)) return 0;

        URL level_file = null;
        try
        {
            level_file = new URL(res_levels, level_name + ".json");
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        JSONObject json;
        try
        {
            String src = new String(level_file.openStream().readAllBytes());
            json = new JSONObject(src);
        } catch (IOException e)
        {
            e.printStackTrace();
            return 1;
        }
        if (!json.has("name") || !json.has("music") || !json.has("objects")) return 2;
        String name = json.getString("name");
        if (!name.equals(level_name) || !isValidIdentifier(name)) return 2;
        int state_count = json.has("states") ? json.getInt("states") : 1;

        List<String> bg_list = new ArrayList<>();
        if (json.has("background"))
        {
            Object bg = json.get("background");
            if (bg instanceof JSONArray)
                for (Object o : (JSONArray) bg)
                    bg_list.add((String) o);
            else if (bg instanceof String)
                bg_list.add((String) bg);
            else
                return 3;
            if (bg_list.size() != state_count) return 3;
        } else if (state_count > 1)
            for (int i = 1; i <= state_count; ++i)
                bg_list.add(name + "_" + i);
        else
            bg_list.add(name);

        List<Image> bg_list_img = new ArrayList<>();
        for (String bg : bg_list)
            try
            {
                // bg_list_img.add(Loader.loadBackground(res_img.resolve(bg + ".png"), game_pane.getWidth(),
                // game_pane.getHeight()));
                bg_list_img.add(Loader.loadSprite(new URL(res_img, bg + ".png")));
            } catch (IOException e)
            {
                e.printStackTrace();
                return 4;
            }

        GALevel level = new GALevel(name, state_count, bg_list_img.toArray(new Image[0]));
        for (Object entry : json.getJSONArray("objects"))
        {
            if (!(entry instanceof JSONObject)) return 5;
            GAObject object = load_object((JSONObject) entry, name);
            if (object == null) return 6;
            level.addObject(object);
        }

        levels.put(name, level);
        return 0;
    }

    protected GALevel get_level(String level_name)
    {
        int err = load_level(level_name);
        if (err != 0)
        {
            System.err.println("Error " + err);
            controller.setStatus("Error " + err);
        }

        return levels.get(level_name);
    }

    private static final Background NULL_BG = new Background(new BackgroundFill(Color.BLACK, null, null));

    private void setBackground(Image bg_img)
    {
        if (bg_img == null)
        {
            game_pane.setBackground(NULL_BG);
            return;
        }

        BackgroundImage bg = new BackgroundImage(bg_img, null, null, null,
                new BackgroundSize(1.0, 1.0, true, true, true, true));
        // game_pane.setMaxWidth(bg_img.getWidth());
        // game_pane.setMaxHeight(bg_img.getHeight());
        game_pane.setBackground(new Background(bg));

        double scale = game_pane.getWidth() / bg_img.getWidth();
        if (scale != game_pane.getHeight() / bg_img.getHeight())
            throw new IllegalArgumentException("Background for " + current_level.name + " has wrong aspect ratio");

        game_pane.getChildren().forEach(n ->
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
            game_pane.getChildren().clear();
        }

        current_level = get_level(new_level);
        game_pane.getChildren().addAll(current_level.objects.values());
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
                ob.visibleIntProperty().set(ia.getInt(0));
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
        put_def.accept("Saves_Dirf", System.getProperty("user.home"));

        res_levels = getClass().getResource("/level/");
        res_img = getClass().getResource("/img/");

        lexer = new ScriptLexer(null);
        eval = new ScriptEvaluator(this::get_level, this::enter, System.out::println);

        Platform.setImplicitExit(true);
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));

        controller = new Controller(this::saveGame, this::loadGame);
        controller.setStage(primaryStage);
        controller.setPrefs(prefs);
        loader.setController(controller);
        Parent root = loader.load();
        root.getStylesheets().add(getClass().getResource("main.css").toString());

        game_pane = controller.game;

        primaryStage.setTitle("Gilbert's Adventüres");
        setUserAgentStylesheet(STYLESHEET_MODENA);
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        enter("start");
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