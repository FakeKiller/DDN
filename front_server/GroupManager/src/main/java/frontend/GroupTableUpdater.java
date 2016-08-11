package frontend;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;
import org.json.JSONArray;

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
    public ConcurrentHashMap<String, String> group2ClusterMap = null;

    public GroupTableUpdater( String hostname, String brokerList, ConcurrentHashMap<String, String> group2ClusterMap ) {
        this.hostname = hostname;
        this.brokerList = brokerList;
        this.group2ClusterMap = group2ClusterMap;
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
        consumer.subscribe(Arrays.asList("feature2group_table", "group2cluster_table"));
    }

    public void run() {
        while (true) {
            ConsumerRecords<String, String> records = this.consumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals("group2cluster_table")) {
                    JSONObject jObject = new JSONObject(record.value());
                    JSONArray jArray = jObject.names();
                    for (int i = 0; i < jArray.length(); i++) {
                        group2ClusterMap.put(jArray.getString(i), jObject.getString(jArray.getString(i)));
                    }
                }
                else {
                    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/var/www/info/match.php"), "utf-8"))) {
                        // TODO: generate the php code
                        String code = record.value();




                        writer.write(code);
                    } catch (Exception e) {
                        System.err.println("Caught Exception: " + e.getMessage());
                    }
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
