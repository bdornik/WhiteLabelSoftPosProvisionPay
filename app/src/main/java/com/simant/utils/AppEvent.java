package com.simant.utils;

public class AppEvent {
    private String tag;

    private int process;
    public AppEvent(String tag, int process) {
        this.tag= tag;
        this.process = process;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getProcess() {
        return process;
    }

    public void setProcess(int process) {
        this.process = process;
    }

    @Override
    public String toString() {
        return "AppEvent{" +
                "tag='" + tag + '\'' +
                ", process=" + process +
                '}';
    }
}
