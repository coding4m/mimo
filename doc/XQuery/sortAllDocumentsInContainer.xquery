(:
   Gets in chronological order all documents (full structs and delta) 
   fromt the database

:)
let $container := "testmachine_ftp.dbxml"

let $deltas := collection($container)/delta
let $fulls  := collection($container)/mirror

for $a in $deltas union $fulls
 order by  xs:decimal($a/@timestamp)
return $a