package com.github.weaselworks.myo.driver;

/**
 * Created by paulwatson on 23/11/2014.
 */
public enum Pose {

    FIST("FIST"),
    SPREAD("SPREAD"),
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    UNKNOWN("x");

    private String poseName;

    Pose(String poseName){
        this.poseName = poseName;
    }

    public static Pose fromString(String pose){
        if (pose == null || pose.trim().length() ==0) return Pose.UNKNOWN;
        switch (pose){
            case "FIST": return Pose.FIST;
            case "SPREAD": return Pose.SPREAD;
            case "LEFT": return Pose.LEFT;
            case "RIGHT": return Pose.RIGHT;
            default: return UNKNOWN;
        }
    }

    public String getName() {
        return poseName;
    }

    public boolean isKnownPose(){
        return !this.equals(UNKNOWN);
    }


}
