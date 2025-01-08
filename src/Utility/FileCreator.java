package Utility;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileCreator {
    public static boolean WriteToFile(String path, Object object)
    {
        Gson gson = new Gson();
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
