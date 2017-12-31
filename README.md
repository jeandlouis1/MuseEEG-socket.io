# MuseEEG-socket.io 

Sending Electroencephalography(EEG) data over Socket.io using Muse EEG headset

This is an example built on the muse sdk example code and socket.io java client code, modified to communicate EEG data to a web server using socket.io.

## Installation
create new Constants class due to git ignore for the Constants file.
place your ip for port 3000 for socket .io in the android javafile Constants

### change public class ConstantsExample to public class Constants

```java
public class Constants{
    //replace with your ip/server address :portserver is listening on
    public static final String CHAT_SERVER_URL = "http://your ip addres:3000";
}
```

1. If you are running the the server on your local computer make sure your mobile device is on the same network 
2. Your network setting is set to private instead of public

- Run the index.js file on the server
- launch the app

## Usage

### Server Side snipit

```javascript
//add other listenners for other signals if you want
	socket.on("alphaData",function(data){
    //send back to requesting client

    //calling function in adroid app to plot the data point on android graph
		socket.emit("alphaDataEcho",{
      point: data
    });
		//console.log(data); for testing
	});
```
		
### Android Side

#### emit message from client
```java
mSocket.emit("alphaData",alphaBuffer[0]);
//or 
//perfered
//use the built object wich holds all the main eeg vals
mSocket.emit("alphaData",mEEGdata.getAlphaRef().getelem1());	
```

#### recieve message from server 

```java
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
					//plot data if plot is true
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
```


## Misc	

### Problems faced
 - [cannot pass socket connection between activities in android, so  I used a singleton pattern](http://grokbase.com/t/gg/android-developers/124g28kxsx/socket-sharing-between-activities)

 - [connecting physical android device to PC](https://stackoverflow.com/questions/4779963/how-can-i-access-my-localhost-from-my-android-device)

### Extra reff
Main socket.io java client git example
(https://github.com/socketio/socket.io-client-java)
mpandroid chart
(https://www.youtube.com/watch?v=QEbljbZ4dNs)
Socket.io notes/playlist
(https://drewww.github.io/socket.io-benchmarking/)
 
