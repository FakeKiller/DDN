package frontend;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Maintain (cluster, ip) table and (group, subscriber) table
 *
 * Author: Shijie Sun
 * Email: septimus145@gmail.com
 * August, 2016
 */


public class TableMaintainer implements Runnable {

    protected String brokerList = "";		// list of broker
    protected String clusterID = "";		// name of current host
    public KafkaConsumer<String, String> consumer = null;       // kafka consumer
    public ConcurrentHashMap<String, String> clustersIP = null;
    public ConcurrentHashMap<String, HashSet> groupSubscriber = null;

    public TableMaintainer( String clusterID, String brokerList, ConcurrentHashMap<String, String> clustersIP, ConcurrentHashMap<String, HashSet> groupSubscriber ) {
        this.clusterID = clusterID;
        this.brokerList = brokerList;
        this.clustersIP = clustersIP;
        this.groupSubscriber = groupSubscriber;

        // setup consumer
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", brokerList);
        consumerProps.put("group.id", this.clusterID);
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("session.timeout.ms", "30000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList("cluster_ip", "group_subscriber"));
    }

    public void run() {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals("cluster_ip")) {
                    JSONObject jObject = new JSONObject(record.value());
                    JSONArray jArray = jObject.names();
                    for (int i = 0; i < jArray.length(); i++) {
                         clustersIP.put(jArray.getString(i), jObject.getString(jArray.getString(i)));
                    }
                }
                else {
                    String[] gs = record.value().split(";");
                    if (gs.length != 2)
                        continue;
                    if (groupSubscriber.containsKey(gs[0])) {
                        HashSet subscriber = groupSubscriber.get(gs[0]);
                        if (! subscriber.contains(gs[1]))
                            subscriber.add(gs[1]);
                        groupSubscriber.put(gs[0], subscriber);
                    } else {
                        HashSet subscriber = new HashSet();
                        subscriber.add(gs[1]);
                        groupSubscriber.put(gs[0], subscriber);
                    }
                }
                // After update:
                System.out.println("After update: cluster ip:");
                for (String key : clustersIP.keySet()) {
                    System.out.println(key + " " + clustersIP.get(key));
                }
                System.out.println("After update: subscriber:");
                for (String key : groupSubscriber.keySet()) {
                    System.out.println(key + " " + groupSubscriber.get(key).toString());
                }
            }
            try {
                Thread.sleep(500);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
