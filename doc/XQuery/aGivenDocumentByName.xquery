let $container := "testmachine_ftp.dbxml"
let $docN := "testmachine_ftp-1141883685516.xml"
let $url := concat( $container, '/', $docN)

return doc($url)