package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.beans.property.IntegerProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import script.Script;
import util.RangedIntegerProperty;

public class GAObject extends ImageView
{
    // logger
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    final String name;
    final int state_count;
    final Image[] sprites;
    final Script[] scripts;
    final boolean start_visible;

    private final IntegerProperty state;

    public GAObject(String name, boolean visible, int state_count, Image[] sprites, Script[] scripts, double x,
            double y)
    {
        this.name = name;
        this.state_count = state_count;
        this.sprites = sprites;
        this.scripts = scripts;
        setX(x);
        setY(y);
        setFitWidth(sprites[0].getWidth());
        setFitHeight(sprites[0].getHeight());

        start_visible = visible;

        state = new RangedIntegerProperty(0, state_count);
        setImage(sprites[0]);
        state.addListener((state, old_s, new_s) -> setImage(sprites[new_s.intValue()]));

        setOnMouseClicked(ev -> scripts[getState()].eval(this));
    }

    public static GAObject load(JSONObject json, String level, Loader loader, double width, double height)
            throws IOException
    {
        if (!json.has("name") || !json.has("x") || !json.has("y") || !json.has("script"))
            throw new IOException("Object in " + level + " lacks required data");

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
                sprite_list.addAll(Collections.nCopies(state_count, (String) sp));
            else
                throw new IOException("Object " + level + "." + name + " sprite structure is invalid");

            if (sprite_list.size() != state_count)
                throw new IOException(String.format("Object %s.%s has %d states and %d backgrounds", level, name,
                        state_count, sprite_list.size()));
        } else if (state_count > 1)
            for (int i = 1; i <= state_count; ++i)
                sprite_list.add(level + "." + name + "_" + i);
        else
            sprite_list.add(level + "." + name);

        List<Image> sprite_list_img = new ArrayList<>();
        for (String file : sprite_list)
            sprite_list_img.add(loader.loadSprite(file));

        List<Script> script_list = new ArrayList<>();
        Object script_entry = json.get("script");
        if (script_entry instanceof JSONArray)
        {
            for (Object o : (JSONArray) script_entry)
                script_list.add(loader.loadScript((String) o));
            if (script_list.size() < state_count)
            {
                script_list.addAll(Collections.nCopies(state_count - script_list.size(), script_list.get(0)));
                logger.warning(String.format("Amended script list of %s.%s by %d scripts", level, name,
                        state_count - script_list.size()));
            }
        } else
            script_list.addAll(Collections.nCopies(state_count, loader.loadScript((String) script_entry)));

        return new GAObject(name, active, state_count, sprite_list_img.toArray(new Image[0]),
                script_list.toArray(new Script[0]), json.getDouble("x") * width, json.getDouble("y") * height);
    }

    public int getState()
    {
        return state.get();
    }

    public void setState(int new_state)
    {
        state.set(new_state);
    }

    public IntegerProperty stateProperty()
    {
        return state;
    }
}
