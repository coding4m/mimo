
let $latestFullStructTimestamp := max(collection('testmachine_ftp.dbxml')/mirror/@checkoutTime)
return collection('testmachine_ftp.dbxml')[delta/@timestamp > $latestFullStructTimestamp]