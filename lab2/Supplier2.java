package zad2;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Supplier2 {

    private static int ID = 0;

    public static void main(String[] argv) throws Exception {

        //info
        System.out.println("Enter your supplier name: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String supplierName = br.readLine();
        System.out.println("Welcome " + supplierName);
        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // routing key
        String br2 = "tlen plecak";
        String[] routingKeys = br2.split(" ");

        // exchange
        String EXCHANGE_NAME = "exchange17";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        // queue & bind
        for (String s: routingKeys) {
            channel.queueDeclare(s, true, false, false, null);
            channel.queueBind(s, EXCHANGE_NAME, s);
            // supplier (message handling)
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String clientName = new String(body, StandardCharsets.UTF_8);
                    System.out.println("Received an order from " + clientName + " for " + envelope.getRoutingKey());
                    channel.basicPublish(EXCHANGE_NAME, clientName, null, ("Your request was accepted by " + supplierName + " with ID = " + ID++).getBytes());
                }
            };
            channel.basicConsume(s, true, consumer);
        }
    }
}