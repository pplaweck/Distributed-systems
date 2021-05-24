import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Client1 {

    private static final String ASCII = "    ^    \n" +
                                        "   ^^^   \n" +
                                        "  ^^^^^  \n" +
                                        " ^^^^^^^ \n" +
                                        "^^^^^^^^^\n" +
                                        "   |||   \n" +
                                        "   |||   \n";

    String nick;
    DataInputStream iS;
    DataOutputStream oS;
    DatagramSocket dSoc;

    public static void main(String[] args) {
        Client1 client1 = new Client1();
        client1.init();
    }

    public void connect() {
        try (Socket clientSocket = new Socket("127.0.0.1", 12345);
             DatagramSocket dSoc = new DatagramSocket(clientSocket.getLocalPort())) {

            iS = new DataInputStream(clientSocket.getInputStream());
            oS = new DataOutputStream(clientSocket.getOutputStream());
            oS.writeUTF(nick);

            this.dSoc = dSoc;
            new Thread(() -> listenUDP()).start();

            while (true) {
                String message = iS.readUTF();
                System.out.print("\r" + " ".repeat(50));
                System.out.println("\r" + message);
                System.out.print("- ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        Scanner stdin = new Scanner(System.in);
        System.out.print("Wpisz swój nick: ");
        this.nick = stdin.nextLine();

        new Thread(this::connect).start();

        while (true) {
            System.out.print("- ");
            String message = stdin.nextLine();

            if (message.equals("U")) {
                sendUDP();
            } else {
                sendMessage(message);
            }
        }
    }

    public void sendUDP() {
        String nickname = nick;
        String text = ": Poprawnie wysłano UDP \n" + ASCII;
        String message = nickname.concat(text);
        byte[] buffer = message.getBytes();
        try {
            int port = 12345;
            InetAddress destAddress = InetAddress.getLocalHost();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destAddress, port);
            dSoc.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void sendMessage(String message) {
        try {
            oS.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenUDP() {
        byte[] buffer = new byte[1024];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                dSoc.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] fullMsg = new String(packet.getData()).trim().split(":");
            String sender = fullMsg[0];
            String message = fullMsg[1];

            if (sender.equals(nick)) {
                continue;
            }
            String spaces = concatSpaces(50);
            System.out.print("\r" + spaces);
            System.out.print("\r" + sender + ": ");
            System.out.println(message);
            System.out.print("- ");
        }

    }
    private String concatSpaces(int num){
        String spaceString = " ";
        for(int i = 0; i<num; i++){
            spaceString.concat(" ");
        }
        return spaceString;
    }
}