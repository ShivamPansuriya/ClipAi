package com.example.ClipAI.model.youtube;

import java.util.ArrayList;
import java.util.List;

public class YoutubeMonitor
{
    private String videoCategory;
    private List<String > channelTags;
    private long userId;
    private MonitorEnum monitorType;
    private List<Scheduler> schedulers;
    private String playListUrl;

    public String getPlayListUrl() {
        return playListUrl;
    }

    public void setPlayListUrl(String playListUrl) {
        this.playListUrl = playListUrl;
    }


    public List<Scheduler> getSchedulers()
    {

        return schedulers;
    }

    public void setSchedulers(List<Scheduler> schedulers)
    {

        this.schedulers = schedulers;
    }

    public long getUserId()
    {

        return userId;
    }

    public void setUserId(long userId)
    {

        this.userId = userId;
    }

    public String getVideoCategory()
    {
        return videoCategory;
    }

    public void setVideoCategory(String videoCategory)
    {
        this.videoCategory = videoCategory;
    }

    public List<String> getChannelTags()
    {
        return channelTags;
    }

    public void setChannelTags(List<String> channelTags)
    {
        this.channelTags = channelTags;
    }

    public void addChannelTags(List<String> channelTags)
    {
        if(this.channelTags == null){
            this.channelTags = new ArrayList<>();
        }
        this.channelTags.addAll(channelTags);
    }

    public MonitorEnum getMonitorType()
    {

        return monitorType;
    }

    public void setMonitorType(MonitorEnum monitorType)
    {

        this.monitorType = monitorType;
    }

}
