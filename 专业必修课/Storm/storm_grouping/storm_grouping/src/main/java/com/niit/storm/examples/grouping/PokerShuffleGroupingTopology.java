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

import java.security.acl.Group;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shuffle Grouping
 * 随机分组 - 元组在指定Bolt的任务中随机分布，这样每个Bolt任务就能保证得到差不多相同数量的元组。
 *
 *  Spout emited: ♥7
 *  Thread A, received: ♥7
 *  Spout emited: ♠K
 *  Thread B, received: ♠K
 *  Spout emited: ♠3
 *  Thread A, received: ♠3
 *  Spout emited: ♦9
 *  Thread C, received: ♦9
 *  Spout emited: ♥A
 *  Thread A, received: ♥A
 *  Spout emited: ♥Q
 *  Thread A, received: ♥Q
 *  Spout emited: ♠J
 *  Thread C, received: ♠J
 *  Spout emited: ♥A
 *  Thread D, received: ♥A
 *  Spout emited: ♠4
 *  Thread C, received: ♠4
 *  Spout emited: ♥3
 *  Thread C, received: ♥3
 *  Spout emited: ♠8
 *  Thread C, received: ♠8
 *  Spout emited: ♦8
 *  Thread D, received: ♦8
 *  Spout emited: ♥7
 *  Thread B, received: ♥7
 */
public class PokerShuffleGroupingTopology {

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

            this.collector.emit(new Values(randomPoker));

            System.out.println("Spout emited: " + randomPoker);
            Utils.sleep(1000);

        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("poker"));
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
            char threadName = (char) (65 + Thread.currentThread().getId() % 4);
            System.out.println("Thread " + threadName + ", received: " + value);

            Common.UpdateGroupInfo(GroupInfo, "" + threadName, value.substring(0,1));
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {

        }
    }


    public static void main(String[] args) {

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("PokerSpout", new RandomPokerSpout());
        builder.setBolt("PrintBolt", new PrintBolt(), 4)
                .shuffleGrouping("PokerSpout");

        // 代码提交到Storm集群上运行
        String topoName = PokerShuffleGroupingTopology.class.getSimpleName();
        try {
            // StormSubmitter.submitTopology(topoName,new Config(), builder.createTopology());
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology(topoName,new Config(), builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
