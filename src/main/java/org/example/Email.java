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
    private Integer scoreUser;


    public void checkSender() throws SQLException {
        try {
            dataBase.connect();

            //look for the email in the users database
            String q = "SELECT * FROM Users WHERE \"email\" = ?";
            PreparedStatement pStmnt = dataBase.prepareStatement(q);
            pStmnt.setString(1, sender);
            ResultSet rs = pStmnt.executeQuery();

            if(!rs.next()){
                //Creates values for the user
                String email = sender;
                Integer truth = 0;
                Integer emailsSent = 1;
                Integer phishingEmails = 0;

                int domainStart = email.indexOf("@");
                String domain = email.substring(domainStart);
                q = "SELECT domain FROM CompanyDomains WHERE \"domain\" = ?";
                PreparedStatement preparedstatementDomain = dataBase.prepareStatement(q);
                preparedstatementDomain.setString(1,domain);
                ResultSet resultSetDomain = preparedstatementDomain.executeQuery();
                if(resultSetDomain.next()){
                    //if it comes from a known source we increase the truth
                    scoreUser++;
                }else{
                    //If the company domain is not in the database lets check if it's the type amazon -> amazonn
                    q = "SELECT domain FROM CompanyDomains WHERE similarity(domain, ?) <= 1";
                    PreparedStatement checkCompanyName = dataBase.prepareStatement(q);
                    checkCompanyName.setString(1,domain);
                    ResultSet resultCompanyName = checkCompanyName.executeQuery();
                    if(resultCompanyName.next()) {
                        scoreUser--;
                    }
                }


                //Insert user

                q = "INSERT INTO Users (email, truth, emailsSent, phishingEmails) VALUES (?, ?, ?, ?)";
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
            }

            dataBase.disconnect();
        }catch(SQLException e){
            e.printStackTrace();
        }

    }
}
