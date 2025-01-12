package Server;

import Utility.FileCreator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

//Classe per gestire i login degli utenti
public class LoginHandler {
    private static List<Credentials> credentials; //Lista serializzata delle credenziali degli utenti
    private static final String pathFile = "Data/credentials.json";

    //Funzione per caricare i dati dal file o per inizializzare la lista in caso il file non esista o abbia una sintassi errata
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

    //Funzione sincronizzata chiamata da ogni ClientHandler per verificare il login dell'utente
    public static synchronized int LoginUser(String username, String password)
    {
        for(Credentials cred : credentials)
        {
            if(cred.username.equals(username) && cred.password.equals(password))
            {
                return 100; //Username e password corretti
            }
        }
        return 101; //Username non esistente o password non corretta
    }

    //Funzione sincronizzata chiamata da ogni ClientHandler per eseguire la registrazione di un utente
    public static synchronized int RegisterUser(String username, String password)
    {
        if(password.isEmpty()) return 101; //Controllo se la password è valida
        if(CheckUsername(username)) return 102; //Controllo se l'username già esiste

        credentials.add(new Credentials(username, password)); //Aggiungo le credenziali

        if(FileCreator.WriteToFile(pathFile, credentials)) return 100; //Scrivo le credenziali nel file
        else return 103; //Ritorno errore se non è stato possibile salvare le credenziali
    }

    //Funzione sincronizzata chiamata da ogni ClientHandler per aggiornare le credenziali di un utente
    public static synchronized int UpdateCredentials(String username, String oldPassword, String newPassword)
    {
        if(newPassword.isEmpty()) return 101; //Controllo se la nuova password è valida
        if(newPassword.equals(oldPassword)) return 103; //Controllo se la nuova password è uguale a quella vecchia
        for(Credentials cred : credentials)
        {
            if(cred.username.equals(username) && cred.password.equals(oldPassword))
            {
                cred.password = newPassword; //Aggiorno la password
                FileCreator.WriteToFile(pathFile, credentials); //Aggiorno il file delle credenziali
                return 100;
            }
        }
        return 102; //Se c'è stato un errore ritorno un codice di errore
    }

    //Funzione sincronizzata chiamata da ogni ClientHandler per controllare se l'username inserito esiste
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
