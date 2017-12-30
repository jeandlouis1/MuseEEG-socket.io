var express = require('express');
var socket = require('socket.io');
var fs = require('fs');
    
     

// app setup
var app = express();

//make sure the port is the same as on the app
var server = app.listen(3000,function(){
	console.log('listening to requests on port 3000');
});

var numUsers =0;
//static files
app.use(express.static('public'));

//socket setup
var io = socket(server);

//notify connection
io.on('connection',function(socket){
	console.log('made connection: ',socket.id);
	console.log('number of users:',++numUsers);


	var addedUser = false;

	socket.on('add user', function (username) {
    if (addedUser) return;

    // we store the username in the socket session for this client
    socket.username = username;
    //++numUsers;
    addedUser = true;
    socket.emit('login', {
      numUsers: numUsers
    });
    console.log('added user');
    // echo globally (all clients) that a person has connected
    socket.broadcast.emit('user joined', {
      username: socket.username,
      numUsers: numUsers
    });
  });


  //add other listenners for other signals if you want
	socket.on("alphaData",function(data){
    //send back to requesting client

    //calling function in adroid app to plot the data point on android graph
		socket.emit("alphaDataEcho",{
      point: data
    });
		//console.log(data); for testing
	});



  /*
    EEG CODE
  */


    //connected to headset

    socket.on('disconnect',function(){
    	console.log('loss a connection',socket.id);
		  numUsers--;
      console.log('number of users:',numUsers);
    })
});










/*

engagment equation (Beta/(Alpha + Theta)


//send message only to sender-client

socket.emit('message', 'check this');

//or you can send to all listeners including the sender

io.emit('message', 'check this');

//send to all listeners except the sender

socket.broadcast.emit('message', 'this is a message');

//or you can send it to a room

socket.broadcast.to('chatroom').emit('message', 'this is the message to all');


a good start
https://github.com/iamshaunjp/websockets-playlist/tree/lesson-2


*/