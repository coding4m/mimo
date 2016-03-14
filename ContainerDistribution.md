# Container distribution #

“In BDB XML you store your XML Documents in containers. A container is a file on disk that contains all the data associated with your documents, including metadata and indices.” (BDB XML documentation)

There are tens of mirrors, we need a meaningfull organization of the data in different collections.

Mirror A: a collection dedicated to a mirror A, containing complete XML representation of its content, plus some delta files which represent changes from the last complete snapshots (to save in space)
Mirror list: index of the mirrors and collections/containers
Latest diffs: precomputed diffs between the master and some mirror Lambda