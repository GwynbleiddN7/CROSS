package Utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileCreator {
    public static boolean WriteToFile(String path, Object object) //Funzione per scrivere un oggetto JSON in un file e crearlo se non esiste
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
