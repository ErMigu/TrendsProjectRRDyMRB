package org.example;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Email {
    public String sender;
    public String subject;
    public String text;

    private final Auxiliar auxClass;

    private final DB dataBase;
    private double scoreUser = 1;
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
    boolean debug2 = false;
    double phishingScore = 0;
    boolean fakeDomain = false;

    private final double MAX_PHISHING_SCORE = 0.3;

    public Email(String sender, String subject, String text, Auxiliar aux) {
        this.sender = sender;
        this.subject = subject;
        this.text = text;
        this.auxClass = aux;
        dataBase = new DB();
    }

    public void checkSender() throws SQLException {

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
                        dangerousDomain = 0;
                        reasons += " The email domain is trying to impersonate a known company\n";
                        fakeDomain = true;
                    }
                    notKnownDomain = 0;
                    reasons += " The email domain is not known\n";
                }

                //Insert user
                q = "INSERT INTO Users (email, truth, nemailsSent, nphishingEmails, nWork, nMoney, nAccount, nRandom, lastTopics, isFakeDomain) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?,?)";
                PreparedStatement pstMnt = dataBase.prepareStatement(q);
                pstMnt.setString(1, email);
                pstMnt.setDouble(2, scoreUser);
                pstMnt.setInt(3, emailsSent);
                pstMnt.setInt(4,phishingEmails);
                pstMnt.setInt(5, 0);
                pstMnt.setInt(6,0);
                pstMnt.setInt(7, 0);
                pstMnt.setInt(8,0);
                pstMnt.setString(9, "");
                pstMnt.setBoolean(10,fakeDomain);
                int insertionCompleted  = pstMnt.executeUpdate();
                if(insertionCompleted > 0){
                    if(debug2)
                        System.out.println("Inserted email");
                }
            }else{
                scoreUser = rs.getInt("truth");
                if(debug2)
                    System.out.println("User in database, truth: " + scoreUser);
                fakeDomain = rs.getBoolean("isFakeDomain");
                if(fakeDomain){
                    reasons += " The email domain is trying to impersonate a known company\n";
                }
            }

            dataBase.disconnect();
        }catch(SQLException e){
            e.printStackTrace();
        }

        if(debug) {
            System.out.println("======Cosas del usuario al mirar correo=======");
            System.out.println("Score usuario: " + scoreUser);
            System.out.println("Score domain: " + notKnownDomain);
            System.out.println("Score dangerousDomain: " + dangerousDomain);
            System.out.println("================================");
        }


    }

    public void checkSubject(){
        String[] words = subject.split(" ");
        HashMap<String,String> subjectTopics = auxClass.getSubjectTopics();
        boolean changedCompanyDomain = false;
        boolean changedCompanyDomainAndDomain = false;
        boolean changedCautionWord = false;
        try{
            dataBase.connect();

            for (String word : words) {
                if(debug2)
                    System.out.println(word);
                word=word.toLowerCase();
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
                                    if(companyNameNoCompanyDomain < 0){
                                        companyNameNoCompanyDomain = 0;
                                    }
                                    changedCompanyDomainAndDomain = true;
                                    break;
                                }else{
                                    //If the subject contains amazon but comes from nvidia -> no trust
                                    if(debug2) {
                                        System.out.println("Contains company but comes from other official mail");
                                    }
                                    companyNameNoCompanyDomain++;
                                    if(companyNameNoCompanyDomain > 1){
                                        companyNameNoCompanyDomain = 1;
                                    }
                                    changedCompanyDomainAndDomain = true;
                                }

                            }else{
                                if(debug2) {
                                    System.out.println("Contains company but doesn't come from official mail");
                                }
                                companyName++;
                                if(companyName > 1){
                                    companyName = 1;
                                }
                                changedCompanyDomain = true;

                            }
                        }catch(SQLException e){
                            e.printStackTrace();
                        }
                    }else{
                        if(debug2) {
                            System.out.println("Contains keyword");
                        }
                        cautionWordDetected++;
                        if(cautionWordDetected > 1){
                            cautionWordDetected = 1;
                        }
                        changedCautionWord = true;
                    }

                }
            }
            scoreSubject = 0.5 * cautionWordDetected + 0.1 * companyName + 0.4 * companyNameNoCompanyDomain;
            if(scoreSubject < 0){
                scoreSubject = 0;
            }else if(scoreSubject > 1){
                scoreSubject = 1;
            }
            if(companyName >= 1 && changedCompanyDomain){
                reasons += " Contains company but doesn't come from official mail\n";
            }
            if(companyNameNoCompanyDomain >= 1 && changedCompanyDomainAndDomain){
                reasons += " Contains company name in the subject but comes from other company email\n";
            }
            if(cautionWordDetected >= 1 && changedCautionWord){
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
        boolean scam;
        int phishingEmails = 0;
        double truth = 1;
        if(fakeDomain){
            System.out.println("=*=*=*=*=*=*=*=*ES PHISHING*=*=*=*=*=*=*=");
            scam = true;
            phishingScore = 1;
        }else {
            phishingScore = (0.35 * scoreSubject + 0.65 * scoreText) / scoreUser;
            if (phishingScore >= MAX_PHISHING_SCORE) {
                System.out.println("=*=*=*=*=*=*=*=*ES PHISHING*=*=*=*=*=*=*=");
                scam = true;
            } else {
                System.out.println("#=#=#=#=#NO ES PHISHING#=#=#=#=#");
                scam = false;
            }
        }
        try {
            dataBase.connect();
            String getInformationAboutUser = "SELECT * FROM Users WHERE email = ?";
            PreparedStatement statementInfoUser = dataBase.prepareStatement(getInformationAboutUser);
            statementInfoUser.setString(1,sender);
            ResultSet informationAboutUser = statementInfoUser.executeQuery();
            if(informationAboutUser.next()){
                phishingEmails = informationAboutUser.getInt("nPhishingEmails");
                truth = informationAboutUser.getDouble("truth");
            }
            PreparedStatement statementUpdateUser;
            if (scam) {
                truth = truth - 0.5 * phishingEmails;
                if(truth < 1){
                    truth = 1;
                }
                String updatePhishingUser = "UPDATE Users SET truth = ?, nEmailsSent = nEmailsSent+1, nPhishingEmails = nPhishingEmails+1 WHERE email = ?";
                statementUpdateUser = dataBase.prepareStatement(updatePhishingUser);
            } else {
                truth = truth + 0.5;
                if(truth > 10){
                    truth = 10;
                }
                String updateGoodUser = "UPDATE Users SET truth = ?, nEmailsSent = nEmailsSent+1 WHERE email = ?";
                statementUpdateUser = dataBase.prepareStatement(updateGoodUser);
            }
            statementUpdateUser.setDouble(1,truth);
            statementUpdateUser.setString(2,sender);
            int updateCompleted = statementUpdateUser.executeUpdate();
            if(updateCompleted > 0 && debug2){
                System.out.println("Update completed");
            }

        }catch(SQLException exception){
            exception.printStackTrace();
        }
        if (debug) {
            System.out.println("=======Cosas del phishing final========");
            System.out.println("Score User " + scoreUser);
            System.out.println("Score Subject " + scoreSubject);
            System.out.println("Score Text " + scoreText);
            System.out.println("Phishing value: " + phishingScore);
            System.out.println("====================");
            System.out.println(reasons);
        }
    }

    public void processAndCheck() {
        HashMap<String, String> banking_phrases = auxClass.getBankingPhrases();
        HashMap<String, String> account_phrases = auxClass.getAccountsPhrases();
        HashMap<String, String> working_phrases = auxClass.getWorkPhrases();
        HashMap<String, String> confusionPhrases = auxClass.getConfusionPhrases();
        HashMap<String, String> excitementPhrases = auxClass.getExcitementPhrases();
        HashMap<String, String> fearPhrases = auxClass.getFearPhrases();
        HashMap<String, String> interestPhrases = auxClass.getUrgencyPhrases();
        HashMap<String, String> urgencyPhrases = auxClass.getInterestPhrases();
        String rutaModelo = "Assets/en-pos-maxent.bin";
        int numBanking = 0;
        int numAccount = 0;
        int numWorking = 0;
        String topic;
        boolean reliable = false;

        try (InputStream modelIn = new FileInputStream(rutaModelo)) {
            // Cargar el modelo POS pre-entrenado
            POSModel posModel = new POSModel(modelIn);
            POSTaggerME posTagger = new POSTaggerME(posModel);

            // Tokenizar la frase
            SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
            String[] tokens = tokenizer.tokenize(text.toLowerCase());
            ArrayList<String> potentialTokens = new ArrayList<>();

            // Etiquetar las palabras con sus categorías gramaticales
            String[] tags = posTagger.tag(tokens);

            // Filtrar palabras con significado semántico
            for (int i = 0; i < tokens.length; i++) {
                if (tags[i].startsWith("N") || // Sustantivos
                        tags[i].startsWith("V") || // Verbos
                        tags[i].startsWith("J") || // Adjetivos
                        tags[i].startsWith("R")) { // Adverbios
                    potentialTokens.add(tokens[i]);
                }
            }

            // Verificar combinaciones de 2 palabras para determinar el tema
            for (String potentialToken : potentialTokens) {
                if (banking_phrases.containsValue(potentialToken)) {
                    numBanking++;
                } else if (working_phrases.containsValue(potentialToken)) {
                    numWorking++;
                } else if (account_phrases.containsValue(potentialToken)) {
                    numAccount++;
                }
            }

            // Determinar el tema basado en las ocurrencias
            if (numBanking > numWorking && numBanking > numAccount) {
                topic = "A";
            } else if (numWorking > numBanking && numWorking > numAccount) {
                topic = "W";
            } else if (numAccount > numBanking && numAccount > numWorking) {
                topic = "M";
            } else {
                topic = "R"; // Random en caso de empate
            }

            // Consultar los valores actuales de nWork, nMoney, nAccount y nRandom del usuario
            String query = "SELECT nWork, nMoney, nAccount, nRandom FROM Users WHERE email = ?";
            PreparedStatement pstmt = dataBase.prepareStatement(query);
            pstmt.setString(1, sender);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int nWork = rs.getInt("nWork");
                int nMoney = rs.getInt("nMoney");
                int nAccount = rs.getInt("nAccount");
                int nRandom = rs.getInt("nRandom");

                // Determinar el mayor valor actual de nX
                int maxCurrentValue = Math.max(Math.max(nWork, nMoney), Math.max(nAccount, nRandom));

                // Incrementar el valor correspondiente basado en el topic
                int newValue = 0;
                switch (topic) {
                    case "A":
                        newValue = nMoney + 1;
                        break;
                    case "W":
                        newValue = nWork + 1;
                        break;
                    case "M":
                        newValue = nAccount + 1;
                        break;
                    case "R":
                        newValue = nRandom + 1;
                        break;
                }

                // Comparar el nuevo valor con el mayor valor actual
                if (Math.abs(newValue - maxCurrentValue) > 1) {
                    changeTopic = -1;
                    reasons += " The text changes the topic of the last emails\n";
                }

                // Actualizar el valor en la base de datos
                String updateQuery = "UPDATE Users SET ";
                switch (topic) {
                    case "A":
                        updateQuery += "nMoney = ? ";
                        break;
                    case "W":
                        updateQuery += "nWork = ? ";
                        break;
                    case "M":
                        updateQuery += "nAccount = ? ";
                        break;
                    case "R":
                        updateQuery += "nRandom = ? ";
                        break;
                }
                updateQuery += "WHERE email = ?";
                PreparedStatement updateStmt = dataBase.prepareStatement(updateQuery);
                updateStmt.setInt(1, newValue);
                updateStmt.setString(2, sender);
                updateStmt.executeUpdate();

            }

            // Procesar los sentimientos
            double score = processFeelingsAux(potentialTokens,text.toLowerCase(), tokenizer, confusionPhrases, excitementPhrases, fearPhrases, interestPhrases, urgencyPhrases);
            if(score > 1){
                feelings = 1;
                reasons += " The text implies feelings associated with phishing\n";
            }else if(score <= 0){
                feelings = 0;
            }else{
                feelings = score;
                reasons += " The text implies feelings associated with phishing\n";
            }

            // Verificar errores ortográficos
            String q = "SELECT truth FROM Users WHERE email = ?";
            PreparedStatement pStmnt = dataBase.prepareStatement(q);
            pStmnt.setString(1, sender);
            ResultSet rsTruth = pStmnt.executeQuery();
            if (rsTruth.next()) {
                int truth = rsTruth.getInt("truth");
                for (String word : tokens) {
                    if (!auxClass.spellChecker.isCorrect(word)) {
                        System.out.println(word);
                        misspelling = 1;
                        reasons += " The text contains misspelling\n";
                        break;
                    }
                }
                reliable = truth >= 7;
            }

            if(debug){
                System.out.println("Determinado topic: " + changeTopic);
                System.out.println("Scorefeelings: " + feelings);
                System.out.println("Scoremisspelling: " + misspelling);
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }

        if(reliable){
            scoreText=feelings*0.6+changeTopic*0.4;
        }else{
            scoreText=misspelling*0.7+feelings*0.15+changeTopic*0.15;
        }
    }


    public double processFeelingsAux(ArrayList<String> potentialTokens, String frase, SimpleTokenizer tokenizer,
                                     HashMap<String, String> confusionPhrases, HashMap<String, String> excitementPhrases,
                                     HashMap<String, String> fearPhrases, HashMap<String, String> interestPhrases,
                                     HashMap<String, String> urgencyPhrases) {
        int numCoincidences = 0;

        // Tokenizar la frase
        String[] tokens = tokenizer.tokenize(frase);

        // Verificar combinaciones de 2 palabras
        for (int i = 0; i < potentialTokens.size() - 1; i++) {
            String combo2 = potentialTokens.get(i) + " " + potentialTokens.get(i + 1);
            if (checkHashMaps(combo2, confusionPhrases, excitementPhrases, fearPhrases, interestPhrases, urgencyPhrases)) {
                numCoincidences++;
            }
        }

        // Verificar combinaciones de 3 palabras
        for (int i = 0; i < potentialTokens.size() - 2; i++) {
            String combo3 = potentialTokens.get(i) + " " + potentialTokens.get(i + 1) + " " + potentialTokens.get(i + 2);
            if (checkHashMaps(combo3, confusionPhrases, excitementPhrases, fearPhrases, interestPhrases, urgencyPhrases)) {
                numCoincidences++;
            }
        }

        if(debug){
            System.out.println("Num coincidences for feelings: " + numCoincidences);
        }

        return (double) (numCoincidences ) / (tokens.length * 0.01)  ;
    }


    public boolean checkHashMaps(String phrase, HashMap<String, String>... hashMaps) {
        for (HashMap<String, String> hashMap : hashMaps) {
            if (hashMap.containsValue(phrase)) {
                return true;
            }
        }
        return false;
    }

    public String getMessage(){
        String phishing;
        if(phishingScore >= MAX_PHISHING_SCORE)
            phishing = "The email contains phishing due to this reasons:\n" + reasons;
        else{
            if(reasons.isEmpty()){
                phishing = "The email doesn't contains phishing";
            }else{
                phishing = "The email doesn't contains phishing but there are some warnings:\n" + reasons;
            }
        }
        return phishing;
    }
}


