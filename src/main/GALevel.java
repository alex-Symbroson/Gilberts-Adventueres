package main;

import java.util.HashMap;
import java.util.Map;

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
