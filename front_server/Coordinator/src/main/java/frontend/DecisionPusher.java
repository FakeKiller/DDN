package frontend;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;

/**
 * Push new decisions of groups maintained by current cluster to subscribed clusters
 *
 * Author: Shijie Sun
 * Email: septimus145@gmail.com
 * August, 2016
 */

public class DecisionPusher implements Runnable {

    protected String brokerList = "";		// list of broker
    protected String clusterID = "";
    public KafkaConsumer<String, String> consumer = null;       // kafka consumer
    public ConcurrentHashMap<String, String> clustersIP = null;
    public ConcurrentHashMap<String, KafkaProducer<String, String>> producerMap = null;
    public ConcurrentHashMap<String, HashSet> groupSubscriber = null;
    protected Properties producerProps = new Properties();

    public DecisionPusher( String clusterID, String brokerList, ConcurrentHashMap<String, String> clustersIP, ConcurrentHashMap<String, KafkaProducer<String, String>> producerMap, ConcurrentHashMap<String, HashSet> groupSubscriber ) {
        this.brokerList = brokerList;
        this.clusterID = clusterID;
        this.clustersIP = clustersIP;
        this.producerMap = producerMap;
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
        this.consumer.subscribe(Arrays.asList("decision"));

        // setup producer basic configurations
        producerProps.put("bootstrap.servers", brokerList); // only change this when create new producer
        producerProps.put("acks", "all");
        producerProps.put("retries", 0);
        producerProps.put("batch.size", 16384);
        producerProps.put("linger.ms", 1);
        producerProps.put("buffer.memory", 33554432);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    }

    public void run() {
        while(true) {
            ConsumerRecords<String, String> records = this.consumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                // decision format:   group_id;decision
                String[] decision = record.value().split(";");
                // if there are subscribers of this group_id
                if (groupSubscriber.containsKey(decision[0])) {
                    HashSet subscribers = groupSubscriber.get(decision[0]);
                    Iterator<String> it = subscribers.iterator();
                    // foreach subscriber
                    while(it.hasNext()){
                        String cluster = it.next();
                        // if it is not current cluster
                        if (cluster.equals(this.clusterID))
                            continue;
                        // if do not have producer for this cluster, create one
                        if (! producerMap.containsKey(cluster)) {
                            this.producerProps.put("bootstrap.servers", clustersIP.get(cluster).replace(",",":9092,") + ":9092");
                            KafkaProducer<String, String> producer = new KafkaProducer<String, String>(producerProps);
                            producerMap.put(cluster, producer);
                        }
                        // push the decision
                        ProducerRecord<String, String> data = new ProducerRecord<>("decision", record.value());
                        producerMap.get(cluster).send(data);
                    }
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
