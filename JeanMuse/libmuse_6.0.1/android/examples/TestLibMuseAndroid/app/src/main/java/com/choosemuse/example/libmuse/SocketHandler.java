package com.choosemuse.example.libmuse;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by Jean on 12/24/2017.
 */

//singelton
class SocketHandler {
    private static Socket ourInstance;

    static synchronized Socket getInstance() {
        if(ourInstance == null){
            try {
                ourInstance = IO.socket(Constants.SERVER_URL);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        return ourInstance;
    }

    private SocketHandler() {

    }
}

