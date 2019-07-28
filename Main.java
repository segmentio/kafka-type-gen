import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        String filename = "";

        try {
            filename = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Expecting output filename as a single argument.");
            System.exit(1);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            String output = "";

            if (filename.endsWith(".bnf")) {
                output = KafkaBNF.generate();
            } else if (filename.endsWith(".go")) {
                output = KafkaGo.generate();
            } else {
                System.err.println("Unknown file type: " + filename);
                System.exit(1);
            }

            bw.write(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
