package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application
{

    private Controller controller;
    private Pane game_pane;

    private GALevel current_level;
    private InvalidationListener level_listener;
    private Map<String, GALevel> levels = new HashMap<>();

    private Path res_levels, res_img;

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
                sprite_list_img.add(Loader.loadImage(res_img.resolve(file + ".png")));
            } catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }

        return new GAObject(name, active, state_count, sprite_list_img.toArray(new Image[0]),
                json.getDouble("x") * game_pane.getWidth(), json.getDouble("y") * game_pane.getHeight());
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

        Path level_file = res_levels.resolve(level_name + ".json");
        if (!Files.exists(level_file) || !Files.isReadable(level_file)) return 1;

        JSONObject json;
        try
        {
            json = new JSONObject(Files.readString(level_file));
        } catch (IOException e)
        {
            e.printStackTrace();
            return 1;
        }
        if (!json.has("name") || !json.has("music") || !json.has("objects")) return 2;
        String name = json.getString("name");
        if (!name.equals(level_name) || !isValidIdentifier(name)) return 2;
        int state_count = json.getInt("states");

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
                bg_list_img.add(Loader.loadImage(res_img.resolve(bg + ".png")));
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
            if(object == null) return 5;
            level.addObject(object);
        }

        levels.put(name, level);
        return 0;
    }
    
    protected GALevel get_level(String level_name) {
        int err = load_level(level_name);
        if(err != 0) controller.setStatus("Error " + err);
        
        return levels.get(level_name);
    }

    protected void enter(String new_level)
    {
        current_level.stateProperty().removeListener(level_listener);
        game_pane.getChildren().clear();
        
        current_level = levels.get(new_level);
        Background bg = new Background(new BackgroundImage(current_level.getBackground(), null, null, null, null));
        game_pane.setBackground(bg);
        level_listener = e -> game_pane.getBackground().getImages().set(0, new BackgroundImage(current_level.getBackground(), null, null, null, null));
        current_level.stateProperty().addListener(level_listener);
        
        game_pane.getChildren().addAll(current_level.objects.values());
    }

    @Override
    public void init() throws Exception
    {
        res_levels = Paths.get("res/level");
        res_img = Paths.get("res/img");
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));

        Parent root = loader.load();

        controller = (Controller) loader.getController();
        controller.setStage(primaryStage);
        game_pane = controller.game;

        primaryStage.setTitle("Gilbert's Adventüres");
        setUserAgentStylesheet(STYLESHEET_MODENA);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    public static boolean isValidIdentifier(String s)
    {
        if (s == null || s.isEmpty() || s.equals("_")) return false;

        if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;
        for (char c : s.toCharArray())
            if (!Character.isJavaIdentifierPart(c)) return false;
        return true;
    }
}