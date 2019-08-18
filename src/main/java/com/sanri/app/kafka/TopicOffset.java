package com.sanri.app.kafka;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

public class TopicOffset {
    private String group;
    private String topic;
    private int partitions;

    private long logSize;
    private long lag;
    private long offset;
    private List<OffsetShow> partitionOffsets = new ArrayList<OffsetShow>();

    public TopicOffset(String group, String topic, int partitions) {
        this.group = group;
        this.topic = topic;
        this.partitions = partitions;
    }

    public TopicOffset(String group, String topic) {
        super();
        this.group = group;
        this.topic = topic;
    }

    public void addPartitionOffset(OffsetShow offsetShow) {
        partitionOffsets.add(offsetShow);
    }

    public String getTopic() {
        return topic;
    }

    public long getLogSize() {
        return logSize;
    }

    public long getLag() {
        return lag;
    }

    public int getPartitions() {
        return partitions;
    }

    public List<OffsetShow> getPartitionOffsets() {
        return partitionOffsets;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public String getGroup() {
        return group;
    }


    public void setLogSize(long logSize) {
        this.logSize = logSize;
    }


    public void setLag(long lag) {
        this.lag = lag;
    }


    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }


    public void setPartitionOffsets(List<OffsetShow> partitionOffsets) {
        this.partitionOffsets = partitionOffsets;
    }


    public long getOffset() {
        return offset;
    }


    public void setOffset(long offset) {
        this.offset = offset;
    }

    public OffsetShow getPartitionOffsetShow(int partition) {
        for (OffsetShow offsetShow : partitionOffsets) {
            int currentPartition = offsetShow.getPartition();
            if (currentPartition == partitions) {
                return offsetShow;
            }
        }

        return null;
    }

    /**
     * 总偏移量计算
     */
    public void totalLagCalc() {
        for (OffsetShow partitionOffset : partitionOffsets) {
            long lag = partitionOffset.getLag();
            long offset = partitionOffset.getOffset();
            long logSize = partitionOffset.getLogSize();

            this.lag += lag;
            this.offset += offset;
            this.logSize += logSize;
        }
    }
}
