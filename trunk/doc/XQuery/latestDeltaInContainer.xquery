(:
     Returns a delta with the latest timestamp (max number of seconds since epoch).
     If many nodes are max, take the first one (it should not happen).
 :)
let $latestTimestamp := max(collection('testmachine_ftp.dbxml')/delta/@timestamp)
return collection('testmachine_ftp.dbxml')[delta/@timestamp = $latestTimestamp][1] 
