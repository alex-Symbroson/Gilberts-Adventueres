package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.beans.property.IntegerProperty;
import javafx.scene.image.Image;
import util.RangedIntegerProperty;

public class GALevel
{

    final String name;
    final int state_count;
    final Image[] backgrounds;
    // final String music_track;
    final Map<String, GAObject> objects;

    private IntegerProperty state;

    public GALevel(String name, int state_count, Image[] backgrounds)
    {
        this.name = name;
        this.state_count = state_count;
        this.backgrounds = backgrounds;

        objects = new HashMap<>();

        state = new RangedIntegerProperty(0, state_count);
    }

    public static GALevel load(String name, Loader loader, double width, double height) throws IOException
    {
        JSONObject json = loader.loadLevelJson(name);

        if (!json.has("name") || !json.has("music") || !json.has("objects"))
            throw new IOException("Level " + name + " JSON lacks required data");
        String level_name = json.getString("name");
        if (!name.equals(level_name) || !Main.isValidIdentifier(level_name))
            throw new IOException("Level name " + level_name + " isn't valid and equal to " + name);

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
                throw new IOException("Level " + name + " background structure is invalid");

            if (bg_list.size() != state_count) throw new IOException(
                    String.format("Level %s has %d states and %d backgrounds", name, state_count, bg_list.size()));
        } else if (state_count > 1)
            for (int i = 1; i <= state_count; ++i)
                bg_list.add(name + "_" + i);
        else
            bg_list.add(name);

        List<Image> bg_list_img = new ArrayList<>();
        for (String bg : bg_list)
            // bg_list_img.add(Loader.loadBackground(bg, width, height));
            bg_list_img.add(loader.loadImage(bg));

        GALevel level = new GALevel(name, state_count, bg_list_img.toArray(new Image[0]));
        for (Object entry : json.getJSONArray("objects"))
        {
            if (!(entry instanceof JSONObject)) throw new IOException("Object entry in level " + name + " invalid");

            GAObject object = GAObject.load((JSONObject) entry, name, loader, width, height);
            level.addObject(object);
        }
        return level;
    }

    public void addObject(GAObject object)
    {
        objects.put(object.name, object);
    }

    public GAObject getObject(String name)
    {
        return objects.get(name);
    }

    public Image getBackground()
    {
        return backgrounds[state.get()];
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
