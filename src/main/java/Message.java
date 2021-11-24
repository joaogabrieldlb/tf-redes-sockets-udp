package main.java;

import java.io.Serializable;

public class Message implements Serializable
{
    private char[2] command = new cj[2];
    private long seq;
    private String payload;
    
    public Message(String command, long seq, String payload) {
        this.command = command;
        this.seq = seq;
        this.payload = payload;
    }
}
