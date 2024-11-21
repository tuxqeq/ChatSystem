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
            new ReadThread().start();
            sendClientName();
        } catch (IOException e) {
            System.err.println("Unable to connect to the server.");
        }
    }

    // Send normalized client name to the server
    private void sendClientName() {
        out.println(clientName);
    }

    public void sendMessage(String message) {
        out.println(message);
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
        System.out.print("1Enter username: ");
        Scanner scanner = new Scanner(System.in);
        String username;
        Client client = null;
        boolean isRegistered = false;

        while (!isRegistered) {

            username = scanner.nextLine();
            username = username.toLowerCase().replace(" ",  "");

            if (username.isEmpty()) {
                System.out.println("Username cannot be empty. Please try again.");
                continue;
            }

            try {
                String response;
                client = new Client(username, "localhost", 12345);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.socket.getInputStream()));

                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                    if (response.startsWith("Registration successful")) {
                        isRegistered = true;
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


        // Interaction loop for sending messages
        System.out.println("Commands:\n" +
                "@username1 @username2 message (send private message to multiple users)\n" +
                "!username1 !username2 message (broadcast excluding specific users)");
        while (true) {
            String input = scanner.nextLine();
            if (input.isEmpty()) {
                System.out.println("Message cannot be empty.");
            } else {
                client.sendMessage(input);
            }
        }
    }
}