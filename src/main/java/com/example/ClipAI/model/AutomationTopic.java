package com.example.ClipAI.model;

import java.util.List;

public class AutomationTopic {
    private List<String> topic;

    private String test;
    public List<String> getTopic() {
        return this.topic;
    }

    public void setTopic(List<String> topic) {
        this.topic = topic;
    }

    public String getTest()
    {
        return test;
    }

    public void setTest(String test){
        this.test = test;
    }
}
