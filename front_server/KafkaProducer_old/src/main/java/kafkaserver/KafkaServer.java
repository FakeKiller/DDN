package kafkaserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class KafkaServer implements Runnable {

    protected int serverPort = 8356;		// port to listen
    protected ServerSocket serverSocket = null;
    protected boolean isStopped = false;
    protected Thread runningThread = null;
    protected String brokerList = "";		// list of broker
    protected String hostName = "";		// name of current host
    public KafkaProducer<String, String> producer = null;	// kafka producer
    public ConcurrentHashMap<String, String> curr_decision = null;
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
 
        // initialize the decision map
        this.curr_decision = new ConcurrentHashMap<>();
        this.curr_decision.put("score", "Oops");

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
                ConcurrentHashMap<String, String> tcurr_decision = curr_decision;
                while (true) {
                    ConsumerRecords<String, String> records = tconsumer.poll(1000);
                    for (ConsumerRecord<String, String> record : records) {
                        tcurr_decision.put("score", record.value());
                        //System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value());
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
        // start port listener
        openServerSocket();
        System.out.println("Start listening...");
        // Listen to conn request and establish if avaliable
        while(! isStopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                System.out.println("I have a problem");
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            // start a new thread to deal with the conn
            Thread newWorker = new Thread(new KafkaWorker(this.hostName, clientSocket, this.producer, this.curr_decision));
            newWorker.setDaemon(true);
            newWorker.start();
        }
        System.out.println("Server Stopped.");
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
             this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + String.valueOf(this.serverPort), e);
        }
    }

}
