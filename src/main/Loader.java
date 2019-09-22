package main;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.image.Image;

public class Loader
{
    static Image loadSprite(URL url) throws IOException
    {
        Image img = new Image(url.openStream(), 0, 0, true, false);

        if (img.errorProperty().get()) throw new IOException(img.getException());

        return img;
    }

    @Deprecated
    static Image loadBackground(Path path, double width, double height) throws IOException
    {
        Image img = new Image(Files.newInputStream(path), width, height, true, false);

        if (img.errorProperty().get()) throw new IOException(img.getException());

        return img;
    }

    @Deprecated
    static Image loadSprite(Path path) throws IOException
    {
        Image img = new Image(Files.newInputStream(path), 0, 0, true, false);

        if (img.errorProperty().get()) throw new IOException(img.getException());

        return img;
    }
}
