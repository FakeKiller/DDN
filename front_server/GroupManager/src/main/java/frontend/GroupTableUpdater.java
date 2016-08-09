package frontend;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Fetch group table from Kafka and maintain it
 *
 * Author: Shijie Sun
 * Email: septimus145@gmail.com
 * August, 2016
 */


public class GroupTableUpdater implements Runnable {

    protected String brokerList = "";		// list of broker
    protected String hostname = "";		// name of current host
    public KafkaConsumer<String, String> consumer = null;       // kafka consumer
    protected String password = "";

    public GroupTableUpdater( String hostname, String brokerList, String password ) {
        this.hostname = hostname;
        this.brokerList = brokerList;
        this.password = password;
        // setup consumer
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", brokerList);
        consumerProps.put("group.id", this.hostname);
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("session.timeout.ms", "30000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList("group_table"));
    }

    public void run() {
        KafkaConsumer<String, String> tconsumer = consumer;
        while (true) {
            ConsumerRecords<String, String> records = tconsumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/var/www/info/groupEnv.conf"), "utf-8"))) {
                    writer.write(record.value());
                    String[] cmd = {"/bin/bash", "-c", "echo \"" + this.password + "\" | sudo -S service apache2 reload"};
                    Process pr = Runtime.getRuntime().exec(cmd);
                } catch (Exception e) {
                    System.err.println("Caught Exception: " + e.getMessage());
                }
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
