import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private int port;
    private String serverName;
    private Set<String> bannedPhrases;
    private Map<String, Integer> clientPorts;

    public Server(String configFile) {
        loadConfiguration(configFile);
        clientPorts = new HashMap<>();
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

    public synchronized boolean registerClient(String clientName, int port) {
        if (clientPorts.containsKey(clientName)) {
            ClientHandler.sendMess("The username '" + clientName + "' is already taken. Please choose another.", port);
            return false;
        }
        if(clientPorts.size() >= 15) {
            ClientHandler.sendMess("Server is full, please try connecting later", port);
            return false;
        }
        ClientHandler.sendMess("Registration successful. " + clientName + ", welcome, to the " + serverName + ".", port);
        broadcastMessage("Server", clientName + " has joined the chat.");
        clientPorts.put(clientName, port);
        System.out.println("Registered client: " + clientName + " on port: " + port);
        return true;
    }

    public synchronized void broadcastMessage(String sender, String message) {
        if (containsBannedPhrase(message)) {
            ClientHandler.sendMess("Message blocked: contains banned phrase.", clientPorts.get(sender));
            return;
        }
        for (Map.Entry<String, Integer> entry : clientPorts.entrySet()) {
            if (!entry.getKey().equals(sender)) {
                ClientHandler.sendMess(sender + ": " + message, entry.getValue());
            }
        }
    }

    public synchronized void sendMultiplePrivateMessages(String sender, Set<String> recipients, String message) {
        for (String recipient : recipients) {
            if (clientPorts.get(recipient) != null) {
                int port = clientPorts.get(recipient);
                ClientHandler.sendMess("(Private) " + sender + ": " + message, port);
            } else {
                int sendPort = clientPorts.get(sender);
                ClientHandler.sendMess("!User " + recipient + " is not connected.", sendPort);
            }
        }
    }
    public synchronized void sendToAllExcept(String sender, Set<String> excludedUsers, String message) {
        for (Map.Entry<String, Integer> entry : clientPorts.entrySet()) {
            String recipient = entry.getKey().toLowerCase();
            if (!excludedUsers.contains(recipient) && !recipient.equals(sender.toLowerCase())) {
                ClientHandler.sendMess(sender + ": " + message, entry.getValue());
            }
        }
    }

    public synchronized String[] getClients() {
        return clientPorts.keySet().toArray(new String[0]);
    }

    public synchronized void removeClient(String clientName) {
        clientPorts.remove(clientName);
        broadcastMessage("Server", clientName + " has disconnected.");
        System.err.println("Client " + clientName + " has disconnected.");
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