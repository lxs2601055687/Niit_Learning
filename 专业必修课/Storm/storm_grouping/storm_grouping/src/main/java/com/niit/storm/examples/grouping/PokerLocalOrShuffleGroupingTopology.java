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
 * Local or Shuffle Grouping
 * 本地或随机分组 - 如果目标Bolt在同一个工作进程中有一个或多个任务，则元组将被随机发射到那些进程内任务（本地）。
 * 否则，这就像正常的洗牌分组一样（Shuffle）。
 *
 * Spout: ♦2
 *  Thread A, received: ♦2
 *  Spout: ♦K
 *  Thread B, received: ♦K
 *  Spout: ♠7
 *  Thread B, received: ♠7
 *  Spout: ♠6
 *  Thread A, received: ♠6
 *  Spout: ♣6
 *  Thread C, received: ♣6
 *  Spout: ♦Q
 *  Thread C, received: ♦Q
 *  Spout: ♠7
 *  Thread A, received: ♠7
 *  Spout: ♣9
 *  Thread B, received: ♣9
 *  Spout: ♦A
 *  Thread A, received: ♦A
 */
public class PokerLocalOrShuffleGroupingTopology {

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
                .localOrShuffleGrouping("PokerSpout");

        // 代码提交到Storm集群上运行
        String topoName = PokerLocalOrShuffleGroupingTopology.class.getSimpleName();
        try {
            // StormSubmitter.submitTopology(topoName,new Config(), builder.createTopology());
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology(topoName,new Config(), builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
