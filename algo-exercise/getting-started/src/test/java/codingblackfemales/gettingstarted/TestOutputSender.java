import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import codingblackfemales.gettingstarted.MyAlgoTest;


public class TestOutputSender {

    public static void main(String[] args) {
        TestOutputSender sender = new TestOutputSender();
        sender.runTestAndSendOutput();
    }

    public void runTestAndSendOutput() {
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Run MyAlgoBackTest programmatically
            Result result = JUnitCore.runClasses(MyAlgoTest.class);

            // Log test failures if any
            for (Failure failure : result.getFailures()) {
                System.err.println(failure.toString());
            }

            // Restore System.out
            System.setOut(originalOut);

            // Send output to the Node server
            String testOutput = outputStream.toString();
            sendOutputToNodeServer(testOutput);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.setOut(originalOut); // Ensure System.out is restored
        }
    }

    private void sendOutputToNodeServer(String data) {
        try {
            URL url = new URL("http://localhost:5173/receive-data");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);

            String jsonInputString = "{\"output\": \"" + data.replace("\"", "\\\"") + "\"}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            System.out.println("POST Response Code: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
