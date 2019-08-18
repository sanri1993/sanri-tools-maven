package com.sanri.app.kafka;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sanri.app.servlet.KafkaServlet;
import org.apache.commons.lang3.StringUtils;
import sanri.utils.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方监控工具
 */
public class OffsetThirdpartMonitor extends BaseKafkaMonitor{

    /**
     * offset 监控
     * @param name
     * @param group
     * @return
     * @throws IOException
     */
    public List<TopicOffset> offsetMonitor(String name,String group) throws IOException {
        KafkaConnInfo kafkaConnInfo = connInfo(name);
        String thirdpartTool = kafkaConnInfo.getThirdpartTool();

        if(StringUtils.isBlank(thirdpartTool))return null;
        String url = "http://"+thirdpartTool+"/group/"+group;

        List<OffsetShow > offsetShows = new ArrayList<OffsetShow>();

        JSONObject data = HttpUtil.getJSON(url,null);
        JSONArray jsonArray = data.getJSONArray("offsets");
        for(int i=0;i<jsonArray.size();i++){
            JSONObject offsetShowJson = jsonArray.getJSONObject(i);

            String topic = offsetShowJson.getString("topic");
            Long offset = offsetShowJson.getLong("offset");
            String owner = offsetShowJson.getString("owner");
            Integer partition = offsetShowJson.getInteger("partition");
            Long logSize = offsetShowJson.getLong("logSize");
            Long modified = offsetShowJson.getLong("modified");

            OffsetShow offsetShow = new OffsetShow(topic, partition, offset, logSize);
            offsetShow.setOwner(owner);
            offsetShow.setModified(modified);
            offsetShows.add(offsetShow);
        }

        //分组
        Map<String,TopicOffset> topicOffsetMap = new HashMap<String, TopicOffset>();
        for (OffsetShow offsetShow : offsetShows) {
            String topic = offsetShow.getTopic();
            TopicOffset topicOffset = topicOffsetMap.get(topic);
            if(topicOffset == null){
                topicOffset = new TopicOffset(group, topic);
                topicOffsetMap.put(topic, topicOffset);
            }
            long logSize = topicOffset.getLogSize();topicOffset.setLogSize(logSize+=offsetShow.getLogSize());
            long offset = topicOffset.getOffset();topicOffset.setOffset(offset+=offsetShow.getOffset());
            long lag = topicOffset.getLag();topicOffset.setLag(lag+=offsetShow.getLag());
            int partitions = topicOffset.getPartitions();topicOffset.setPartitions(partitions++);

            topicOffset.addPartitionOffset(offsetShow);
        }

        return new ArrayList<TopicOffset>(topicOffsetMap.values());
    }
}
