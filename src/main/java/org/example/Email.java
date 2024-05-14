package org.example;

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
    private Integer scoreUser = 0;
    private Integer scoreSubject = 0;
    private boolean suspiciousSender = false;
    private boolean suspiciousSubject = false;
    private boolean suspiciousContent = false;

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
                System.out.println("User not in database");
                //Creates values for the user
                String email = sender;
                int emailsSent = 1;
                int phishingEmails = 0;

                int domainStart = email.indexOf("@");
                String domain = email.substring(domainStart);
                q = "SELECT * FROM CompanyDomains WHERE domain = ?";
                PreparedStatement preparedstatementDomain = dataBase.prepareStatement(q);
                preparedstatementDomain.setString(1,domain);
                ResultSet resultSetDomain = preparedstatementDomain.executeQuery();
                if(resultSetDomain.next()){
                    System.out.println(resultSetDomain.getString("domain"));
                    //if it comes from a known source we increase the truth
                    System.out.println("Trusty domain");
                    scoreUser++;
                }else{
                    System.out.println("Non trusty domain");
                    //If the company domain is not in the database lets check if it's the type amazon -> amazonn
                    q = "SELECT domain FROM CompanyDomains WHERE levenshtein(CAST(? AS text),CAST(domain AS TEXT)) BETWEEN 1 AND 2";
                    PreparedStatement checkCompanyName = dataBase.prepareStatement(q);
                    checkCompanyName.setString(1,domain);
                    ResultSet resultCompanyName = checkCompanyName.executeQuery();
                    if(resultCompanyName.next()) {
                        System.out.println("Dangerous domain");
                        scoreUser--;
                    }
                }

                //Insert user
                q = "INSERT INTO Users (email, truth, nemailsSent, nphishingEmails) VALUES (?, ?, ?, ?)";
                PreparedStatement pstMnt = dataBase.prepareStatement(q);
                pstMnt.setString(1, email);
                pstMnt.setInt(2, scoreUser);
                pstMnt.setInt(3, emailsSent);
                pstMnt.setInt(4,phishingEmails);
                int insertionCompleted  = pstMnt.executeUpdate();
                if(insertionCompleted > 0){
                    System.out.println("Inserted email");
                }
            }else{
                scoreUser = rs.getInt("truth");
                System.out.println("User in database, truth: " + scoreUser);
            }

            dataBase.disconnect();
        }catch(SQLException e){
            e.printStackTrace();
        }

    }

    public void checkSubject(){
        int score = 0;
        String[] words = subject.split(" ");
        HashMap<String,String> subjectTopics = auxClass.getSubjectTopics();

        try{
            dataBase.connect();

            for (String word : words) {
                System.out.println(word);
                if (subjectTopics.containsKey(word)) {
                    if(subjectTopics.get(word).equals("company")){
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
                                    score = 1;
                                    System.out.println("Contains company and comes from official mail");
                                    break;
                                }else{
                                    //If the subject contains amazon but comes from nvidia -> no trustç
                                    System.out.println("Contains company but comes from other official mail");
                                    score--;
                                }

                            }else{
                                System.out.println("Contains company but doesn't come from official mail");
                                score--;
                            }
                        }catch(SQLException e){
                            e.printStackTrace();
                        }
                    }else{
                        System.out.println("Contains keyword");
                        score--;
                    }

                }
            }
            if(score < 0){
                scoreSubject = -1;
            }else scoreSubject = score;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
