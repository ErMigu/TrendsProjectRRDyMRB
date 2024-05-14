package org.example;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.sql.SQLException;

public class GUI {
    private static int width = 400;
    private static int height = 300;
    public static void main(String[] args) {
        JFrame frame = new JFrame("Enviar Email");
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Crear campos de texto
        JTextField emailField = new JTextField(20);
        JTextField subjectField = new JTextField(20);
        JTextField textField = new JTextField(20);

        // Crear botón
        JButton enviarButton = new JButton("Enviar");
        Auxiliar aux = new Auxiliar();

        // Acción del botón
        enviarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Obtener el texto de los campos
                String email = emailField.getText();
                String subject = subjectField.getText();
                String text = textField.getText();

                // Crear y enviar el email
                Email emailObj = new Email(email, subject, text,aux);
                try {
                    emailObj.checkSender();
                    emailObj.checkSubject();
                }catch (SQLException exception){
                    exception.printStackTrace();
                }
                // Limpiar los campos después de enviar el email
                emailField.setText("");
                subjectField.setText("");
                textField.setText("");
            }
        });

        // Crear panel y agregar componentes
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2, 0, 40)); // GridLayout con dos columnas y espacio vertical de 10

        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Asunto:"));
        panel.add(subjectField);
        panel.add(new JLabel("Texto:"));
        panel.add(textField);
        panel.add(new JLabel()); // Espacio en blanco para el botón
        panel.add(enviarButton);

        // Agregar panel al marco
        frame.getContentPane().add(panel);
        // Mostrar la ventana
        frame.setVisible(true);

    }
}
