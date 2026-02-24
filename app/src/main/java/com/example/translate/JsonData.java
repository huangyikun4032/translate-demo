package com.example.translate;


public class JsonData {
    private long code;
    private String message;
    private Data data;

    public long getCode() { return code; }
    public void setCode(long value) { this.code = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { this.message = value; }
    public Data getData() { return data; }
    public void setData(Data value) { this.data = value; }
}
