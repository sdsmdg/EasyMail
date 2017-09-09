package com.example.android.easymail.models;

import io.realm.RealmObject;

/**
 * Created by Harshit Bansal on 6/21/2017.
 */

public class CustomListDetails extends RealmObject{

    private String listName;
    private String subject;
    private String notes;
    private boolean isAlarmEnabled;
    private boolean isNotifEnabled;
    private String date;
    private String time;

    public CustomListDetails(){}

    public CustomListDetails(String listName, String subject, String notes, String date, String time, boolean isAlarmEnabled, boolean isNotifEnabled){

        this.listName = listName;
        this.subject = subject;
        this.notes = notes;
        this.date = date;
        this.time = time;
        this.isAlarmEnabled = isAlarmEnabled;
        this.isNotifEnabled = isNotifEnabled;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSubject() {

        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getNotes() {

        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getListName() {

        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public boolean isNotifEnabled() {

        return isNotifEnabled;
    }

    public void setNotifEnabled(boolean notifEnabled) {
        isNotifEnabled = notifEnabled;
    }

    public boolean isAlarmEnabled() {

        return isAlarmEnabled;
    }

    public void setAlarmEnabled(boolean alarmEnabled) {
        isAlarmEnabled = alarmEnabled;
    }

    public String getDate() {

        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
