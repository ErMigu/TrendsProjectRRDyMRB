package org.example;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class Auxiliar {
    public HashMap<String,String> subjectTopics;
    DB dataBase;


    public Auxiliar() {
        subjectTopics = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("subjectTopics.txt"))) {
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

    }

    public HashMap<String,String> getSubjectTopics(){
        return subjectTopics;
    }
}
