package com.example.ClipAI.service.chat;

public enum ChatType {

    BIBLE("Imagine you are an experienced scriptwriter. Write a 1 minute video transcript in plain English that narrates the Bible story %s, write it in a way that's easy to digest and understand for a general audience. The story should be engaging and captivating, with a clear beginning, middle, and end. Focus on the key events and lessons of the story, and ensure the tone is approachable and relatable, making the story accessible to people who may not be familiar with it.      FORMAT REQUIREMENTS: - Write in plain text without quotation marks - No speaker/narrator labels - No timestamps or technical directions - One continuous paragraph - Avoid special characters that could break JSON"),
    HISTORY_SCHOKING("Imagine you are an experienced scriptwriter. Write a 1 minute video transcript in plain English that narrates the History story or current situations %s, write it in a way that's easy to digest and understand for a general audience. The story/narration should be engaging and captivating, with a clear beginning, middle, and end. Focus on the key events and unknown truth, and ensure the tone is approachable and relatable, making the story accessible to people who may not be familiar with it.      FORMAT REQUIREMENTS: - Write in plain text without quotation marks - No speaker/narrator labels - No timestamps or technical directions - One continuous paragraph - Avoid special characters that could break JSON");
    private final String script;
    private ChatType(String script){
        this.script = script;
    }

    public String getScript(){
        return script;
    }
}
