package frontend;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Manage the groups of current cluster
 *
 * Retrive the info of updates from file and send them to Kafka server
 * Fetch group table from Kafka and maintain it
 * Fetch decisions from Kafka
 *
 * Author: Shijie Sun
 * Email: septimus145@gmail.com
 * August, 2016
 */

public class GroupManager {

    protected Thread decisionCollector = null;
    protected Thread groupTableUpdater = null;
    protected Thread infoSender = null;
    protected String hostname = "";
    protected String kafkaBrokerList = "";
    protected String clusterID = "";
    protected String password = "";

    public GroupManager( String clusterID, String kafkaServerList, String password ) {
        this.clusterID = clusterID;
        this.password = password;
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e){
            this.hostname = "HOST";
        }
        this.kafkaBrokerList = kafkaServerList.replace(",",":9092,") + ":9092";

        this.groupTableUpdater = new Thread(new GroupTableUpdater(this.hostname, this.kafkaBrokerList, this.password));
        this.groupTableUpdater.setDaemon(true);
        this.groupTableUpdater.start();
        System.out.println("Group table updater ready.");

        this.decisionCollector = new Thread(new DecisionCollector(this.hostname, this.kafkaBrokerList));
        this.decisionCollector.setDaemon(true);
        this.decisionCollector.start();
        System.out.println("Decision collector ready.");

        this.infoSender = new Thread(new InfoSender(this.hostname, this.kafkaBrokerList, this.clusterID));
        this.infoSender.setDaemon(true);
        this.infoSender.start();
        System.out.println("Info sender ready.");
    }

    public static void main( String[] args )
    {
        // TODO: Fix this dangerous usage of root password
        if (args.length < 3) {
            System.out.println("Usage: java frontend.GroupManager cluster_ID kafka_server rootpwd");
            System.out.println("\n\tcluster_ID is the ID of current cluster");
            System.out.println("\n\tkafka_server is the list of IP of kafka servers, separated by comma");
            System.out.println("\n\trootpwd is the password for root privilege");
            return;
        }

        GroupManager gManager = new GroupManager(args[0], args[1], args[2]);

        while (true)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // code for stopping current task so thread stops
            }
        }
    }
}