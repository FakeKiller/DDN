package frontend;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;

/**
 * Forward updates whose group do not belong to current cluster to other clusters
 *
 * Author: Shijie Sun
 * Email: septimus145@gmail.com
 * August, 2016
 */

public class Uploader implements Runnable {

    protected String sourceBrokerList = "";		// list of broker of source-kafka
    protected String destBrokerList = "";		// list of broker of dest-kafka
    protected String sourceTopic = "";
    protected String destTopic = "";
    protected KafkaProducer<String, String> producer = null;       // kafka consumer
    protected KafkaConsumer<String, String> consumer = null;       // kafka consumer
    protected Properties producerProps = null;
    protected Properties consumerProps = null;

    public Uploader( String sourceBrokerList, String destBrokerList, String sourceTopic, String destTopic ) {
        this.sourceBrokerList = sourceBrokerList;
        this.destBrokerList = destBrokerList;
        this.sourceTopic = sourceTopic;
        this.destTopic = destTopic;

        // setup consumer
        consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", sourceBrokerList);
        consumerProps.put("group.id", "Uploader");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("session.timeout.ms", "30000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumer = new KafkaConsumer<>(consumerProps);
        this.consumer.subscribe(Arrays.asList(sourceTopic));

        // setup producer
        producerProps = new Properties();
        producerProps.put("bootstrap.servers", destBrokerList);
        producerProps.put("acks", "all");
        producerProps.put("retries", 0);
        producerProps.put("batch.size", 16384);
        producerProps.put("linger.ms", 1);
        producerProps.put("buffer.memory", 33554432);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<>(producerProps);
    }

    public void run() {
        while(true) {
            ConsumerRecords<String, String> records = this.consumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                // upload the update
                ProducerRecord<String, String> data = new ProducerRecord<>(destTopic, record.value());
                this.producer.send(data);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    public static void main( String[] args )
    {
        if (args.length < 4) {
            System.out.println("Usage: java frontend.Coordinator source_ip dest_ip source_topic dest_topic");
            System.out.println("\n\tsource_ip is the list of IP of source kafka servers, separated by comma");
            System.out.println("\tdest_ip is the list of IP of destination kafka servers, separated by comma");
            System.out.println("\tsource_topic is the topic to be uploaded");
            System.out.println("\tdest_topic is the topic uploaded to");
            return;
        }

        String sourceBrokerList = args[0].replace(",", ":9092,") + ":9092";
        String destBrokerList = args[1].replace(",", ":9092,") + ":9092";

        Thread uploader = new Thread(new Uploader(sourceBrokerList, destBrokerList, args[2], args[3]));
        uploader.setDaemon(true);
        uploader.start();

        while (true)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
