import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private String clientName;
    protected Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Client(String clientName, String serverAddress, int port) {
        this.clientName = clientName;

        try {
            this.socket = new Socket(serverAddress, port);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
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

    private void startReadThread() {
        Thread.ofVirtual().start(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }
            } catch (IOException e) {
                System.err.println("Connection closed.");
            }
        });
    }

    public static void main(String[] args) {
        System.out.print("Enter username: ");
        Scanner scanner = new Scanner(System.in);
        String username;
        Client client = null;
        boolean isRegistered = false;

        while (!isRegistered) {
            username = scanner.nextLine();
            username = username.toLowerCase().replace(" ", "");

            if (username.isEmpty()) {
                System.err.println("Username cannot be empty. Please try again.");
                continue;
            }

            try {
                String response;
                client = new Client(username, "localhost", 12345);
                BufferedReader in = client.in;

                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                    if (response.startsWith("Registration successful")) {
                        isRegistered = true;
                        break;
                    }
                    if (response.startsWith("Server is full, please try connecting later")) {
                        System.out.println("Disconnecting....");
                        client.socket.close();
                        System.exit(0);
                        break;
                    }
                    if (response.startsWith("The username")) {
                        System.out.print("Enter username: ");
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Unable to connect to the server. Retrying...");
            }
        }
        client.startReadThread();

        System.out.println("\nCommands:\n" +
                "@username1 @username2 message (send private message to multiple users)\n" +
                "!username1 !username2 message (broadcast excluding specific users)\n" +
                "CLIENTS to list all the clients\n" +
                "BANNED_PHRASES to list all the banned phrases");
        while (true) {
            String input = scanner.nextLine();
            if (input.isEmpty()) {
                System.err.println("Message cannot be empty.");
            } else {
                client.sendMessage(input);
            }
        }
    }
}
