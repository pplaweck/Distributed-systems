import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private final List<ClientList> clientList = new ArrayList<>();

    public static void main(String[] args) {
        Server s = new Server();
        s.listenToClient();
    }

    public void listenToClient() {
        new Thread(this::listenToUDP).start();

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Zaakceptowano nowe połączenie.");
                ClientList newContext = new ClientList(clientList, clientSocket);
                clientList.add(newContext);
                new Thread(newContext).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void listenToUDP() {
        byte[] buffer = new byte[1024];
        try (DatagramSocket datagramSocket = new DatagramSocket(12345)) {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                String user = clientList.stream()
                        .filter(client -> client.socket.getPort() == packet.getPort()).findFirst().get().nick;
                System.out.println("Otrzymano UDP od " + user + "!");
                clientList.stream()
                        .filter(c -> c.socket.getPort() != packet.getPort())
                        .forEach(c -> c.sendUDP(datagramSocket, packet));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

