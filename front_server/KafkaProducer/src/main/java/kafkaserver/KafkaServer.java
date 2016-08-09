package kafkaserver;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Writer;
import java.io.File;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class KafkaServer implements Runnable {

    protected int serverPort = 8356;		// port to listen
    protected boolean isStopped = false;
    protected Thread runningThread = null;
    protected String brokerList = "";		// list of broker
    protected String hostName = "";		// name of current host
    public KafkaProducer<String, String> producer = null;	// kafka producer
    public KafkaConsumer<String, String> consumer = null;       // kafka consumer
    public Thread consumerThread = null;

    public KafkaServer(int port, String hostName, String brokerList) {
        this.serverPort = port;
        this.hostName = hostName;
        this.brokerList = brokerList;
        System.out.println("------ Here is " + this.hostName + "------");
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
 
        // setup consumer
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", brokerList);
        consumerProps.put("group.id", this.hostName);
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("session.timeout.ms", "30000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList("info_result"));
        // run consumer in new thread
        consumerThread = new Thread(new Runnable() {
            public void run() {
                KafkaConsumer<String, String> tconsumer = consumer;
                while (true) {
                    ConsumerRecords<String, String> records = tconsumer.poll(1000);
                    for (ConsumerRecord<String, String> record : records) {
                        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/var/www/html/result.txt"), "utf-8"))) {
                            //System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value());
                            writer.write(record.value());
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
        });
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    public void run() {
        synchronized(this) {
            this.runningThread = Thread.currentThread();
        }
        while(! isStopped()) {
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                File file = new File("/var/www/html/payload.txt"); 
                File file2 = new File("/var/www/html/payload2.txt");
                file.renameTo(file2);
            } catch (Exception e2) {
                //System.err.println("Change file Exception: " + e2.getMessage());
            }
            try (BufferedReader br = new BufferedReader(new FileReader("/var/www/html/payload2.txt"))) {
                String line;
                int i = 0;
                while ((line = br.readLine()) != null) {
                    i++;
                    ProducerRecord<String, String> data = new ProducerRecord<>("info_update", line);
                    this.producer.send(data); 
                }
                System.out.printf("Send %d msgs!\n",i);
            } catch (Exception e3) {
                //System.err.println("Read file Exception: " + e3.getMessage());
            }
            try {
                File file = new File("/var/www/html/payload2.txt"); 
                file.delete();
            } catch (Exception e4) {
                //System.err.println("Deletc file Exception: " + e4.getMessage());
            }
        }
        System.out.println("Server Stopped.");
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
    }

}
