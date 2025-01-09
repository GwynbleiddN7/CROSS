package Utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileCreator {
    public static boolean WriteToFile(String path, Object object)
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String text = gson.toJson(object);
        File file = new File(path);
        try{
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
