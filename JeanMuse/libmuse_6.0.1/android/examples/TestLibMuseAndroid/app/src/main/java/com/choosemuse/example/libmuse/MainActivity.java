/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2016
 */

package com.choosemuse.example.libmuse;


import java.lang.ref.WeakReference;
import java.util.List;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseVersion;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import android.content.Intent;
import android.graphics.Color;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

//this is adapted from the muse example and the socket.io java client

/**
 * This example will illustrate how to connect to a Muse headband,
 * register for and receive EEG data and disconnect from the headband.
 * Saving EEG data to a .muse file is also covered.
 *
 * For instructions on how to pair your headband with your Android device
 * please see:
 * http://developer.choosemuse.com/hardware-firmware/bluetooth-connectivity/developer-sdk-bluetooth-connectivity-2
 *
 * Usage instructions:
 * 1. Pair your headband if necessary.
 * 2. Run this project.
 * 3. Turn on the Muse headband.
 * 4. Press "Refresh". It should display all paired Muses in the Spinner drop down at the
 *    top of the screen.  It may take a few seconds for the headband to be detected.
 * 5. Select the headband you want to connect to and press "Connect".
 * 6. You should see EEG and accelerometer data as well as connection status,
 *    version information and relative alpha values appear on the screen.
 * 7. You can pause/resume data transmission with the button at the bottom of the screen.
 * 8. To disconnect from the headband, press "Disconnect"
 */
public class MainActivity extends Activity implements OnClickListener  {

    // added chart to display data from the socket server
    private LineChart mChart;
    private Thread thread;  //thread for graph sampling rate
    private boolean plotData =true;
    private boolean plot=true;
    //socket.io
    private static final int REQUEST_LOGIN = 0;
    private String mUsername;
    private Socket mSocket;

    private Boolean isConnected = true;
    private TextView mDataEchoTx;
    private TextView mUserNameTx;


    /**
     * Tag used for logging purposes.
     */
    private final String TAG = "Socket_LibMuseAndroid";

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     *
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     *
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

    /**
     * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
     * depending on the type of headband and the preset configuration.  We buffer the
     * data that is read until we can update the UI.
     *
     * The stale flags indicate whether or not new data has been received and the buffers
     * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
     * and ACCELEROMETER values in this example.
     *
     * Note: the array lengths of the buffers are taken from the comments in
     * MuseDataPacketType, which specify 3 values for accelerometer and 6
     * values for EEG and EEG-derived packets.
     */
    private final double[] eegBuffer = new double[6];
    private boolean eegStale;

    private final double[] alphaBuffer = new double[6]; //other buffer
    private boolean alphaStale;
    private final double[] betaBuffer = new double[6]; //other buffer
    private boolean betaStale;
    private final double[] thetaBuffer = new double[6]; //other buffer
    private boolean thetaStale;
    private final double[] deltaBuffer = new double[6]; //other buffer
    private boolean deltaStale;
    private final double[] gammaBuffer = new double[6]; //other buffer
    private boolean gammaStale;

    private final double[] accelBuffer = new double[3];
    private boolean accelStale;

    /**
     * We will be updating the UI using a handler instead of in packet handlers because
     * packets come in at a very high frequency and it only makes sense to update the UI
     * at about 60fps. The update functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Handler handler = new Handler();

    /**
     * In the UI, the list of Muses you can connect to is displayed in a Spinner object for this example.
     * This spinner adapter contains the MAC addresses of all of the headbands we have discovered.
     */
    private ArrayAdapter<String> spinnerAdapter;

    /**
     * It is possible to pause the data transmission from the headband.  This boolean tracks whether
     * or not the data transmission is enabled as we allow the user to pause transmission in the UI.
     */
    private boolean dataTransmission = true;




    //--------------------------------------
    // Lifecycle / Connection code
   /**holds my EEGsignals and device atributes */
   EEGdata mEEGdata = new EEGdata();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<MainActivity> weakActivity =
                new WeakReference<MainActivity>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);
        // Register a listener to receive notifications of what Muse headbands
        // we can connect to.
        manager.setMuseListener(new MuseL(weakActivity));

        // Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
        // simplify the connection process.  This requires access to the COARSE_LOCATION
        // or FINE_LOCATION permissions.  Make sure we have these permissions before
        // proceeding.
        ensurePermissions();



        mSocket =SocketHandler.getInstance(); //singleton for socket ref
        mSocket.on(Socket.EVENT_CONNECT,onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);

        mSocket.on("alphaDataEcho", onAlphaDataEcho);
        mSocket.on("user joined", onUserJoined);
        mSocket.on("user left", onUserLeft);
        //mSocket.on("engagement ",onEngagement); implement

        mSocket.connect();



        initUI();

        //create graph for data
        drawGraph();

        //call to login activity for userName
        startSignIn();

        // Start our asynchronous updates of the UI.
        handler.post(tickUi);

        //start plotting data
        startPlot();
    }

    /*public boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }*/

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.refresh) {
            // The user has pressed the "Refresh" button.
            // Start listening for nearby or paired Muse headbands. We call stopListening
            // first to make sure startListening will clear the list of headbands and start fresh.
            manager.stopListening();
            manager.startListening();

        } else if (v.getId() == R.id.connect) {

            // The user has pressed the "Connect" button to connect to
            // the headband in the spinner.

            // Listening is an expensive operation, so now that we know
            // which headband the user wants to connect to we can stop
            // listening for other headbands.
            manager.stopListening();

            List<Muse> availableMuses = manager.getMuses();
            Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);

            // Check that we actually have something to connect to.
            if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
                Log.w(TAG, "There is nothing to connect to");
            } else {

                // Cache the Muse that the user has selected.
                muse = availableMuses.get(musesSpinner.getSelectedItemPosition());

                //set name and ref for possible socket use
                mEEGdata.setDeviceIdRef(muse.getName() + " - " + muse.getMacAddress());



                // Unregister all prior listeners and register our data listener to
                // receive the MuseDataPacketTypes we are interested in.  If you do
                // not register a listener for a particular data type, you will not
                // receive data packets of that type.
                muse.unregisterAllListeners();

                muse.registerConnectionListener(connectionListener);

                muse.registerDataListener(dataListener, MuseDataPacketType.EEG);


                muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);//other signal **

                muse.registerDataListener(dataListener, MuseDataPacketType.BETA_RELATIVE);

                muse.registerDataListener(dataListener, MuseDataPacketType.THETA_RELATIVE);

                muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_RELATIVE);

                muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_RELATIVE);

                //muse.registerDataListener(dataListener, MuseDataPacketType.);
                muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
                muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
                muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
                muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);

                mSocket.on("alphaDataEcho", onAlphaDataEcho);//start listening
                // Initiate a connection to the headband and stream the data asynchronously.
                muse.runAsynchronously();
                plot =true;
            }

        } else if (v.getId() == R.id.disconnect) {

            // The user has pressed the "Disconnect" button.
            // Disconnect from the selected Muse.
            if (muse != null) {
                muse.disconnect();
                mSocket.off("alphaDataEcho", onAlphaDataEcho);//stop listening
                plot = false;
            }

        } else if (v.getId() == R.id.pause) {

            // The user has pressed the "Pause/Resume" button to either pause or
            // resume data transmission.  Toggle the state and pause or resume the
            // transmission on the headband.
            if (muse != null) {
                dataTransmission = !dataTransmission;
                mEEGdata.setPausedRef(String.valueOf(!dataTransmission));// could use if you want to send to socket
                if(dataTransmission){
                    mSocket.on("alphaDataEcho", onAlphaDataEcho);//stop listening
                    plot =true;
                }
                else{
                    mSocket.off("alphaDataEcho", onAlphaDataEcho);//start listening
                    plot =false;
                }
                muse.enableDataTransmission(dataTransmission);
            }
        }
    }

    //--------------------------------------
    // Permissions

    /**
     * The ACCESS_COARSE_LOCATION permission is required to use the
     * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
     * On an Android 6.0 device, the following code will display 2 dialogs,
     * one to provide context and the second to request the permission.
     * On an Android device running an earlier version, nothing is displayed
     * as the permission is granted from the manifest.
     *
     * If the permission is not granted, then Muse 2016 (MU-02) headbands will
     * not be discovered and a SecurityException will be thrown.
     */
    private void ensurePermissions() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
            // the user to grant us the permission.

            DialogInterface.OnClickListener buttonListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which){
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    0);
                        }
                    };

            // This is the context dialog which explains to the user the reason we are requesting
            // this permission.  When the user presses the positive (I Understand) button, the
            // standard Android permission dialog will be displayed (as defined in the button
            // listener above).
            AlertDialog introDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_description)
                    .setPositiveButton(R.string.permission_dialog_understand, buttonListener)
                    .create();
            introDialog.show();
        }
    }


    //--------------------------------------
    // Listeners

    /**
     * You will receive a callback to this method each time a headband is discovered.
     * In this example, we update the spinner with the MAC address of the headband.
     */
    public void museListChanged() {
        final List<Muse> list = manager.getMuses();
        spinnerAdapter.clear();
        for (Muse m : list) {
            spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
        }
    }

    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     * @param p     A packet containing the current and prior connection states
     * @param muse  The headband whose state changed.
     */
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {

        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        final String status = p.getPreviousConnectionState() + " -> " + current;
        Log.i(TAG, status);

        // Update the UI with the change in connection state.
        handler.post(new Runnable() {
            @Override
            public void run() {

                final TextView statusText = (TextView) findViewById(R.id.con_status);
                statusText.setText(status);
                //connectedRef.setValue(status);//add
                mEEGdata.setConnectedRef(status);

                final MuseVersion museVersion = muse.getMuseVersion();
                final TextView museVersionText = (TextView) findViewById(R.id.version);
                // If we haven't yet connected to the headband, the version information
                // will be null.  You have to connect to the headband before either the
                // MuseVersion or MuseConfiguration information is known.
                if (museVersion != null) {
                    final String version = museVersion.getFirmwareType() + " - "
                            + museVersion.getFirmwareVersion() + " - "
                            + museVersion.getProtocolVersion();
                    museVersionText.setText(version);
                } else {
                    museVersionText.setText(R.string.undefined);
                }
            }
        });

        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());
            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;
            plot=false;
            //socket off??
        }
    }

    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     * @param p     The data packet containing the data from the headband (eg. EEG data)
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {

        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert(eegBuffer.length >= n);
                getEegChannelValues(eegBuffer,p);
                eegStale = true;
                break;
            case ACCELEROMETER:
                assert(accelBuffer.length >= n);
                getAccelValues(p);
                accelStale = true;
                break;

            case ALPHA_RELATIVE://add
                assert(alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer,p);
                alphaStale = true;
                break;
            case BETA_RELATIVE://add
                assert(betaBuffer.length >= n);
                getEegChannelValues(betaBuffer,p);
                betaStale = true;
                break;
            case THETA_RELATIVE://add
                assert(thetaBuffer.length >= n);
                getEegChannelValues(thetaBuffer,p);
                thetaStale = true;
                break;
            case DELTA_RELATIVE://add
                assert(deltaBuffer.length >= n);
                getEegChannelValues(deltaBuffer,p);
                deltaStale = true;
                break;
            case GAMMA_RELATIVE://add
                assert(gammaBuffer.length >= n);
                getEegChannelValues(gammaBuffer,p);
                gammaStale = true;
                break;

            case BATTERY://find out
            case DRL_REF:
            case QUANTIZATION:
            default:
                break;
        }
    }

    /**
     * You will receive a callback to this method each time an artifact packet is generated if you
     * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
     * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
     * @param p     The artifact packet with the data from the headband.
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
    }

    /**
     * Helper methods to get different packet values.  These methods simply store the
     * data in the buffers for later display in the UI.
     *
     * getEegChannelValue can be used for any EEG or EEG derived data packet type
     * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
     * of MuseDataPacketType for all of the available values.
     * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
     * getValue methods.
     */
    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }

    private void getAccelValues(MuseDataPacket p) {
        accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
        accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
        accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
    }


    //--------------------------------------
    // UI Specific methods

    /**
     * Initializes the UI of the example application.
     */
    private void initUI() {
        setContentView(R.layout.activity_main);
        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        Button disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);
        Button pauseButton = (Button) findViewById(R.id.pause);
        pauseButton.setOnClickListener(this);
        mDataEchoTx =(TextView) findViewById(R.id.textView1);
        mUserNameTx = (TextView)findViewById(R.id.userNameTx);
        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);
        mChart=(LineChart) findViewById(R.id.chart1);
    }

    /**
     * The runnable that is used to update the UI at 60Hz.
     *
     * We update the UI from this Runnable instead of in packet handlers
     * because packets come in at high frequency -- 220Hz or more for raw EEG
     * -- and it only makes sense to update the UI at about 60fps. The update
     * functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            if (eegStale) {
                updateEeg();
            }
            if (accelStale) {
                updateAccel();
            }
            if (alphaStale) {
                updateAlpha();//add
            }
            if (betaStale) {
                updateBeta();//add
            }
            if (thetaStale) {
                updateTheta();//add
            }
            if (deltaStale) {
                updateDelta();//add
            }
            if (gammaStale) {
                updateGamma();//add
            }
            //posible place to send socket data
            handler.postDelayed(tickUi, 1000 / 60);
        }
    };

    private void updateElem(double[] buffer,EEGsignal signal){
        signal.setElem1(String.format("%6.2f", buffer[0]));
        signal.setElem2(String.format("%6.2f", buffer[1]));
        signal.setElem3(String.format("%6.2f", buffer[2]));
        signal.setElem4(String.format("%6.2f", buffer[3]));
    }
    private void updateAccel() {
        TextView acc_x = (TextView)findViewById(R.id.acc_x);
        TextView acc_y = (TextView)findViewById(R.id.acc_y);
        TextView acc_z = (TextView)findViewById(R.id.acc_z);
        acc_x.setText(String.format("%6.2f", accelBuffer[0]));
        acc_y.setText(String.format("%6.2f", accelBuffer[1]));
        acc_z.setText(String.format("%6.2f", accelBuffer[2]));
    }

    private void updateEeg() {
        TextView tp9 = (TextView)findViewById(R.id.eeg_tp9);
        TextView fp1 = (TextView)findViewById(R.id.eeg_af7);
        TextView fp2 = (TextView)findViewById(R.id.eeg_af8);
        TextView tp10 = (TextView)findViewById(R.id.eeg_tp10);
        tp9.setText(String.format("%6.2f", eegBuffer[0]));
        fp1.setText(String.format("%6.2f", eegBuffer[1]));
        fp2.setText(String.format("%6.2f", eegBuffer[2]));
        tp10.setText(String.format("%6.2f", eegBuffer[3]));


        //update EEG object with new data
        updateElem(eegBuffer,mEEGdata.getEEG_DataRef());
    }
    //--------------------EMIT AN ALPHA SIGNAL TO SOCKET-------------------------
    private void updateAlpha() { //add
        TextView Aelem1 = (TextView)findViewById(R.id.Aelem1);
        Aelem1.setText(String.format("%6.2f", alphaBuffer[0]));
        TextView Aelem2 = (TextView)findViewById(R.id.Aelem2);
        Aelem2.setText(String.format("%6.2f", alphaBuffer[1]));
        TextView Aelem3 = (TextView)findViewById(R.id.Aelem3);
        Aelem3.setText(String.format("%6.2f", alphaBuffer[2]));
        TextView Aelem4 = (TextView)findViewById(R.id.Aelem4);
        Aelem4.setText(String.format("%6.2f", alphaBuffer[3]));
       // alphaRef.child("elem4").setValue(String.format("%6.2f", alphaBuffer[3]));

        updateElem(alphaBuffer,mEEGdata.getAlphaRef());
        //--------#######################----------------------------
        //add other signal and make sure to put the receiver on the server side
        mSocket.emit("alphaData",alphaBuffer[0]);
    }

    private void updateBeta() { //add
        TextView Belem1 = (TextView)findViewById(R.id.Belem1);
        Belem1.setText(String.format("%6.2f", betaBuffer[0]));
        //betaRef.child("elem1").setValue(String.format("%6.2f", betaBuffer[0]));
        TextView Belem2 = (TextView)findViewById(R.id.Belem2);
        Belem2.setText(String.format("%6.2f", betaBuffer[1]));
        //betaRef.child("elem2").setValue(String.format("%6.2f", betaBuffer[1]));
        TextView Belem3 = (TextView)findViewById(R.id.Belem3);
        Belem3.setText(String.format("%6.2f", betaBuffer[2]));
        //betaRef.child("elem3").setValue(String.format("%6.2f", betaBuffer[2]));
        TextView Belem4 = (TextView)findViewById(R.id.Belem4);
        Belem4.setText(String.format("%6.2f", betaBuffer[3]));
        //betaRef.child("elem4").setValue(String.format("%6.2f", betaBuffer[3]));
        updateElem(betaBuffer,mEEGdata.getBetaRef());
    }

    private void updateTheta() { //add
        TextView Telem1 = (TextView)findViewById(R.id.Telem1);
        Telem1.setText(String.format("%6.2f", thetaBuffer[0]));
        //thetaRef.child("elem1").setValue(String.format("%6.2f", thetaBuffer[0]));
        TextView Telem2 = (TextView)findViewById(R.id.Telem2);
        Telem2.setText(String.format("%6.2f", thetaBuffer[1]));
       // thetaRef.child("elem2").setValue(String.format("%6.2f", thetaBuffer[1]));
        TextView elem3 = (TextView)findViewById(R.id.Telem3);
        elem3.setText(String.format("%6.2f", thetaBuffer[2]));
       // thetaRef.child("elem3").setValue(String.format("%6.2f", thetaBuffer[2]));
        TextView elem4 = (TextView)findViewById(R.id.Telem4);
        elem4.setText(String.format("%6.2f", thetaBuffer[3]));
       // thetaRef.child("elem4").setValue(String.format("%6.2f", thetaBuffer[3]));

        updateElem(thetaBuffer,mEEGdata.getThetaRef());
    }
    private void updateDelta() { //add
        TextView Delem1 = (TextView)findViewById(R.id.Delem1);
        Delem1.setText(String.format("%6.2f", deltaBuffer[0]));
        //deltaRef.child("elem1").setValue(String.format("%6.2f", deltaBuffer[0]));
        TextView Delem2 = (TextView)findViewById(R.id.Delem2);
        Delem2.setText(String.format("%6.2f", deltaBuffer[1]));
       // deltaRef.child("elem2").setValue(String.format("%6.2f", deltaBuffer[1]));
        TextView Delem3 = (TextView)findViewById(R.id.Delem3);
        Delem3.setText(String.format("%6.2f", deltaBuffer[2]));
        //deltaRef.child("elem3").setValue(String.format("%6.2f", deltaBuffer[2]));
        TextView Delem4 = (TextView)findViewById(R.id.Delem4);
        Delem4.setText(String.format("%6.2f", deltaBuffer[3]));
        //deltaRef.child("elem4").setValue(String.format("%6.2f", deltaBuffer[3]));
        updateElem(deltaBuffer,mEEGdata.getDeltaRef());
    }

    private void updateGamma() { //add
        TextView Gelem1 = (TextView)findViewById(R.id.Gelem1);
        Gelem1.setText(String.format("%6.2f", gammaBuffer[0]));
       // gammaRef.child("elem1").setValue(String.format("%6.2f", gammaBuffer[0]));
        TextView Gelem2 = (TextView)findViewById(R.id.Gelem2);
        Gelem2.setText(String.format("%6.2f", gammaBuffer[1]));
       // gammaRef.child("elem2").setValue(String.format("%6.2f", gammaBuffer[1]));
        TextView Gelem3 = (TextView)findViewById(R.id.Gelem3);
        Gelem3.setText(String.format("%6.2f", gammaBuffer[2]));
       // gammaRef.child("elem3").setValue(String.format("%6.2f", gammaBuffer[2]));
        TextView Gelem4 = (TextView)findViewById(R.id.Gelem4);
        Gelem4.setText(String.format("%6.2f", gammaBuffer[3]));
        //gammaRef.child("elem4").setValue(String.format("%6.2f", gammaBuffer[3]));

        updateElem(gammaBuffer,mEEGdata.getGammaRef());
    }

    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.
    class MuseL extends MuseListener {
        final WeakReference<MainActivity> activityRef;

        MuseL(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged() {
            activityRef.get().museListChanged();
        }
    }

    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<MainActivity> activityRef;

        ConnectionListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<MainActivity> activityRef;

        DataListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p, muse);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            activityRef.get().receiveMuseArtifactPacket(p, muse);
        }
    }


    //-------------GRAPH CODE-----------------

    private void drawGraph(){
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setText("Real Time Engagment DATA PLOT!");

        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(false);

        mChart.setBackgroundColor(Color.BLACK);//TEST


        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        Legend l =mChart.getLegend();

        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl =mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(true);
    }

    //holds a runnalbe for how often to plt new data
    private void startPlot() {
        if(thread != null){
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    plotData =true;
                    try{
                        Thread.sleep(50);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    //adds new data point to the graph
    private void addEntry(float sensorEvent) {
        LineData data = mChart.getData();

        if(data != null){
            ILineDataSet set = data.getDataSetByIndex(0);
            if(set == null){
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(),sensorEvent),0);//val from server
           // mSocket.emit("acclData",sensorEvent);
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();
            //mChart.setMaxVisibleValueCount(150);
            mChart.setVisibleXRangeMaximum(150);
            mChart.moveViewToX(data.getEntryCount());
        }
    }
    //create line data
    private LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null,"Alpha ch1.");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(8f);
        set.setLineWidth(Color.MAGENTA);
        set.setHighlightEnabled(false);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.3f);
        return set;
    }
    //-------------------------------------------

    /* start log in activity for result*/
    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            //getActivity().finish();
            return;
        }

        mUsername = data.getStringExtra("username");
        mUserNameTx.setText("Hello "+ mUsername);
        int numUsers = data.getIntExtra("numUsers", 1);

        //addLog(getResources().getString(R.string.message_welcome));
        //addParticipantsLog(numUsers);
    }



    @Override
    protected void onPause() {
        super.onPause();

        // It is important to call stopListening when the Activity is paused
        // to avoid a resource leak from the LibMuse library.
        manager.stopListening();
        mSocket.off("alphaDataEcho", onAlphaDataEcho);
        plot = false;
        //mSocket.on
        if(thread != null){
            thread.interrupt();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSocket.on("alphaDataEcho", onAlphaDataEcho);
        plot = true;
    }



    @Override
    protected void onDestroy() {
        //mSensorManager.unregisterListener(MainActivity.this);
        thread.interrupt();
        mSocket.disconnect();
        manager.stopListening();
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewMessage);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("user left", onUserLeft);
        mSocket.off("alphaDataEcho", onAlphaDataEcho);
        plot =false;
        super.onDestroy();
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!isConnected) {
                        if(null!=mUsername)
                            mSocket.emit("add user", mUsername);
                        Toast.makeText(getApplicationContext(),
                                "connected", Toast.LENGTH_LONG).show();
                        isConnected = true;
                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "diconnected");
                    isConnected = false;
                    Toast.makeText(getApplicationContext(),
                            R.string.disconnect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "Error connecting");
                    Toast.makeText(getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }

                    //do something
                }
            });
        }
    };


    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }

                    //handle if you want
                }
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }

                    //handle if you want
                }
            });
        }
    };

    private Emitter.Listener onAlphaDataEcho = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    String point;
                    try {
                        point = data.getString("point");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }
                    mDataEchoTx.setText(point);
                    if(plotData && plot){
                        if(point.equals("NaN")){
                            addEntry(0);
                        }
                        else{
                            addEntry(Float.parseFloat(point));
                        }
                        plotData =false;
                    }
                }
            });
        }
    };

}
