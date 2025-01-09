package Server;

import Utility.FileCreator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LoginHandler {
    private static List<Credentials> credentials;
    private static final String pathFile = "credentials.json";
    public static void LoadData()
    {
        File userFile = new File(pathFile);
        try{
            Gson gson = new Gson();
            TypeToken<ArrayList<Credentials>> gsonType = new TypeToken<>(){};
            credentials = gson.fromJson(new FileReader(userFile), gsonType);

        } catch (Exception _) {
            credentials = new ArrayList<>();
        }
    }

    public static synchronized int LoginUser(String username, String password)
    {
        for(Credentials cred : credentials)
        {
            if(cred.username.equals(username) && cred.password.equals(password))
            {
                return 100;
            }
        }
        return 101;
    }

    public static synchronized int RegisterUser(String username, String password)
    {
        if(password.isEmpty()) return 101;
        if(CheckUsername(username)) return 102;

        credentials.add(new Credentials(username, password));

        if(FileCreator.WriteToFile(pathFile, credentials)) return 100;
        else return 103;
    }

    public static synchronized int UpdateCredentials(String username, String oldPassword, String newPassword)
    {
        if(newPassword.isEmpty()) return 101;
        if(newPassword.equals(oldPassword)) return 103;
        for(Credentials cred : credentials)
        {
            if(cred.username.equals(username) && cred.password.equals(oldPassword))
            {
                cred.password = newPassword;
                FileCreator.WriteToFile(pathFile, credentials);
                return 100;
            }
        }
        return 102;
    }

    public static synchronized boolean CheckUsername(String username)
    {
        for(Credentials cred : credentials)
        {
            if(cred.username.equals(username))
            {
                return true;
            }
        }
        return false;
    }
}