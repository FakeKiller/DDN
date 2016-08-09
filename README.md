#DDN Real-time Decision Loop

##Environment

System: Ubuntu 14.04

Java compiler tools install:

```sh
$ sudo apt-get update
```

```sh
$ sudo apt-get install -y default-jdk maven
```


##Front Server

###Web Server

Deploy the httpd:

```sh
../front_server $ sudo ./frontserver_deploy.sh
```
###GroupManager

compile:

```sh
../GroupManager $ mvn package
```

run:

```sh
../GroupManager $ java -cp target/GroupManager-1.0-SNAPSHOT.jar frontend.GroupManager cluster_ID kafka_server rootpwd
```

	cluster_ID is the ID of current cluster

	kafka_server is the list of IP of kafka servers, separated by comma

	rootpwd is the password for root privilege

###Coordinator

compile:

```sh
../Coordinator $ mvn package
```

run:

```sh
../Coordinator $ java -cp target/Coordinator-1.0-SNAPSHOT.jar frontend.Coordinator cluster_ID kafka_server
```

	cluster_ID is the ID of current cluster

	kafka_server is the list of IP of kafka servers, separated by comma


##Kafka

deploy:

```sh
../kafka $ sudo ./kafka_deploy.sh host_list host_number
```

	host_list is all IP addresses of kafka servers, separated by comma
	
	host_number is the sequence number of current host in host_list

run: 

```sh
$ cd /usr/share/kafka
```

```sh
$ sudo bin/zookeeper-server-start.sh config/zookeeper.properties &
```

```sh
$ sudo bin/kafka-server-start.sh config/server.properties
```

**Note**:
If run kafka on more than one host. Execute third command only if second command has been executed on each host.

##Spark

deploy:

```sh
../spark $ sudo ./spark_deploy.sh
```

TODO: a shell to run spark master and slave and submit the job.

###DMLogic

compile:

```sh
../DMLogic $ mvn package
```

run:

```sh
$ cd /usr/share/spark
```

Then submit the job use `sudo bin/spark-submit`

##Benchmark

###post
A simple python program to perform HTTP POST request:

```sh
$ ./post.py
```

###post_time
A simple python program to perform HTTP POST request 1000 times and plot the CDF of response time:

```sh
$ ./post_time.py
```

###benchmark
A standalone benchmark to perform the HTTP POST request.
Test time and request per second(RPS) can be controlled.

```sh
$ ./benchmark.py
```

###distributed benchmark
A distributed benchmark to perform the HTTP POST request.

Run slave program on all the hosts to perform the benchmark. 
Then run master program on one host to start test.
When test finished, master program will generator three figures(Response Time, Successful RPS, CDF Response Time)


run slave:

```sh
$ ./dbenchmark_slave url
```

	url: Desti-URL slave program will send requests to

run master:

```sh
$ ./dbenchmark_master Time RPS
```

	Time: the time this test will last
	
	RPS: request per second. Actually this parameter is only positive correlated with real RPS. The real RPS will show in the result figure.

**Note**: the host runs master program need to install `matplotlib` :

```sh
sudo apt-get install -y python-matplotlib
```

###KafkaBenchmark

This is special designed for test of throughput of Kafka and Spark Streaming. Need cooperation of special msg format.

compile:

```sh
../KafkaBenchmark $ mvn package
```

run:

send msg to kafka:

```sh
java -cp target/KafkaBenchmark-1.0-SNAPSHOT.jar mybenchmark.MsgSender mps
```

	mps: messages per second

receive msg from kafka:

```sh
java -cp target/KafkaBenchmark-1.0-SNAPSHOT.jar mybenchmark.MsgReader topic
```

	topic: Kafka topic this Reader will comsume

TODO: dynamic topic and kafkaserver