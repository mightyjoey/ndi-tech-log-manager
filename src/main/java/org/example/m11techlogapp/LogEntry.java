package org.example.m11techlogapp;

public class LogEntry {
    private String dttm;
    private String name;
    private String nomen;
    private String mal_cd;
    private String hours;
    private String corr_act;

    public LogEntry(String date, String name, String nomen, String malCd, String hours, String corr_act) {
        this.dttm = date;
        this.name = name;
        this.nomen = nomen;
        this.mal_cd = malCd;
        this.hours = hours;
        this.corr_act = corr_act;
    }

    public String getDttm() { return dttm; }
    public String getName() { return name; }
    public String getNomen() { return nomen; }
    public String getMal_cd() { return mal_cd; }
    public String getHours() { return hours; }
    public String getCorr_act() { return corr_act; }
}
