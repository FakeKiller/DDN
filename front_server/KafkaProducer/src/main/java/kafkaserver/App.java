package kafkaserver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Server of kafka producer
 * Receiving message from port and submit it to Kafka cluster
 */
public class App 
{
    public static void main( String[] args )
    {
        if (args.length < 1) {
            System.out.println("Usage: java KafkaServer port_number");
            return;
        }
        String hostname = "HOST";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e){
            hostname = "HOST";
        }
        String brokerList = "10.1.1.2:9092,10.1.1.3:9092";
        Thread server = new Thread(new KafkaServer(Integer.parseInt(args[0]), hostname, brokerList));
        server.setDaemon(true);
        server.start();
        while (true)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // code for stopping current task so thread stops
            }
        }
    }
}
