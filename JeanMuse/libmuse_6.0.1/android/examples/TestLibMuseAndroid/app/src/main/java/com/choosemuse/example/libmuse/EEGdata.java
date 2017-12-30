package com.choosemuse.example.libmuse;

/**
 * Created by Jean on 12/28/2017.
 */

public class EEGdata {


    private EEGsignal alphaRef;
    private EEGsignal betaRef;
    private EEGsignal thetaRef;
    private EEGsignal deltaRef;
    private EEGsignal gammaRef;
    private EEGsignal eegDataRef ;

    private String connectedRef ;
    private String DeviceIdRef ;
    private String pausedRef ;


    public EEGdata(){
        alphaRef =new EEGsignal("Alpha_relative");
        betaRef =new EEGsignal("Beta_relative");
        thetaRef =new EEGsignal("Theta_relative");
        deltaRef =new EEGsignal("Delta_relative");
        gammaRef =new EEGsignal("Gamma_relative");
        eegDataRef =new EEGsignal("EEG_relative");
    }

    public EEGsignal getAlphaRef() {
        return alphaRef;
    }

    public EEGsignal getBetaRef() {
        return betaRef;
    }

    public EEGsignal getThetaRef() {
        return thetaRef;
    }

    public EEGsignal getDeltaRef() {
        return deltaRef;
    }

    public EEGsignal getGammaRef() {
        return gammaRef;
    }

    public EEGsignal getEEG_DataRef() {
        return eegDataRef;
    }

    public String getConnectedRef() {
        return connectedRef;
    }

    public String getDeviceIdRef() {
        return DeviceIdRef;
    }

    public String getPausedRef() {
        return pausedRef;
    }

    public void setConnectedRef(String connectedRef) {
        this.connectedRef = connectedRef;
    }

    public void setDeviceIdRef(String deviceIdRef) {
        DeviceIdRef = deviceIdRef;
    }

    public void setPausedRef(String pausedRef) {
        this.pausedRef = pausedRef;
    }

    @Override
    public String toString() {
        return "EEGdata{" +
                "alphaRef=" + alphaRef +
                ", betaRef=" + betaRef +
                ", thetaRef=" + thetaRef +
                ", deltaRef=" + deltaRef +
                ", gammaRef=" + gammaRef +
                ", eegDataRef=" + eegDataRef +
                ", connectedRef='" + connectedRef + '\'' +
                ", DeviceIdRef='" + DeviceIdRef + '\'' +
                ", pausedRef='" + pausedRef + '\'' +
                '}';
    }
}

/*
    DatabaseReference betaRef = database.getReference("Device/Beta_relative");
    DatabaseReference thetaRef = database.getReference("Device/Theta_relative");
    DatabaseReference deltaRef = database.getReference("Device/Delta_relative");
    DatabaseReference gammaRef = database.getReference("Device/Gamma_relative");

    DatabaseReference connectedRef = database.getReference("Device/Connected");
    DatabaseReference DeviceIdRef = database.getReference("Device/DeviceID");
    DatabaseReference eegDataRef = database.getReference("Device/EEG_data");
    DatabaseReference pausedRef = database.getReference("Device/Paused");
*/