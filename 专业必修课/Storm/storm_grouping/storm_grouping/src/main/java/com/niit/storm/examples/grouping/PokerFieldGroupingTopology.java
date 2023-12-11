package com.niit.storm.examples.grouping;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Field Grouping
 * 字段分组 - 流根据分组中指定的字段进行分区。
 *
 * Spout: ♣8
 * Thread D, received: ♣8
 * Spout: ♣9
 * Thread C, received: ♣9
 * Spout: ♦Q
 * Thread B, received: ♦Q
 * Spout: ♣9
 * Thread A, received: ♣9
 * Spout: ♠7
 * Thread C, received: ♠7
 * Spout: ♠2
 * Thread C, received: ♠2
 * Spout: ♣3
 * Thread A, received: ♣3
 * Spout: ♠9
 * Thread A, received: ♠9
 * Spout: ♠Q
 * Thread C, received: ♠Q
 * Spout: ♠8
 * Thread C, received: ♠8
 * Spout: ♥6
 * Thread B, received: ♥6
 */
public class PokerFieldGroupingTopology {

    public static class RandomPokerSpout extends BaseRichSpout {

        private final String[] TYPES = {"♠", "♥", "♦", "♣"};
        private final String[] NUMS = {"2","3","4","5","6","7","8","9","J","Q","K","A"};
        private Random rand;

        private SpoutOutputCollector collector;

        @Override
        public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
            rand = new Random();
            this.collector = collector;
        }

        @Override
        public void nextTuple() {
            String randomPoker = TYPES[rand.nextInt(TYPES.length)] + NUMS[rand.nextInt(NUMS.length)];
            this.collector.emit(new Values(randomPoker,randomPoker.substring(0,1)));
            System.out.println("Spout: " + randomPoker);
            Utils.sleep(1000);

        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("poker", "type"));
        }
    }

     public static class PrintBolt extends BaseRichBolt {

         private static ConcurrentHashMap<String, String> GroupInfo = new ConcurrentHashMap<>();

         @Override
        public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {

        }

        int sum = 0;

        @Override
        public void execute(Tuple input) {

            String value = input.getStringByField("poker");
            String color = input.getStringByField("type");

            char threadName = (char)(65 + Thread.currentThread().getId() % 4);
            System.out.println("Thread " + threadName + ", received: " + value);

            Common.UpdateGroupInfo(GroupInfo, "" + threadName, color);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {

        }
    }


    public static void main(String[] args) {

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("PokerSpout", new RandomPokerSpout());
        // 按字段分组
        builder.setBolt("PrintBolt", new PrintBolt(), 4).fieldsGrouping("PokerSpout", new Fields("type"));

        // 代码提交到Storm集群上运行
        String topoName = PokerFieldGroupingTopology.class.getSimpleName();
        try {
            // StormSubmitter.submitTopology(topoName,new Config(), builder.createTopology());
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology(topoName,new Config(), builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
