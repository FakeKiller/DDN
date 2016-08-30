/**
 * Consume messages from one or more topics in Kafka and make decisions.
 *
 * Example:
 *    $ bin/spark-submit --class frontend.DecisionMaker --master local[*] ~/frontend/DMLogic/target/DMLogic-1.0-SNAPSHOT.jar \
 *      broker1-host:port,broker2-host:port topic-in topic-out RPS
 *
 * Author: Shijie Sun
 * Email: septimus145@gmail.com
 * August, 2016
 */

package frontend;

import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.*;

import scala.Tuple2;

import org.json.JSONObject;
import org.json.JSONArray;

import kafka.serializer.StringDecoder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.apache.spark.SparkConf;
import org.apache.spark.rdd.RDD;
import org.apache.spark.api.java.function.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka.*;
import org.apache.spark.streaming.Durations;

// for changing logger config
import org.apache.log4j.Logger;
import org.apache.log4j.Level;



public final class DecisionMaker {

  public final static int processInterval = 2; // seconds

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      System.err.println("Usage: DecisionMaker <brokers> <topic-in> <topic-out>\n" +
          "  <brokers> is a list of one or more Kafka brokers\n" +
          "  <topic-in> is the kafka topic to consume from\n" +
          "  <topic-out> is the kafka topic to publish the decision to\n");
      System.exit(1);
    }

    Logger.getLogger("org").setLevel(Level.OFF);
    Logger.getLogger("akka").setLevel(Level.OFF);

    // parse the arguments
    final String brokers = args[0];
    String topicIn = args[1];
    final String topicOut = args[2];
    final double gamma = 0.7;

    // setup producer
    final Properties producerProps = new Properties();
    producerProps.put("bootstrap.servers", brokers);
    producerProps.put("acks", "all");
    producerProps.put("retries", 0);
    producerProps.put("batch.size", 16384);
    producerProps.put("linger.ms", 1);
    producerProps.put("buffer.memory", 33554432);
    producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    // Create context with a 1 seconds batch interval
    SparkConf sparkConf = new SparkConf().setAppName("DicisionMaker");
    final JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(processInterval));

    // Create direct kafka stream with brokers and topic
    Set<String> topicSet = new HashSet<>(Arrays.asList(topicIn));
    Map<String, String> kafkaParams = new HashMap<>();
    kafkaParams.put("metadata.broker.list", brokers);
    JavaPairInputDStream<String, String> messages = KafkaUtils.createDirectStream(
        jssc,
        String.class,
        String.class,
        StringDecoder.class,
        StringDecoder.class,
        kafkaParams,
        topicSet
    );

    // map to pair to retrieve the data and group_id
    // then reduce by key to combine the performance of each batch
    JavaPairDStream<String, Map<String, double[]>> qualitySums = messages.mapToPair(
        new PairFunction<Tuple2<String, String>, String, Map<String, double[]>>() {
            @Override
            public Tuple2<String, Map<String, double[]>> call(Tuple2<String, String> tuple2) {
                JSONObject jObject = new JSONObject(tuple2._2().trim());
                String group_id = jObject.getString("group_id");
                String[] updates = jObject.getString("update").split("\t");
                double score = Double.parseDouble(updates[13]);
                String decision = updates[14];
                Map<String, double[]> info = new HashMap<String, double[]>();
                info.put(decision, new double[]{score,1});
                return new Tuple2<>(group_id, info);
            }
        }).reduceByKey(
        new Function2<Map<String, double[]>, Map<String, double[]>, Map<String, double[]>>() {
            @Override
            public Map<String, double[]> call(Map<String, double[]> m1, Map<String, double[]> m2) {
                // iterate map1 and merge it to map2
                Set<Map.Entry<String, double[]>> m1Entries = m1.entrySet();
                for (Map.Entry<String, double[]> m1Entry : m1Entries) {
                    double[] m2Value = m2.get(m1Entry.getKey());
                    double[] m1Value = m1Entry.getValue();
                    if (m2Value == null) {
                        m2.put(m1Entry.getKey(), m1Value);
                    }
                    else {
                        m2Value[0] += m1Value[0];
                        m2Value[1] += m1Value[1];
                    }
                }
                return m2;
            }
        });

    // create a rdd restore the old result
    List<Tuple2<String, Map<String, double[]>>> historyResult = new ArrayList<>();
    //// for test
    //Map<String, double[]> testMap = new HashMap<>();
    //testMap.put("decision1", new double[]{7000,200});
    //historyResult.add(new Tuple2("group1", testMap));
    JavaRDD<Tuple2<String, Map<String, double[]>>> historyDResult = jssc.sparkContext().parallelize(historyResult);
    JavaPairRDD<String, Map<String, double[]>> historyPairDResult = JavaPairRDD.fromJavaRDD(historyDResult);
    // because the variable be sent to inner class must be declared as final
    // using a container to make the variable still changeable
    final ConcurrentLinkedQueue<JavaPairRDD<String, Map<String, double[]>>> queue = new ConcurrentLinkedQueue<>();
    queue.add(historyPairDResult);

    // combine the old result with new result and send to kafka
    qualitySums.foreachRDD(new VoidFunction<JavaPairRDD<String, Map<String, double[]>>>() {
        // foreachRDD will get RDD of each batch of dstream
        @Override
        public void call(JavaPairRDD<String, Map<String, double[]>> groups) throws Exception {
            //System.out.println(groups.cogroup(historyPairDResult).collect());

            // combine old result with new result: cogroup then map
            JavaPairRDD<String, Map<String, double[]>> combinedResult = groups.cogroup(queue.poll()).mapToPair(new PairFunction<Tuple2<String, Tuple2<Iterable<Map<String, double[]>>, Iterable<Map<String, double[]>>>>, String, Map<String, double[]>>() {
                @Override
                public Tuple2<String, Map<String, double[]>> call(Tuple2<String, Tuple2<Iterable<Map<String, double[]>>, Iterable<Map<String, double[]>>>> tuple2) {
                    Map<String, double[]> oldResult=null, newResult=null;
                    Iterator<Map<String, double[]>> iter;
                    iter = tuple2._2()._1().iterator();
                    if (iter.hasNext())
                        newResult = iter.next();
                    iter = tuple2._2()._2().iterator();
                    if (iter.hasNext())
                        oldResult = iter.next();
                    // if there is old result
                    if (oldResult != null) {
                        for(Map.Entry<String, double[]> entry : oldResult.entrySet()) {
                            entry.setValue(new double[]{entry.getValue()[0] * gamma, entry.getValue()[1]*gamma});
                        }
                        // if both old and new results existed
                        if (newResult != null) {
                            for (Map.Entry<String, double[]> oldEntry : oldResult.entrySet()) {
                                double[] newValue = newResult.get(oldEntry.getKey());
                                double[] oldValue = oldEntry.getValue();
                                if (newValue == null) {
                                    newResult.put(oldEntry.getKey(), oldValue);
                                }
                                else {
                                    newValue[0] += oldValue[0];
                                    newValue[1] += oldValue[1];
                                }
                            }
                            return new Tuple2(tuple2._1(), newResult);
                        // if there is only old result
                        } else {
                            return new Tuple2(tuple2._1(), oldResult);
                        }
                    // if there is not old result
                    } else {
                        return new Tuple2(tuple2._1(), newResult);
                    }
                }
            });

            //System.out.println(combinedResult.collect());

            // update the old result rdd
            queue.add(combinedResult);

            //// to show the combined result clearly
            //List<Tuple2<String, Map<String, double[]>>> collectedResult = combinedResult.collect();
            //Tuple2<String, Map<String, double[]>> tmpTuple2 = null;
            //Map<String, double[]> tmpMap = null;
            //for (int i = 0; i < collectedResult.size(); i++) {
            //    tmpTuple2 = collectedResult.get(i);
            //    System.out.println(tmpTuple2._1() + "----");
            //    tmpMap = tmpTuple2._2();
            //    for (Map.Entry<String, double[]> entry : tmpMap.entrySet()) {
            //        System.out.printf("\t%s : (%f, %f)\n", entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
            //    }
            //}

            combinedResult.foreachPartition(new VoidFunction<Iterator<Tuple2<String, Map<String, double[]>>>> () {
                @Override
                public void call(Iterator<Tuple2<String, Map<String, double[]>>> group_iter) throws Exception {
                    KafkaProducer<String, String> kproducer = new KafkaProducer<String, String>(producerProps);
                    Tuple2<String, Map<String, double[]>> group = null;
                    while (group_iter.hasNext()) {
                        group = group_iter.next();
                        // select best decision and put other decisions to a json array
                        int N = 0;
                        int i = 0;
                        double[] score = new double[group._2().size()];
                        double totalScore = 0;
                        for (Map.Entry<String, double[]> entry : group._2().entrySet()) {
                            N += entry.getValue()[1];
                            if (entry.getValue()[1] > 0)
                                entry.getValue()[0] /=  entry.getValue()[1];
                            else
                                entry.getValue()[0] = 0;
                        }
                        double sqrt2logN = 0;
                        // if N <= 1, then it will be a negative number or zero.
                        // in this case, we will not compute the Ct(y,i)
                        if (N > 1)
                            Math.sqrt(2 * Math.log(N));
                        for (Map.Entry<String, double[]> entry : group._2().entrySet()) {
                            if (entry.getValue()[1] > 0)
                                score[i] = entry.getValue()[0] + sqrt2logN / Math.sqrt(entry.getValue()[1]);
                            else
                                score[i] = 0;
                            i++;
                        }
                        for (i = 0; i < score.length; i++) {
                            totalScore += score[i];
                        }
                        // generate the result and sent it to kafka server
                        JSONObject jObject = new JSONObject();
                        i = 0;
                        for (Map.Entry<String, double[]> entry : group._2().entrySet()) {
                            if (totalScore > 0) 
                                jObject.put(entry.getKey(), score[i] / totalScore);
                            else
                                jObject.put(entry.getKey(), 1.0 / score.length);
                            i++;
                        }
                        ProducerRecord<String, String> data = new ProducerRecord<>(topicOut, group._1() + ";" + jObject.toString() + ";From: " + brokers);
                        kproducer.send(data);
                    }
            }});
        }});

    // Start the computation
    jssc.start();
    jssc.awaitTermination();
  }
}
