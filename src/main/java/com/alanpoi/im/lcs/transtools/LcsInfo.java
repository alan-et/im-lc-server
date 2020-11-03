package com.alanpoi.im.lcs.transtools;


import java.util.Objects;

public class LcsInfo {
    private int id;
    private String ip;
    private int port;
    private long hbtime;
    private long starttime;

    LcsInfo(){
    }

    public int getId() {
        return id;
    }
    public String getStringId(){
        return String.format("%04x", id & 0xffff);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public long getHbtime() {
        return hbtime;
    }

    public long getStarttime() {
        return starttime;
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHbtime(long hbtime) {
        this.hbtime = hbtime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LcsInfo that = (LcsInfo) o;
        return Objects.equals(id, that.id) &&
                port == that.port &&
                Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ip, port);
    }
}
