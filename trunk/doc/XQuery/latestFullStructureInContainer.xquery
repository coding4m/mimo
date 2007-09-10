
(:
     Gets the latest full structure stored in the database

     Returns a full structure (mirror) with the latest timestamp (max number of seconds since epoch).
     If many nodes are max, take the first one (it should not happen).
 :)
let $repository := 'testmachine_ftp.dbxml'
let $latestTimestamp := max(xs:dateTime(collection($repository)/mirror/@checkoutTime))
return collection($repository)[mirror/@checkoutTime = $latestTimestamp]