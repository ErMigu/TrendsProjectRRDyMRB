package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Email {
    public Integer score = 0;
    public String sender = "";
    public String subject;
    public String text;

    private DB dataBase;
    private Integer scoreUser = 0;

    public Email(String sender, String subject, String text) {
        this.sender = sender;
        this.subject = subject;
        this.text = text;
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
                Integer emailsSent = 1;
                Integer phishingEmails = 0;

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
                    q = "SELECT domain FROM CompanyDomains WHERE similarity(domain, ?) BETWEEN 1 AND 2";
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
}
