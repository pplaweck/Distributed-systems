import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

class ClientList implements Runnable {
    public Socket socket;
    List<ClientList> clientList;
    public String nick;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    public ClientList(List<ClientList> clientList, Socket socket) {
        this.clientList = clientList;
        this.socket = socket;
        try {
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            nick = inputStream.readUTF();
            while (true) {
                String msg = inputStream.readUTF();
                clientList.stream()
                        .filter(c -> !c.nick.equals(nick))
                        .forEach(c -> c.sendMsg(nick, msg));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String nick, String message) {
        try {
            outputStream.writeUTF(nick + ": " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendUDP(DatagramSocket datagramSocket, DatagramPacket packet) {
        byte[] buffer = Arrays.copyOf(packet.getData(), 1024);
        DatagramPacket newPacket = new DatagramPacket(buffer, buffer.length, socket.getRemoteSocketAddress());
        try {
            datagramSocket.send(newPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}