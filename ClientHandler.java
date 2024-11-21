import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class ClientHandler extends Thread {
    private Socket socket;
    private Server server;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);


            while (true) {
                //out.println("Enter username: ");
                clientName = in.readLine();
                out.println("Hello, " + clientName);

                if (clientName == null || clientName.isEmpty()) {
                    out.println("Invalid username. Please try again.");
                    continue;
                }

                if (server.registerClient(clientName, this)) {
                    //out.println("reg");
                    break;
                }else {
                    out.println("The username '" + clientName + "' is already taken. Please choose another.");
                }
            }

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("BANNED_PHRASES")) {
                    out.println("Banned Phrases: " + server.getBannedPhrases());
                } else if (message.startsWith("@")) {
                    Set<String> recipients = new HashSet<>();
                    String[] parts = message.split(" ");
                    int index = 0;

                    while (index < parts.length && parts[index].startsWith("@")) {
                        recipients.add(parts[index].substring(1).toLowerCase());
                        index++;
                    }

                    String actualMessage = String.join(" ", Arrays.copyOfRange(parts, index, parts.length));
                    server.sendMultiplePrivateMessages(clientName, recipients, actualMessage);
                } else if (message.startsWith("!")) {
                    Set<String> excludedUsers = new HashSet<>();
                    String[] parts = message.split(" ");
                    int index = 0;

                    while (index < parts.length && parts[index].startsWith("!")) {
                        excludedUsers.add(parts[index].substring(1).toLowerCase());
                        index++;
                    }

                    String actualMessage = String.join(" ", Arrays.copyOfRange(parts, index, parts.length));
                    server.sendToAllExcept(clientName, excludedUsers, actualMessage);
                } else {
                    server.broadcastMessage(clientName, message);
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client " + clientName);
        } finally {
            server.removeClient(clientName);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}