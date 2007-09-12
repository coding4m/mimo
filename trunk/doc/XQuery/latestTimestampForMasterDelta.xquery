max(for $date in collection('master.dbxml')/delta/@timestamp  return xs:dateTime($date))

