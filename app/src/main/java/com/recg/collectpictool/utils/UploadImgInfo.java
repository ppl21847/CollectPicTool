package com.recg.collectpictool.utils;

/**
 * Created by liupaipai on 2018/7/2.
 */

public class UploadImgInfo {
    private int grade;
    private String subject;
    private String path;

    public UploadImgInfo(int grade, String subject, String path) {
        this.grade = grade;
        this.subject = subject;
        this.path = path;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
