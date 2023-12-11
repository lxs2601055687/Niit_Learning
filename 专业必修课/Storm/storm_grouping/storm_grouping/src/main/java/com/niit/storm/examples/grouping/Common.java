package com.niit.storm.examples.grouping;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Common {
    public static void UpdateGroupInfo(ConcurrentHashMap<String, String> groupInfo, String key, String color){
        if(!groupInfo.containsKey(key)){
            groupInfo.put(key, color);
        }else{
            String colors = groupInfo.get(key);
            if(!colors.contains(color)){
                colors = colors + color;
                groupInfo.put(key, colors);
            }
        }

        for (String k:groupInfo.keySet()){
            System.out.println(k + " ==> " + groupInfo.get(k));
        }
    }
}

