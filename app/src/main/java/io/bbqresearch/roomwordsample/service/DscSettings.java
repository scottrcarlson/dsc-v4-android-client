package io.bbqresearch.roomwordsample.service;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DscSettings {
    private final static String TAG = DscSettings.class.getSimpleName();

    private String deviceName = "";
    private String deviceAddr = "";
    private String userName = "";
    private String codingRate = "";
    private Integer syncWord = 0;
    private Integer deadBand = 0;
    private Integer txTime = 0;
    private String bandwidth = "";
    private Integer txPower = 0;
    private Integer spread_factor = 0;
    private boolean airplaneMode = false;
    private String frequency = "";
    private Integer totalNodes = 0;
    private Integer tdmaSlot = 0;


    public DscSettings() {

    }

    public String getSettingsJsonStr() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("topic", "setparms");

            obj.put("airplane_mode", new Boolean(airplaneMode));

            obj.put("freq", new String(frequency));
            obj.put("bw", new String(bandwidth));
            obj.put("sp_factor", new Integer(spread_factor));
            obj.put("coding_rate", new String(codingRate));
            obj.put("tx_power", new Integer(txPower));
            obj.put("sync_word", new Integer(syncWord));

            obj.put("deadband", new Integer(deadBand));
            obj.put("total_nodes", new Integer(totalNodes));
            obj.put("tdma_slot", new Integer(tdmaSlot));
            obj.put("tx_time", new Integer(txTime));
            return obj.toString();
        } catch (Exception e) {

        }
        return "";
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddr() {
        return deviceAddr;
    }

    public void setDeviceAddr(String deviceAddr) {
        this.deviceAddr = deviceAddr;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getMinTxPower() {
        return 11;
    }

    public Integer getMaxTxPower() {
        return 27;
    }

    public Integer getMinSpreadFactor() {
        return 6;
    }

    public Integer getMaxSpreadFactor() {
        return 12;
    }

    public String getCodingRate() {
        return codingRate;
    }

    public List<String> getCodingRateList() {
        List<String> cr = new ArrayList<String>();
        cr.add("4/5");
        cr.add("4/6");
        cr.add("4/7");
        cr.add("4/8");
        return cr;
    }

    public void setCodingRate_4_5() {
        this.codingRate = "4/5";
    }

    public void setCodingRate_4_6() {
        this.codingRate = "4/6";
    }

    public void setCodingRate_4_7() {
        this.codingRate = "4/7";
    }

    public void setCodingRate_4_8() {
        this.codingRate = "4/8";
    }

    public Integer getSyncWord() {
        return syncWord;
    }

    public void setSyncWord(Integer syncWord) {
        this.syncWord = syncWord;
    }

    public Integer getDeadBand() {
        return deadBand;
    }

    public void setDeadBand(Integer deadBand) {
        if (deadBand < 0) {
            deadBand = 0;
        }
        this.deadBand = deadBand;
    }

    public Integer getTxTime() {
        return txTime;
    }

    public void setTxTime(Integer txTime) {
        this.txTime = txTime;
    }

    public String getBandwidth() {
        return bandwidth;
    }

    public List<String> getBandwidthList() {
        List<String> bws = new ArrayList<String>();
        bws.add("62.5");
        bws.add("125");
        bws.add("250");
        bws.add("500");
        return bws;
    }

    public void setBandwidth_62_5khz() {
        this.bandwidth = "62.5";
    }

    public void setBandwidth_125khz() {
        this.bandwidth = "125";
    }

    public void setBandwidth_250khz() {
        this.bandwidth = "250";
    }

    public void setBandwidth_500khz() {
        this.bandwidth = "500";
    }

    public Integer getTxPower() {
        return txPower;
    }

    public void setTxPower(Integer txPower) {
        if (txPower > this.getMaxTxPower()) {
            this.txPower = this.getMaxTxPower();
        } else if (txPower < this.getMinTxPower()) {
            this.txPower = this.getMinTxPower();
        }
        this.txPower = txPower;
    }

    public Integer getSpread_factor() {
        return spread_factor;
    }

    public void setSpread_factor(Integer spread_factor) {
        if (spread_factor > this.getMaxSpreadFactor()) {
            this.spread_factor = this.getMaxSpreadFactor();
        } else if (spread_factor < this.getMinSpreadFactor()) {
            this.spread_factor = this.getMinSpreadFactor();
        }

        this.spread_factor = spread_factor;
    }

    public boolean isAirplaneMode() {
        return airplaneMode;
    }

    public void setAirplaneMode(boolean airplaneMode) {
        this.airplaneMode = airplaneMode;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public Integer getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(Integer totalNodes) {
        if (totalNodes < 1) {
            totalNodes = 1;
        }
        this.totalNodes = totalNodes;
    }

    public Integer getTdmaSlot() {
        return tdmaSlot;
    }

    public void setTdmaSlot(Integer tdmaSlot) {
        if (tdmaSlot < 0) {
            tdmaSlot = 0;
        }
        this.tdmaSlot = tdmaSlot;
    }
}
