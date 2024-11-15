import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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

            clientName = in.readLine();
            server.registerClient(clientName, this);
            server.broadcastMessage("Server", clientName + " has joined the chat.");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("BANNED_PHRASES")) {
                    out.println("Banned Phrases: " + server.getBannedPhrases());
                } else if (message.startsWith("@")) { // Check for private message
                    int spaceIndex = message.indexOf(" ");
                    if (spaceIndex != -1) {
                        String recipient = message.substring(1, spaceIndex);
                        String privateMessage = message.substring(spaceIndex + 1);
                        server.sendDirectMessage(clientName, recipient, privateMessage);
                    }
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
