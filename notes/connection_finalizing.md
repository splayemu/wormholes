## Incode Notes
Properties of a connection
between two rooms
persists until one of the two browser tabs closes

Creating and Finalizing a connection
user actions to get a connection
user clicks on center room
user clicks on outside room
  client1 sends to remote
  remote initializes connection
  client2 comes up in new tab (does javascript run on a tab in the background? (yes))
  client2 initializes websocket (but how does server know that the new client is the correct one?)
    first thing each client can do is send down an "finalize-connection" mutation with the room id
    if there is a connection waiting for a specific room, finalize connection can finalize it

Finalized Connections
Clicking on a wormhole with a finalized connection does nothing
The only way to break the wormhole is to close one of the browser tabs
For that we need to listen to sente's events
How are we going to tie a sente "end event" to a specific connection?
  Can we add the pathom env above the sente layer?
How are we going to trigger a backend pathom mutation?
 


;; TODO:
- [x] flesh out a connection model as well
- [x] connection storage
- [x] creating finalized connections
- [x] updating wormhole state to include finalized connections
- [ ] breaking connections
- [ ] pushing broken connections to the client
