import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class ClientHandler extends Thread {
    private Socket socket;
    private Server server;
    private PrintWriter out;
    private String clientName;
    private static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public Socket getSocket() {
        return socket;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            clientHandlers.add(this);

            while (true) {
                clientName = in.readLine();

                if (server.registerClient(clientName, this, this.socket.getPort())) {
                    break;
                }else{
                    clientHandlers.remove(this);
                }
            }

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("BANNED_PHRASES")) {
                    out.println("Banned Phrases: " + server.getBannedPhrases());
                } else if(message.equalsIgnoreCase("CLIENTS")){
                    out.println("Connected clients: " + String.join(",",server.getClients()));
                }else if (message.startsWith("@")) {
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

    public static void sendMess(String message, int port){
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.getSocket().getPort() == port) {
                clientHandler.sendMessage(message);
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}