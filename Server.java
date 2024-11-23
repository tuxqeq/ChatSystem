import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private int port;
    private String serverName;
    private Set<String> bannedPhrases;
    private Map<String, ClientHandler> clients;
    private ExecutorService executorService;

    public Server(String configFile) {
        loadConfiguration(configFile);
        clients = new ConcurrentHashMap<>();
        executorService = Executors.newVirtualThreadPerTaskExecutor();
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
                executorService.submit(() -> new ClientHandler(socket, this).run());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    public synchronized boolean registerClient(String clientName, ClientHandler clientHandler) {
        if (clients.containsKey(clientName)) {
            clientHandler.sendMessage("The username '" + clientName + "' is already taken. Please choose another.");
            return false;
        }
        if (clients.size() >= 15) {
            clientHandler.sendMessage("Server is full, please try connecting later.");
            return false;
        }
        clientHandler.sendMessage("Registration successful. " + clientName + ", welcome to the " + serverName + ".");
        broadcastMessage("Server", clientName + " has joined the chat.");
        clients.put(clientName, clientHandler);
        System.out.println("Registered client: " + clientName + " on port: " + clientHandler.getSocket().getPort());
        return true;
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

    public synchronized void sendMultiplePrivateMessages(String sender, Set<String> recipients, String message) {
        for (String recipient : recipients) {
            ClientHandler client = clients.get(recipient);
            if (client != null) {
                client.sendMessage("(Private) " + sender + ": " + message);
            } else {
                clients.get(sender).sendMessage("User " + recipient + " is not connected.");
            }
        }
    }

    public synchronized void sendToAllExcept(String sender, Set<String> excludedUsers, String message) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            String recipient = entry.getKey();
            if (!excludedUsers.contains(recipient) && !recipient.equals(sender)) {
                entry.getValue().sendMessage(sender + ": " + message);
            }
        }
    }

    public synchronized String[] getClients() {
        return clients.keySet().toArray(new String[0]);
    }

    public synchronized void removeClient(String clientName) {
        clients.remove(clientName);
        broadcastMessage("Server", clientName + " has disconnected.");
        System.err.println("Client " + clientName + " has disconnected.");
    }

    private boolean containsBannedPhrase(String message) {
        String lowerMessage = message.toLowerCase();
        return bannedPhrases.stream().anyMatch(lowerMessage::contains);
    }

    public static void main(String[] args) {
        Server server = new Server("server_config.txt");
        server.start();
    }
}
