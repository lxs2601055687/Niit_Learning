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
 * Global Grouping
 * 全局分组-整个流进入到bolt的单个任务中。具体来说，它会转到id值最小的任务。
 *
 * Thread-199-D preparing bolt PrintBolt:[2]
 * Thread-198-C preparing bolt PrintBolt:[3]
 * Thread-197-B preparing bolt PrintBolt:[4]
 * Thread-196-A preparing bolt PrintBolt:[5]
 *
 * Spout: ♦J
 * Thread-199-D, received: ♦J
 * Spout: ♥9
 * Thread-199-D, received: ♥9
 * Spout: ♥K
 * Thread-199-D, received: ♥K
 * Spout: ♣9
 * Thread-199-D, received: ♣9
 * Spout: ♣7
 * Thread-199-D, received: ♣7
 * Spout: ♦3
 * Thread-199-D, received: ♦3
 * Spout: ♦2
 * Thread-199-D, received: ♦2
 * Spout: ♣9
 * Thread-199-D, received: ♣9
 */
public class PokerGlobalGroupingTopology {

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
            Utils.sleep(3000);

        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("poker"));
        }
    }

     public static class PrintBolt extends BaseRichBolt {
         int taskId;
         String threadName;

        @Override
        public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
            taskId = context.getThisTaskId();
            long threadId = Thread.currentThread().getId();
            threadName = "Thread-" + threadId + "-" + (char)(65 + threadId % 4);
            System.out.println( threadName + " preparing bolt PrintBolt:[" + taskId + "]");
        }

        int sum = 0;

        @Override
        public void execute(Tuple input) {

            String value = input.getStringByField("poker");
            System.out.println(threadName + ", received: " + value);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {

        }
    }


    public static void main(String[] args) {

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("PokerSpout", new RandomPokerSpout());
        // 全局分组
        builder.setBolt("PrintBolt", new PrintBolt(), 6).globalGrouping("PokerSpout");

        // 代码提交到Storm集群上运行
        String topoName = PokerGlobalGroupingTopology.class.getSimpleName();
        try {
            // StormSubmitter.submitTopology(topoName,new Config(), builder.createTopology());
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology(topoName,new Config(), builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
