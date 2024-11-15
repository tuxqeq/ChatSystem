import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private int port;
    private String serverName;
    private Set<String> bannedPhrases;
    private Map<String, ClientHandler> clients;

    public Server(String configFile) {
        loadConfiguration(configFile);
        clients = new HashMap<>();
    }

    private void loadConfiguration(String configFile) {
        bannedPhrases = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            port = Integer.parseInt(reader.readLine().trim());
            serverName = reader.readLine().trim();

            String line;
            while ((line = reader.readLine()) != null) {
                bannedPhrases.add(line.trim().toLowerCase());
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading server configuration: " + e.getMessage());
        }
    }

    public Set<String> getBannedPhrases() {
        return bannedPhrases;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void registerClient(String clientName, ClientHandler clientHandler) {
        clients.put(clientName, clientHandler);
    }

    public synchronized void broadcastMessage(String sender, String message) {
        if (containsBannedPhrase(message)) {
            clients.get(sender).sendMessage("Message blocked: contains banned phrase.");
            return;
        }

        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(sender)) {
                entry.getValue().sendMessage(sender + ": " + message);
            }
        }
    }

    public synchronized void sendDirectMessage(String sender, String recipient, String message) {
        ClientHandler client = clients.get(recipient);
        if (client != null) {
            client.sendMessage("(Private) " + sender + ": " + message);
        } else {
            clients.get(sender).sendMessage("User " + recipient + " is not connected.");
        }
    }

    public synchronized void removeClient(String clientName) {
        clients.remove(clientName);
        broadcastMessage("Server", clientName + " has disconnected.");
    }

    private boolean containsBannedPhrase(String message) {
        String lowerMessage = message.toLowerCase();
        for (String banned : bannedPhrases) {
            if (lowerMessage.contains(banned)) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        Server server = new Server("server_config.txt");
        server.start();
    }
}
