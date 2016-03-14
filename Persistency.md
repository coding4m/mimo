# Mirrored files #

option 1: stored in a MirrorStructure (a dom4j-based DOM  tree) -> easy to serialize as XML and from there into any format via XSLT
Option 1 enhanced: the XML documents are stored within a XML database such as eXist or Apache XIndice (both Open Source and following the XML:DB standard) or Berkely DB XML which offers a mature and performant infrastructure
option 2: stored in a relational database eventually via data access object layer (Hibernate)

The nature of the mirror structure is a tree. Therefore it seems natural to use a tree-based structure to store it. Powerful tools like XPATH and XQUERY will come handy at the time of searching the database.

Another advantage of XML is the print-ready capabilities of any document by using XSLT. For example, Mozilla Firefox consumes XML holding a XSL sheet reference and it happily performs the transformation into XHTML.