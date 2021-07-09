package org.myorg.quickstart;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;

import java.util.Properties;

public class WindowWordCount {

    public static void main(String[] args) throws Exception {

        /*Create Connection to broker*/
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        String kafkaServer = "broker:9092";

        /* Configure Connections */
        Properties propertiesC = new Properties();
        propertiesC.setProperty("bootstrap.servers", kafkaServer);
        // zookeeper container_name
        propertiesC.setProperty("zookeeper.connect", "zookeeper:2181");
        propertiesC.setProperty("group.id", "test");

        /* Add Data Source */
        DataStreamSource<ObjectNode> input = env
                .addSource(new FlinkKafkaConsumer<>("myTestTopic", new JSONKeyValueDeserializationSchema(false), propertiesC));
        Properties propertiesP = new Properties();
        propertiesP.setProperty("bootstrap.servers", kafkaServer);


        /* Operate on Datastream */


        SingleOutputStreamOperator<Double> sum = input.map(new Map()).keyBy(v -> v.f0).window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
                .reduce((t1, t2) -> new Tuple2(t1.f0 + t2.f0, t1.f1 + t2.f1))
                .map(new AVG());

        /* Create Producer wich writes into desired topic*/
        FlinkKafkaProducer<String> producer = new FlinkKafkaProducer<>("FilteredTopics", new SimpleStringSchema(), propertiesP);
        /* Execute to above defined operation on the producer*/
        sum.map(s -> s.toString()).addSink(producer);


        sum.print();

        env.execute("Window WordCount");
    }

    private static class Map implements MapFunction<ObjectNode, Tuple2<Double, Integer>> {

        @Override
        public Tuple2<Double, Integer> map(ObjectNode jsonNodes) {
            return new Tuple2<>(jsonNodes.findValue("Price").asDouble(), 1);
        }
    }

    private static class AVG implements MapFunction<Tuple2<Double, Integer>, Double> {
        @Override
        public Double map(Tuple2<Double, Integer> t) {
            return new Double(t.f0/t.f1);
        }
    }
}