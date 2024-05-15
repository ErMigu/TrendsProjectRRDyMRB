package org.example;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
///home/kelokeisik/Escritorio/Uni/Trends/ProjectRRDyMRBTrends/project

public class Main {

    private static final Auxiliar auxClass = new Auxiliar();

    public static void main(String[] args) throws IOException {
        HashMap<String, String> confusionPhrases = auxClass.getConfusionPhrases();
        HashMap<String, String> excitementPhrases = auxClass.getExcitementPhrases();
        HashMap<String, String> fearPhrases = auxClass.getFearPhrases();
        HashMap<String, String> interestPhrases = auxClass.getUrgencyPhrases();
        HashMap<String, String> urgencyPhrases = auxClass.getInterestPhrases();

        List<String> con = Files.readAllLines(Paths.get("Simulation/legitimate_emails_50.txt"));
        List<String> sin = Files.readAllLines(Paths.get("Simulation/phishing_emails_50.txt"));

        // Ruta al modelo POS pre-entrenado
        String rutaModelo = "Assets/en-pos-maxent.bin";

        try (InputStream modelIn = new FileInputStream(rutaModelo)) {
            // Cargar el modelo POS pre-entrenado
            POSModel posModel = new POSModel(modelIn);
            POSTaggerME posTagger = new POSTaggerME(posModel);

            // Tokenizador
            SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

            // Procesar correos legítimos
            System.out.println("Processing legitimate emails:");
            for (String frase : con) {
                frase=frase.toLowerCase();
                int numCoincidences = processTopic(frase);
                System.out.println("Email: " + frase);
                System.out.println("Number of coincidences: " + numCoincidences);
            }

            // Procesar correos de phishing
            System.out.println("\nProcessing phishing emails:");
            for (String frase : sin) {
                frase=frase.toLowerCase();
                int numCoincidences = processTopic(frase);
                System.out.println("Email: " + frase);
                System.out.println("Number of coincidences: " + numCoincidences);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int processEmail(String frase, POSTaggerME posTagger, SimpleTokenizer tokenizer,
                                    HashMap<String, String> confusionPhrases, HashMap<String, String> excitementPhrases,
                                    HashMap<String, String> fearPhrases, HashMap<String, String> interestPhrases,
                                    HashMap<String, String> urgencyPhrases) {
        int numCoincidences = 0;

        // Tokenizar la frase
        String[] tokens = tokenizer.tokenize(frase);
        ArrayList<String> potentialTokens = new ArrayList<>();
        System.out.println("\n"+tokens.length);

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

        return numCoincidences;
    }

    private static boolean checkHashMaps(String phrase, HashMap<String, String>... hashMaps) {
        for (HashMap<String, String> hashMap : hashMaps) {
            if (hashMap.containsValue(phrase)) {
                return true;
            }
        }
        return false;
    }

    public static int processTopic(String frase) {
        HashMap<String,String> banking_phrases = auxClass.getBankingPhrases();
        HashMap<String,String> account_phrases = auxClass.getAccountsPhrases();
        HashMap<String,String> working_phrases = auxClass.getWorkPhrases();
        String rutaModelo = "Assets/en-pos-maxent.bin";
        int numBanking = 0;
        int numAccount = 0;
        int numWorking = 0;

        try (InputStream modelIn = new FileInputStream(rutaModelo)) {
            // Cargar el modelo POS pre-entrenado
            POSModel posModel = new POSModel(modelIn);
            POSTaggerME posTagger = new POSTaggerME(posModel);

            // Tokenizar la frase
            SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;



            // Tokenizar la frase
            String[] tokens = tokenizer.tokenize(frase.toLowerCase());
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

            // Verificar combinaciones de 2 palabras
            for (int i = 0; i < potentialTokens.size(); i++) {

                if (checkHashMaps(potentialTokens.get(i), banking_phrases)) {
                    numBanking++;
                } else if (checkHashMaps(potentialTokens.get(i),working_phrases)) {
                    numWorking++;
                } else if (checkHashMaps(potentialTokens.get(i),account_phrases)) {
                    numAccount++;
                }
            }


            System.out.println("Bank:" + numBanking);
            System.out.println("Account: " + numAccount);
            System.out.println("Work: " + numWorking);
            return encontrarMaximo(numBanking,numAccount,numWorking);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int encontrarMaximo(int num1, int num2, int num3) {
        if(num1 == num2 && num2 == num3){
            return -5;
        }
        int max = num1; // Supongamos que num1 es el mayor inicialmente

        if (num2 > max) {
            max = num2; // Si num2 es mayor que el actual máximo, actualiza max
        }

        if (num3 > max) {
            max = num3; // Si num3 es mayor que el actual máximo, actualiza max
            return 3;
        }else if(max == num2){
            return 2;
        }else return 1;
        // Devuelve el máximo encontrado
    }
}





//emociones a analizar 1. Urgency 2. Fear 3. Excitement/Expectation 4. Confusion 5. Interest
//def diccionario: Hola, quiero que me saques 5 deccionarios de frases clave, separadas en las emociones: 1. Urgency 2. Fear 3. Excitement/Expectation 4. Confusion 5. Interest en un txt cada uno de unas 50 lineas (mas o menos) cada uno, estos seran
// palabras/frases que se encontrarían en un mail calificado como "phishing" o "posible estafa". Las frases que añadas podran tener un numero menor o igual a 3 palabras y cada una de esas palabras que compone cada frase debera de ser un sustantivo, un verbo, un adjetivo, o un adverbio PORFAVOR (obio toodo en ingles)

//PROBLEMA CON APOSTROFES, se eliminaran todos por fallo en NLP don't -> dont

