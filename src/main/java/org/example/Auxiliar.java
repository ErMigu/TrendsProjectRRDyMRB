package org.example;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Auxiliar {
    public HashMap<String,String> subjectTopics;
    public HashMap<String,String> confusionPhrases;
    public HashMap<String,String> excitementPhrases;
    public HashMap<String,String> fearPhrases;
    public HashMap<String,String> interestPhrases;
    public HashMap<String,String> urgencyPhrases;
    public HashMap<String,String> bankingPhrases;
    public HashMap<String,String> accountsPhrases;
    public HashMap<String,String> workPhrases;

    DB dataBase;

    public Auxiliar() {
        subjectTopics = new HashMap<>();
        confusionPhrases = new HashMap<>();
        excitementPhrases = new HashMap<>();
        fearPhrases = new HashMap<>();
        interestPhrases = new HashMap<>();
        urgencyPhrases = new HashMap<>();
        bankingPhrases = new HashMap<>();
        accountsPhrases = new HashMap<>();
        workPhrases = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("Dictionaries/subjectTopics.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                subjectTopics.put(line.trim(), "topic");
            }
        } catch (IOException e) {
            System.err.println("Failure while reading the file: " + e.getMessage());
        }
        try{
            dataBase = new DB();
            dataBase.connect();
            String query = "SELECT * FROM companyDomains";
            PreparedStatement preparedQueryCompanies = dataBase.prepareStatement(query);
            ResultSet resultCompanies = preparedQueryCompanies.executeQuery();
            while(resultCompanies.next()){
                subjectTopics.put(resultCompanies.getString("company"),"company");
            }
            dataBase.disconnect();
        }catch(SQLException e){
            System.err.println(e.getMessage());
        }

        try {
            loadPhrases("Dictionaries/excitement_phrases.txt", excitementPhrases);
            loadPhrases("Dictionaries/fear_phrases.txt", fearPhrases);
            loadPhrases("Dictionaries/interest_phrases.txt", interestPhrases);
            loadPhrases("Dictionaries/urgency_phrases.txt", urgencyPhrases);
            loadPhrases("Dictionaries/confusion_phrases.txt", confusionPhrases);
            loadPhrases("Dictionaries/accounts_phrases.txt",accountsPhrases);
            loadPhrases("Dictionaries/working_phrases.txt",workPhrases);
            loadPhrases("Dictionaries/banking_phrases.txt",bankingPhrases);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void loadPhrases(String filePath, HashMap<String, String> phrasesMap) throws IOException {
        // Check if file exists
        if (!Files.exists(Paths.get(filePath))) {
            throw new IOException("File does not exist: " + filePath);
        }

        // Check if file is readable
        if (!Files.isReadable(Paths.get(filePath))) {
            throw new IOException("File is not readable: " + filePath);
        }

        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            phrasesMap.put(line, line);
        }
    }

    public HashMap<String,String> getSubjectTopics(){
        return subjectTopics;
    }

    public HashMap<String, String> getConfusionPhrases() {
        return confusionPhrases;
    }

    public HashMap<String, String> getExcitementPhrases() {
        return excitementPhrases;
    }

    public HashMap<String, String> getFearPhrases() {
        return fearPhrases;
    }

    public HashMap<String, String> getUrgencyPhrases() {
        return urgencyPhrases;
    }

    public HashMap<String, String> getInterestPhrases() {
        return interestPhrases;
    }

    public HashMap<String, String> getBankingPhrases() {
        return bankingPhrases;
    }

    public HashMap<String, String> getAccountsPhrases() {
        return accountsPhrases;
    }

    public HashMap<String, String> getWorkPhrases() {
        return workPhrases;
    }
}
