import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private String clientName;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Client(String clientName, String serverAddress, int port) {
        this.clientName = clientName;
        try {
            this.socket = new Socket(serverAddress, port);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            new ReadThread().start();
            sendClientName();
        } catch (IOException e) {
            System.err.println("Unable to connect to the server.");
        }
    }

    private void sendClientName() {
        out.println(clientName);
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendPrivateMessage(String recipient, String message) {
        out.println("@" + recipient + " " + message);
    }

    public void requestBannedPhrases() {
        out.println("BANNED_PHRASES");
    }

    private class ReadThread extends Thread {
        public void run() {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }
            } catch (IOException e) {
                System.err.println("Connection closed.");
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        Client client = new Client(username, "localhost", 12345);

        System.out.println("§--------------------------------------------------------------§\n" +
                        "Commands:\n" +
                        "@username message (private message) \n" +
                        "If you want to send a message to multiple users please type in their names with @ and spaces between mentions\n" +
                        "BANNED_PHRASES (query banned phrases)\n" +
                        "§--------------------------------------------------------------§");
        while (true) {
            String input = scanner.nextLine();
            if (input.isEmpty()) {
                System.out.println("Message can not be empty");
            }
            else if (input.equalsIgnoreCase("BANNED_PHRASES")) {
                client.requestBannedPhrases();
            } else if (input.startsWith("@")) {
                ArrayList<String> recipients = new ArrayList<>();
                int spaceIndex = input.indexOf(" ");
                if (spaceIndex != -1) {
                    String recipient = input.substring(1, spaceIndex);
                    String message = input.substring(spaceIndex + 1);
                    client.sendPrivateMessage(recipient, message);
                }
            } else {
                client.sendMessage(input);
            }
        }
    }
}
