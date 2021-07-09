package org.myorg.quickstart;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.util.Properties;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import org.apache.flink.api.java.tuple.Tuple2;


import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;
import org.apache.flink.util.Collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

/**
 * Skeleton for a Flink Streaming Job.
 *
 * <p>For a tutorial how to write a Flink streaming application, check the
 * tutorials and examples on the <a href="https://flink.apache.org/docs/stable/">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class StreamingJob {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingJob.class);

    public static void main(String[] args) throws Exception {

        LOG.info("Starting");

        // set up the streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // broker container_name
        String kafkaServer = "broker:9092";

        Properties propertiesC = new Properties();
        propertiesC.setProperty("bootstrap.servers", kafkaServer);
        // zookeeper container_name
        propertiesC.setProperty("zookeeper.connnect", "zookeeper:2181");
        propertiesC.setProperty("group.id", "test");


        //{"id":4,"temperature":-1,"c":5,"date":18793}
        // b) Lesen des Datenstroms aus dem Kafka-Topic Kafkawettser
        DataStreamSource<ObjectNode> input = env
                .addSource(new FlinkKafkaConsumer<>("myTestTopic", new JSONKeyValueDeserializationSchema(false), propertiesC));
        Properties propertiesP = new Properties();
        propertiesP.setProperty("bootstrap.servers", kafkaServer);

        // d) Übertragene Datensätze zählen
        // Schreiben der Zählereignisse in das Kafka-Topic "AnzahlData"
        DataStream<Tuple2<String, Integer>> count = input.flatMap(new Splitter())
                .keyBy(value -> value.f0)
                .sum(1);

        count.print();

        // // Schreiben der Zählereignisse in das Kafka-Topic "AnzahlData"
        FlinkKafkaProducer<String> producer1 = new FlinkKafkaProducer<>("AnzahlData", new SimpleStringSchema(), propertiesP);
        count.map(v -> v.f0).addSink(producer1);

        // e) Zählen der Datensätze, die pro Minute übertragen werden
        DataStream<Tuple2<String, Integer>> countM = input.flatMap(new Splitter())
                .keyBy(value -> value.f1)
                .timeWindow(Time.seconds(10))
                .sum(1);

        countM.print();
        FlinkKafkaProducer<String> producer2 = new FlinkKafkaProducer<String>("AnzahlDataMinute", new SimpleStringSchema(), propertiesP);
        //Schreiben der Zählereignisse in das Kafka-Topic "AnzahlDataMinute"
        countM.map(a -> a.f0).addSink(producer2);

        env.execute("Musterlösung Job"); //{"id":14,"temperature":11,"humidity":15,"date":22793}
    }

    public static class Splitter implements FlatMapFunction<ObjectNode, Tuple2<String, Integer>> {
        @Override
        public void flatMap(ObjectNode object, Collector<Tuple2<String, Integer>> out) throws Exception {
            out.collect(new Tuple2<>(object.toPrettyString(), 1));
        }
    }
}
