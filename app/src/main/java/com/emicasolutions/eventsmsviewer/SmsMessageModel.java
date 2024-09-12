package com.emicasolutions.eventsmsviewer;
public class SmsMessageModel {
    private String messageBody;

    public SmsMessageModel(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getMessageBody() {
        return messageBody;
    }
}
