package org.example;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class Email {
    public Integer score = 0;
    public String sender;
    public String subject;
    public String text;

    private final Auxiliar auxClass;

    private final DB dataBase;
    private double scoreUser = 0;
    private double scoreSubject = 0;
    private double scoreText = 0;
    private double notKnownDomain = 0;
    private double dangerousDomain = 0;
    private double cautionWordDetected = 0;
    private double companyNameNoCompanyDomain = 0;
    private double companyName = 0;
    private double misspelling = 0;
    private double feelings = 0;
    private double changeTopic = 0;
    public String reasons = "";
    boolean debug = true;
    private double saveScoreUser = 0;
    boolean debug2 = false;

    public Email(String sender, String subject, String text, Auxiliar aux) {
        this.sender = sender;
        this.subject = subject;
        this.text = text;
        this.auxClass = aux;
        dataBase = new DB();
    }

    public void checkSender() throws SQLException {
        boolean userFound = false;

        try {
            dataBase.connect();

            //look for the email in the users database
            String q = "SELECT * FROM Users WHERE \"email\" = ?";
            PreparedStatement pStmnt = dataBase.prepareStatement(q);
            pStmnt.setString(1, sender);
            ResultSet rs = pStmnt.executeQuery();

            if(!rs.next()){
                if(debug2)
                    System.out.println("User not in database");
                //Creates values for the user
                String email = sender;
                int emailsSent = 0;
                int phishingEmails = 0;
                double defaultValue = 1;

                int domainStart = email.indexOf("@");
                String domain = email.substring(domainStart);
                q = "SELECT * FROM CompanyDomains WHERE domain = ?";
                PreparedStatement preparedstatementDomain = dataBase.prepareStatement(q);
                preparedstatementDomain.setString(1,domain);
                ResultSet resultSetDomain = preparedstatementDomain.executeQuery();
                if(resultSetDomain.next()){
                    if(debug2) {
                        System.out.println(resultSetDomain.getString("domain"));
                        //if it comes from a known source we increase the truth
                        System.out.println("Trusty domain");
                    }
                    notKnownDomain = 1;
                }else{
                    if(debug2)
                        System.out.println("Non trusty domain");
                    //If the company domain is not in the database lets check if it's the type amazon -> amazonn
                    q = "SELECT domain FROM CompanyDomains WHERE levenshtein(CAST(? AS text),CAST(domain AS TEXT)) BETWEEN 1 AND 2";
                    PreparedStatement checkCompanyName = dataBase.prepareStatement(q);
                    checkCompanyName.setString(1,domain);
                    ResultSet resultCompanyName = checkCompanyName.executeQuery();
                    if(resultCompanyName.next()) {
                        if(debug2)
                            System.out.println("Dangerous domain");
                        dangerousDomain = -1;
                        reasons += " The email domain is trying to impersonate a known company\n";
                    }
                    notKnownDomain = -1;
                    reasons += " The email domain is not known\n";
                }

                //Insert user
                q = "INSERT INTO Users (email, truth, nemailsSent, nphishingEmails) VALUES (?, ?, ?, ?)";
                PreparedStatement pstMnt = dataBase.prepareStatement(q);
                pstMnt.setString(1, email);
                pstMnt.setDouble(2, defaultValue);
                pstMnt.setInt(3, emailsSent);
                pstMnt.setInt(4,phishingEmails);
                int insertionCompleted  = pstMnt.executeUpdate();
                if(insertionCompleted > 0){
                    if(debug2)
                        System.out.println("Inserted email");
                }
            }else{
                scoreUser = rs.getInt("truth");
                saveScoreUser = scoreUser;
                userFound = true;
                if(debug2)
                    System.out.println("User in database, truth: " + scoreUser);
            }

            dataBase.disconnect();
        }catch(SQLException e){
            e.printStackTrace();
        }
        if(scoreUser == 0 && !userFound){
            if(dangerousDomain == -1){
                scoreUser = -2;
            }else {
                scoreUser = Math.min(notKnownDomain,2*dangerousDomain);
            }
            if(debug) {
                System.out.println("======Cosas del usuario al mirar correo=======");
                System.out.println("Score usuario: " + scoreUser);
                System.out.println("Score domain: " + notKnownDomain);
                System.out.println("Score dangerousDomain: " + dangerousDomain);
                System.out.println("================================");
            }
        }
    }

    public void checkSubject(){
        String[] words = subject.split(" ");
        HashMap<String,String> subjectTopics = auxClass.getSubjectTopics();

        try{
            dataBase.connect();

            for (String word : words) {
                if(debug2)
                    System.out.println(word);
                if (subjectTopics.containsKey(word)) {
                    if(subjectTopics.get(word).equalsIgnoreCase("company")){
                        if(debug2)
                            System.out.println("Contains company");
                        int domainStart = sender.indexOf("@");
                        String domain = sender.substring(domainStart);
                        String domainQuery = "SELECT * FROM companyDomains WHERE domain = ?";
                        try {
                            PreparedStatement searchDomain = dataBase.prepareStatement(domainQuery);
                            searchDomain.setString(1,domain);
                            ResultSet resultSearchDomain = searchDomain.executeQuery();
                            if(resultSearchDomain.next()){
                                String name = resultSearchDomain.getString("domain");
                                int dotPosition = name.lastIndexOf(".");
                                String cleanName = name.substring(1,dotPosition);
                                //If the subject contains amazon and comes from @amazon.com -> trust
                                if(word.equalsIgnoreCase(cleanName)){
                                    if(debug2)
                                        System.out.println("Contains company and comes from official mail");
                                    companyNameNoCompanyDomain++;
                                    companyName = 1;
                                    if(companyNameNoCompanyDomain > 1){
                                        companyNameNoCompanyDomain = 1;
                                    }
                                    break;
                                }else{
                                    //If the subject contains amazon but comes from nvidia -> no trust
                                    if(debug2) {
                                        System.out.println("Contains company but comes from other official mail");
                                    }
                                    companyNameNoCompanyDomain--;
                                    if(companyNameNoCompanyDomain < -1){
                                        companyNameNoCompanyDomain = -1;
                                    }
                                }

                            }else{
                                if(debug2) {
                                    System.out.println("Contains company but doesn't come from official mail");
                                }
                                companyName--;
                                if(companyName < -1){
                                    companyName = -1;
                                }

                            }
                        }catch(SQLException e){
                            e.printStackTrace();
                        }
                    }else{
                        if(debug2) {
                            System.out.println("Contains keyword");
                        }
                        cautionWordDetected -= 1;
                        if(cautionWordDetected < -1){
                            cautionWordDetected = -1;
                        }
                    }

                }
            }
            scoreSubject = 0.2 * cautionWordDetected + 0.1 * companyName + 0.7 * companyNameNoCompanyDomain;
            if(scoreSubject < -1){
                scoreSubject = -1;
            }else if(scoreSubject > 1){
                scoreSubject = 1;
            }
            if(companyName <= -1){
                reasons += " Contains company but doesn't come from official mail\n";
            }
            if(companyNameNoCompanyDomain <= -1){
                reasons += " Contains company name in the subject but comes from other company email\n";
            }
            if(cautionWordDetected <= -1){
                reasons += " Contains words related to most phishing cases in the subject\n";
            }
            if(debug){
                System.out.println("=====Cosas del asunto=====");
                System.out.println("Company name: " + companyName);
                System.out.println("Comany name and no company domain: " + companyNameNoCompanyDomain);
                System.out.println("Caution word detected: " + cautionWordDetected);
                System.out.println("Final score: " + scoreSubject);
                System.out.println("================================");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkValuesPhishing(){
        double phishing = 0;
        if(scoreUser == -2){
            phishing = -1;
            scoreUser = 100;
        }else {
            phishing = 0.35 * scoreSubject;
        }
        System.out.println("=======Cosas del phishing final========");
        System.out.println("Score User " + scoreUser);
        System.out.println("Score Subject " + scoreSubject);
        System.out.println("Score Text " + scoreText);
        System.out.println("====================");
        System.out.println(reasons);
        System.out.println("Phishing value: " + phishing);

        if(scoreUser < 0){
            scoreUser = 0.1;
        }

        try{
            dataBase.connect();
            double amount = 0;
            double score = phishing * scoreUser;
            System.out.println("System decision phishing and user: " + score);
            if(score < 0){
                System.out.println("Entra en phishing");
                amount = 0.5;
                int phishingEmails = 1;
                String query = "SELECT nPhishingEmails FROM Users WHERE email = ?";
                PreparedStatement phisingQuery = dataBase.prepareStatement(query);
                phisingQuery.setString(1,sender);
                ResultSet emailNumberQuery = phisingQuery.executeQuery();
                if(emailNumberQuery.next()){
                    phishingEmails = emailNumberQuery.getInt("nPhishingEmails");
                }
                if(scoreUser >= 11 || scoreUser == 0.1){
                    scoreUser = 0;
                }
                double scoreUserFinal = scoreUser - amount*phishingEmails;
                if(scoreUserFinal < 0){
                    scoreUserFinal = 0;
                }else if(scoreUserFinal > 10){
                    scoreUserFinal = 10;
                }
                String updateUser = "UPDATE users SET  truth = ?, nPhishingEmails = nPhishingEmails + 1, nEmailsSent = nEmailsSent + 1 WHERE email = ?";
                PreparedStatement updateUserStatement = dataBase.prepareStatement(updateUser);
                updateUserStatement.setDouble(1,scoreUserFinal);
                updateUserStatement.setString(2,sender);
                int checkUpdate = updateUserStatement.executeUpdate();
                if(checkUpdate > 0 && debug){
                    System.out.println("Update completed");
                }
            }else if(score >= 0){
                System.out.println("Entra en no phishing");
                amount = 0.5;
                int sentEmails = 0;
                String query = "SELECT nEmailsSent FROM Users WHERE email = ?";
                PreparedStatement emailsQuery = dataBase.prepareStatement(query);
                emailsQuery.setString(1,sender);
                ResultSet emailNumberQuery = emailsQuery.executeQuery();
                if(emailNumberQuery.next()){
                    sentEmails = emailNumberQuery.getInt("nEmailsSent") + 1;
                }
                int phishingEmails = 1;
                String query2 = "SELECT nPhishingEmails FROM Users WHERE email = ?";
                PreparedStatement phisingQuery = dataBase.prepareStatement(query);
                phisingQuery.setString(1,sender);
                ResultSet emailNumberQuery2 = phisingQuery.executeQuery();
                if(emailNumberQuery2.next()){
                    phishingEmails = emailNumberQuery.getInt("nPhishingEmails");
                }
                double scoreUserFinal = scoreUser + amount*(sentEmails-phishingEmails);
                if(scoreUserFinal < 0){
                    scoreUserFinal = 0;
                }else if(scoreUserFinal > 10){
                    scoreUserFinal = 10;
                }
                String updateUser = "UPDATE users SET  truth = ?, nEmailsSent = nEmailsSent + 1 WHERE email = ?";
                PreparedStatement updateUserStatement = dataBase.prepareStatement(updateUser);
                updateUserStatement.setDouble(1,scoreUserFinal);
                updateUserStatement.setString(2,sender);
                int checkUpdate = updateUserStatement.executeUpdate();
                if(checkUpdate > 0 && debug){
                    System.out.println("Update completed");
                }
            }
            dataBase.disconnect();
        }catch (SQLException exception){
            exception.printStackTrace();
        }
    }

    public void checkFeelings() {
        HashMap<String, String> confusionPhrases = auxClass.getConfusionPhrases();
        HashMap<String, String> excitementPhrases = auxClass.getExcitementPhrases();
        HashMap<String, String> fearPhrases = auxClass.getFearPhrases();
        HashMap<String, String> interestPhrases = auxClass.getUrgencyPhrases();
        HashMap<String, String> urgencyPhrases = auxClass.getInterestPhrases();


        // Ruta al modelo POS pre-entrenado
        String rutaModelo = "/home/kelokeisik/Escritorio/Uni/Trends/ProjectRRDyMRBTrends/project/Assets/en-pos-maxent.bin";

        // Frase de ejemplo
        String frase = text;

        try (InputStream modelIn = new FileInputStream(rutaModelo)) {
            // Cargar el modelo POS pre-entrenado
            POSModel posModel = new POSModel(modelIn);
            POSTaggerME posTagger = new POSTaggerME(posModel);

            // Tokenizar la frase
            SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
            String[] tokens = tokenizer.tokenize(frase);

            // Etiquetar las palabras con sus categorías gramaticales
            String[] tags = posTagger.tag(tokens);

            // Filtrar palabras con significado semántico
            for (int i = 0; i < tokens.length; i++) {
                if (tags[i].startsWith("N") || // Sustantivos
                        tags[i].startsWith("V") || // Verbos
                        tags[i].startsWith("J") || // Adjetivos
                        tags[i].startsWith("R")) { // Adverbios
                    System.out.println(tokens[i]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


