package kafkaserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaWorker implements Runnable{

    protected Socket clientSocket = null;
    public KafkaProducer<String, String> producer = null;
    protected String brokerHost = null;
    public ConcurrentHashMap<String, String> curr_decision = null;

    public KafkaWorker(String brokerHost, Socket clientSocket, KafkaProducer<String, String> producer, ConcurrentHashMap<String, String> curr_decision) {
        this.brokerHost = brokerHost;
        this.clientSocket = clientSocket;
        this.producer = producer;
        this.curr_decision = curr_decision;
    }

    public void run() {
        try {
            InputStream istream = clientSocket.getInputStream();
            OutputStream ostream = clientSocket.getOutputStream();
            // the message to be published
            byte[] tmpbyte = new byte[1024];
            istream.read(tmpbyte);
            String msg = new String(tmpbyte).trim();
            // publish
            ProducerRecord<String, String> data = new ProducerRecord<>("info_update", "node1", msg);
            this.producer.send(data); 
            // response
            long time = System.currentTimeMillis();
            ostream.write((this.curr_decision.get("score") + "\n" + this.brokerHost + " Ok. " + time).getBytes());
            ostream.close();
            istream.close();
            //System.out.println("Request processed: " + time);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
