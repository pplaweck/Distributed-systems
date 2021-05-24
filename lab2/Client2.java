package zad2;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Client2 {

    public static void main(String[] argv) throws Exception {

        // info
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Client name: ");
        String clientName = br.readLine();
        System.out.println("Welcome " + clientName);

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // exchange
        String EXCHANGE_NAME = "exchange17";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, clientName);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println(message);
            }
        };
        channel.basicConsume(queueName, true, consumer);

        while (true) {
            // read message
            String message = br.readLine();
            // break condition
            if ("exit".equals(message)) {
                break;
            }
            // publish
            channel.basicPublish(EXCHANGE_NAME, message, null, clientName.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent request for " + message + " from " + clientName);
        }
        channel.close();
        connection.close();
    }
}