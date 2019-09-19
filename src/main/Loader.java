package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.image.Image;

public class Loader
{
    static Image loadImage(Path path) throws IOException
    {
        Image img = new Image(Files.newInputStream(path), 768.0, 432.0, true, false);

        if (img.errorProperty().get()) throw new IOException(img.getException());

        return img;
    }
}
