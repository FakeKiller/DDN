package frontend;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.producer.KafkaProducer;

/**
 * Coordinator of current cluster
 *
 * Forward updates which belong to other clusters
 * Forward decisions of groups of current cluster to all subscribed clusters
 *
 * Author: Shijie Sun
 * Email: septimus145@gmail.com
 * August, 2016
 */

public class Coordinator {

    protected Thread tableMaintainer = null;
    protected Thread updatesForwarder = null;
    protected Thread decisionPusher = null;
    protected String clusterID = "";
    protected String kafkaBrokerList = "";
    public ConcurrentHashMap<String, String> clustersIP = null;
    public ConcurrentHashMap<String, HashSet> groupSubscriber = null;
    public ConcurrentHashMap<String, String> externalGroupSub = null;
    public ConcurrentHashMap<String, KafkaProducer<String, String>> producerMap = null;

    public Coordinator( String clusterID, String kafkaServerList ) {
        this.clusterID = clusterID;
        this.kafkaBrokerList = kafkaServerList.replace(",",":9092,") + ":9092";
        this.clustersIP = new ConcurrentHashMap<>();
        this.groupSubscriber = new ConcurrentHashMap<>();
        this.producerMap = new ConcurrentHashMap<>();
        this.externalGroupSub = new ConcurrentHashMap<>();

        this.tableMaintainer = new Thread(new TableMaintainer(this.clusterID, this.kafkaBrokerList, this.clustersIP, this.groupSubscriber));
        this.tableMaintainer.setDaemon(true);
        this.tableMaintainer.start();
        System.out.println("Table maintainer ready.");

        this.updatesForwarder = new Thread(new UpdatesForwarder(this.clusterID, this.kafkaBrokerList, this.clustersIP, this.producerMap, this.externalGroupSub));
        this.updatesForwarder.setDaemon(true);
        this.updatesForwarder.start();
        System.out.println("Updates forwarder ready.");

        this.decisionPusher = new Thread(new DecisionPusher(this.clusterID, this.kafkaBrokerList, this.clustersIP, this.producerMap, this.groupSubscriber));
        this.decisionPusher.setDaemon(true);
        this.decisionPusher.start();
        System.out.println("Decision pusher ready.");
    }

    public static void main( String[] args )
    {
        if (args.length < 2) {
            System.out.println("Usage: java frontend.Coordinator cluster_ID kafka_server");
            System.out.println("\n\tcluster_ID is the ID of current cluster");
            System.out.println("\n\tkafka_server is the list of IP of kafka servers, separated by comma");
            return;
        }

        Coordinator co = new Coordinator(args[0], args[1]);

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
