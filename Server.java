import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private int port;
    private String serverName;
    private Set<String> bannedPhrases;
    private Map<String, ClientHandler> clients;
    private Map<String, Integer> clientPorts;

    public Server(String configFile) {
        loadConfiguration(configFile);
        //clients = new HashMap<>();
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

    public synchronized boolean registerClient(String clientName, ClientHandler clientHandler, int port) {
        if (clientPorts.containsKey(clientName)) {
            ClientHandler.sendMess("The username '" + clientName + "' is already taken. Please choose another.", port);
            return false;
        }
        ClientHandler.sendMess("Registration successful. " + clientName + ", welcome, to the " + serverName + ".", port);
        broadcastMessage("Server", clientName + " has joined the chat.");
        //clients.put(clientName, clientHandler);
        clientPorts.put(clientName, port);
        System.out.println("Registered client: " + clientName + " on port: " + port);
        return true;
    }
    //TODO: implement finding the clienthandler with a port and client name
    //TODO: implement storing the client with a port and client name
    //TODO: max num of clients

    public synchronized void broadcastMessage(String sender, String message) {
        if (containsBannedPhrase(message)) {
            //clients.get(sender).sendMessage("Message blocked: contains banned phrase.");
            ClientHandler.sendMess("Message blocked: contains banned phrase.", clientPorts.get(sender));
            return;
        }

        /*for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(sender)) {
                entry.getValue().sendMessage(sender + ": " + message);
            }
        }*/
        for (Map.Entry<String, Integer> entry : clientPorts.entrySet()) {
            if (!entry.getKey().equals(sender)) {
                ClientHandler.sendMess(sender + ": " + message, entry.getValue());
            }
        }
    }

    public synchronized void sendMultiplePrivateMessages(String sender, Set<String> recipients, String message) {
        for (String recipient : recipients) {
            //ClientHandler client = clients.get(recipient.toLowerCase());
            if (clientPorts.get(recipient) != null) {
                int port = clientPorts.get(recipient);
                //client.sendMessage("(Private) " + sender + ": " + message);
                ClientHandler.sendMess("(Private) " + sender + ": " + message, port);
            } else {
                //clients.get(sender).sendMessage("User " + recipient + " is not connected.");
                int sendPort = clientPorts.get(sender);
                ClientHandler.sendMess("!User " + recipient + " is not connected.", sendPort);
            }
        }
    }
    public synchronized void sendToAllExcept(String sender, Set<String> excludedUsers, String message) {
        /*for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            String recipient = entry.getKey().toLowerCase();
            if (!excludedUsers.contains(recipient) && !recipient.equals(sender.toLowerCase())) {
                entry.getValue().sendMessage(sender + ": " + message);
            }
        }*/
        for (Map.Entry<String, Integer> entry : clientPorts.entrySet()) {
            String recipient = entry.getKey().toLowerCase();
            if (!excludedUsers.contains(recipient) && !recipient.equals(sender.toLowerCase())) {
                //entry.getValue().sendMessage(sender + ": " + message);
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