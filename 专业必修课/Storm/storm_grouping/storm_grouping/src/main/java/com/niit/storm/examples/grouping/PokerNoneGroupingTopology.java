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

/**
 * None Grouping
 * 无分组 - 不关心流是如何分组的（和随机分组一样）。
 *

 *
 * Process finished with exit code -1
 */
public class PokerNoneGroupingTopology {

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

            System.out.println("Spout: " + randomPoker);
            Utils.sleep(1000);

        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("poker"));
        }
    }

     public static class PrintBolt extends BaseRichBolt {

        @Override
        public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {

        }

        int sum = 0;

        @Override
        public void execute(Tuple input) {

            String value = input.getStringByField("poker");
            char threadName = (char)(65 + Thread.currentThread().getId() % 4);
            System.out.println("Thread " + threadName + ", received: " + value);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {

        }
    }


    public static void main(String[] args) {

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("PokerSpout", new RandomPokerSpout());
        builder.setBolt("PrintBolt", new PrintBolt(), 4)
                .noneGrouping("PokerSpout");

        // 代码提交到Storm集群上运行
        String topoName = PokerNoneGroupingTopology.class.getSimpleName();
        try {
            // StormSubmitter.submitTopology(topoName,new Config(), builder.createTopology());
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology(topoName,new Config(), builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
