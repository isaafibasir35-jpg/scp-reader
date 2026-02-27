package com.example.scpreader;

import java.io.Serializable;

public class SCPObject implements Serializable {
    private String id;
    private String number;
    private String title;
    private String content;

    public SCPObject(String number, String title) {
        this.number = number;
        this.title = title;
    }

    public SCPObject(String number, String title, String content) {
        this.number = number;
        this.title = title;
        this.content = content;
    }

    // Геттеры и сеттеры
    public String getNumber() { return number; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public void setTitle(String title) { this.title = title; }
}
