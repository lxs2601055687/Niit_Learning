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
 * Direct Grouping
 * 直接分组 - 按照这种方式分组的流意味着元组的生产者将决定消费者的哪个任务将接收该元组。
 *
 * 在默认输出流中向指定任务发出元组。此输出流必须声明为直接流（使用emitDirect方法发射）
 * 指定的任务必须使用此流的直接分组（Direct Grouping）来接收消息。发出的值必须是不可变的。
 * 由于没有指定消息id, Storm将不会跟踪此消息，因此永远不会为该元组调用ack和fail。
 *
 * [Thread-39-PokerSpout-executor[1, 1]] INFO  o.a.s.e.s.SpoutExecutor - Opening spout PokerSpout:[1]
 * [Thread-38-PrintBolt-executor[2, 2]] INFO  o.a.s.e.b.BoltExecutor - Preparing bolt PrintBolt:[2]
 * [Thread-37-PrintBolt-executor[3, 3]] INFO  o.a.s.e.b.BoltExecutor - Preparing bolt PrintBolt:[3]
 * [Thread-36-PrintBolt-executor[4, 4]] INFO  o.a.s.e.b.BoltExecutor - Preparing bolt PrintBolt:[4]
 * [Thread-35-PrintBolt-executor[5, 5]] INFO  o.a.s.e.b.BoltExecutor - Preparing bolt PrintBolt:[5]
 *
 * Spout emited: ♦Q
 * Thread D, received: ♦Q
 * Spout emited: ♠9
 * Thread D, received: ♠9
 * Spout emited: ♦6
 * Thread D, received: ♦6
 * Spout emited: ♣3
 * Thread D, received: ♣3
 * Spout emited: ♥Q
 * Thread D, received: ♥Q
 * Spout emited: ♦5
 * Thread D, received: ♦5
 * Spout emited: ♠A
 * Thread D, received: ♠A
 * Spout emited: ♠J
 * Thread D, received: ♠J
 * Spout emited: ♠2
 * Thread D, received: ♠2
 * Spout emited: ♠8
 * Thread D, received: ♠8
 * Spout emited: ♠3
 * Thread D, received: ♠3
 */
public class PokerDirectGroupingTopology {

    public static class RandomPokerSpout extends BaseRichSpout {

        private final String[] TYPES = {"♠", "♥", "♦", "♣"};
        private final String[] NUMS = {"2", "3", "4", "5", "6", "7", "8", "9", "J", "Q", "K", "A"};
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

            // 直接分组到ID为2的Bolt任务中
            this.collector.emitDirect(2, new Values(randomPoker));

            System.out.println("Spout emited: " + randomPoker);
            Utils.sleep(3000);

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
            char threadName = (char) (65 + Thread.currentThread().getId() % 4);
            System.out.println(Thread.currentThread().getId() + "-Thread " + threadName + ", received: " + value);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {

        }
    }


    public static void main(String[] args) {

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("PokerSpout", new RandomPokerSpout());
        builder.setBolt("PrintBolt", new PrintBolt(), 4)
                .directGrouping("PokerSpout");

        // 代码提交到Storm集群上运行
        String topoName = PokerDirectGroupingTopology.class.getSimpleName();
        try {
            // StormSubmitter.submitTopology(topoName,new Config(), builder.createTopology());
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology(topoName, new Config(), builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
