# Server to Client

We need to update our other clients, or browser tabs, with the updated server state. Fulcro provides a way to return values from a mutation, but that only returns to the client that made the request.

## Pushing from the server to the client

Fulcro provides a `:push` key on the environment when using websockets that hooks into to the sente push function. We add it into the pathom parser like:

```
(defn api-parser
  ([query] (pathom-parser {} query))
  ([env query]
   (pathom-parser {...
                   :push (:push env)}
     query)))
```

Then to explore we can write some REPL code which pulls the `:push` key (representing a function) off of the pathom environment. The push function takes a user-id which sente uses to find every connected client (browser tab) and send the value over each websocket connection.

```
(let [user-id (-> tenv :user :user/id)
        user-room-id [user-id :room.id/down]
        data (get-in @state/room-table user-room-id)]
    ((:push tenv) user-id [:api/server-push
                           {:topic :merge
                            :msg {:message/topic :merge
                                  :merge/component 'app.ui/Room
                                  :merge/data data}}]))
```

Meanwhile on the frontend, we need to handle the pushed websocket messages. Fulcro provides a hook to do that using the `:push-handler` option when creating the websocket remote.

You can push any serializable values over the websocket, and here I am pushing Fulcro's suggested `{:topic ... :msg ...}`. 
Here I just want each client to be updated to the current server value. Fulcro provides the `merge!` and `merge-component!` functions in `com.fulcrologic.fulcro.algorithms.merge` to do just that [1].
That makes it easy to call the message `:merge` and preface the data package with the `:merge` qualified keywords.

```
(def lame-symbol-mapping
  {'app.components/Room app.components/Room})

(defn merge-handler [{:keys [merge/component merge/data]}]
  (js/console.log "merge-handler:" component data)
  (fulcro-merge/merge-component!
    app
    (lame-symbol-mapping component)
    data))

(defn push-handler [{:keys [topic msg]}]
  (case topic
    :merge (merge-handler msg)
    :else (js/console.error "push-handler: topic" topic "does not exist")))
```

Notice some weirdness I do where I look up the component from the component symbol passed from the server. Fulcro components are not serializable so perhaps that's necessary.

## Difficulties

Fulcro's merge functions will remove keys that exist in the query of the component being merged in that don't exist in the incoming data.
This means I can't merge in a single data change and instead need to merge in the whole updated entity.
This now means when I want to broadcast entity updates to my clients in a mutation, I need to query the full entity data just like my clients would expect.
I use Pathom to resolve the queries into their representative UI tree like structure, so it makes sense to use that here.
The next step is how do I get the queries on the backend.

The three ways I thought of were:
1. Query for all subkeys using the Pathom `*` query.

2. Pass the query down as a part of the mutation.
This doesn't make much sense since I don't want to couple the server->client broadcast updates with a query defined in the client. Also some of my mutations on the client are not kicked off inside of a component. I'd rather have the backend mutation smart enough to figure out what to update on each client.

3. Convert cljs Fulcro components into cljc 
Fulcro has the nice property of defining much of it's code in cljc files. One benefit of defining the components in cljc is that you can share the query between the front and backends. Another benefit is server side rendering (which isn't relevant right now).



1 - http://book.fulcrologic.com/fulcro3/#_the_central_functions_transact_and_merge
