package frontend;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

/**
 * Retrive the info of updates from file and send them to Kafka server
 *
 * Author: Shijie Sun
 * Email: septimus145@gmail.com
 * August, 2016
 */

public class InfoSender implements Runnable {

    protected String brokerList = "";		// list of broker
    protected String hostName = "";		// name of current host
    protected String clusterID = "";
    public KafkaProducer<String, String> producer = null;	// kafka producer

    public InfoSender( String hostName, String brokerList, String clusterID ) {
        this.hostName = hostName;
        this.brokerList = brokerList;
        this.clusterID = clusterID;
        // setup producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", brokerList);
        producerProps.put("acks", "all");
        producerProps.put("retries", 0);
        producerProps.put("batch.size", 16384);
        producerProps.put("linger.ms", 1);
        producerProps.put("buffer.memory", 33554432);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<String, String>(producerProps);
    }

    public void run() {
        while(true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                File file = new File("/var/www/info/info_queue");
                File file2 = new File("/var/www/info/info_queue2");
                file.renameTo(file2);
            } catch (Exception e2) {
                //System.err.println("Change file Exception: " + e2.getMessage());
            }
            try (BufferedReader br = new BufferedReader(new FileReader("/var/www/info/info_queue2"))) {
                String line;
                String topic;
                int i = 0;
                // foreach record
                while ((line = br.readLine()) != null) {
                    i++;
                    // if record's group belongs to current cluster, topic is group_id, otherwise topic is "outer"
                    JSONObject jObject = new JSONObject(line);
                    if (jObject.getString("cluster_id").equals(this.clusterID))
                        topic = jObject.getString("group_id");
                    else
                        topic = "external_groups";
                    ProducerRecord<String, String> data = new ProducerRecord<>(topic, line);
                    this.producer.send(data);
                }
                System.out.printf("Send %d msgs!\n",i);
            } catch (Exception e3) {
                //System.err.println("Read file Exception: " + e3.getMessage());
            }
            try {
                File file = new File("/var/www/info/info_queue2");
                file.delete();
            } catch (Exception e4) {
                //System.err.println("Deletc file Exception: " + e4.getMessage());
            }
        }
    }
}
