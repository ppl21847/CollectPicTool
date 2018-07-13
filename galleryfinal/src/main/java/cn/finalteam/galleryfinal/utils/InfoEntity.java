package cn.finalteam.galleryfinal.utils;

import java.util.List;

/**
 * Created by liupaipai on 2018/7/13.
 */

public class InfoEntity {
    private String grade;
    private String subject;
    private List<String>base64img;

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getBase64imgs() {
        return base64img;
    }

    public void setBase64imgs(List<String> base64imgs) {
        this.base64img = base64imgs;
    }
}
