(:
    Gets all checkout time (timestamp) and sorts them

:)
for $a in distinct-values(collection("testmachine_ftp.dbxml")/mirror/@checkoutTime)
    order by  xs:decimal($a)
    return $a 