package frontend;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.Properties;

import scala.Tuple2;

import org.json.JSONObject;

import kafka.serializer.StringDecoder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.*;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka.*;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.Time;
// for changing logger config
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Consumes messages from one or more topics in Kafka and does decision making.
 * Example:
 *    $ bin/spark-submit --class frontend.DecisionMaker --master local[2] ~/frontend/DMLogic/target/DMLogic-1.0-SNAPSHOT.jar \
 *      broker1-host:port,broker2-host:port topic-in1,topic-in2 topic-out
 */

public final class DecisionMaker {

  private static final Pattern SPACE = Pattern.compile(" ");
  public static KafkaProducer<String, String> mykproducer = null;   // kafka producer

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      System.err.println("Usage: DecisionMaker <brokers> <topic-ins> <topic-out>\n" +
          "  <brokers> is a list of one or more Kafka brokers\n" +
          "  <topic-ins> is a list of one or more kafka topics to consume from\n" +
          "  <topic-out> is the kafka topic to publish the result\n\n");
      System.exit(1);
    }

    Logger.getLogger("org").setLevel(Level.OFF);
    Logger.getLogger("akka").setLevel(Level.OFF);

    final String brokers = args[0];
    String topic_ins = args[1];
    final String topic_out = args[2];

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
    mykproducer = new KafkaProducer<String, String>(producerProps);

    // Create context with a 2 seconds batch interval
    SparkConf sparkConf = new SparkConf().setAppName("DicisionMaker").setMaster("local");
    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.milliseconds(1000));

    Set<String> topicsSet = new HashSet<>(Arrays.asList(topic_ins.split(",")));
    Map<String, String> kafkaParams = new HashMap<>();
    kafkaParams.put("metadata.broker.list", brokers);

    // Create direct kafka stream with brokers and topics
    JavaPairInputDStream<String, String> messages = KafkaUtils.createDirectStream(
        jssc,
        String.class,
        String.class,
        StringDecoder.class,
        StringDecoder.class,
        kafkaParams,
        topicsSet
    );

    // map to pair to retrieve the data and group_id
    // then reduce by key to calculate the sum of sliding window
    JavaPairDStream<String, Integer> qualitySums = messages.mapToPair(
      new PairFunction<Tuple2<String, String>, String, Integer>() {
        @Override
        public Tuple2<String, Integer> call(Tuple2<String, String> tuple2) {
          JSONObject jObject = new JSONObject(tuple2._2().trim());
          String group_id = jObject.getString("group_id");
          int score = jObject.getJSONObject("update").getInt("score");
          return new Tuple2<>(group_id, score);
        }
      }).reduceByKeyAndWindow(
        new Function2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer i1, Integer i2) {
          return i1 + i2;
        }
      }, Durations.seconds(10), Durations.seconds(2));  //func, windowlength, slideinterval

    // put the result to kafka broker
    qualitySums.foreachRDD(new VoidFunction<JavaPairRDD<String,Integer>>() {
      @Override
      public void call(JavaPairRDD<String, Integer> values)
        throws Exception {
          values.foreach(new VoidFunction<Tuple2<String, Integer>> () {
            @Override
            public void call(Tuple2<String, Integer> tuple)
              throws Exception {
                ProducerRecord<String, String> data = new ProducerRecord<>(topic_out, tuple._1() + ";Group: " + tuple._1() + "  =>  Sum: " + tuple._2() + "...From: " + brokers);
                KafkaProducer<String, String> mykproducer2 = new KafkaProducer<String, String>(producerProps);
                mykproducer2.send(data);
                //System.out.format("------- Sum: %d ------\n", tuple._2());
            }} );
      }});

    // Start the computation
    jssc.start();
    jssc.awaitTermination();
  }
}

