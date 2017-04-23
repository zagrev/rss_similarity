library(igraph)

#setwd('C:/Users/Steve Betts/workspace-school/betts-ceg7370-proj4.zip_expanded/com.minethurn.cs7210.rss_similarity')
setwd('C:/Users/Steve Betts/')

# the data format is:
# 1 url.earlier, 
# 2 url.later, 
# 3 title1, 
# 4 title2, 
# 5 similarity, 
# 6 time difference
# 7 time of earlier article
# 8 time of later article
F_START_URL <- 1
F_START_TITLE <- 3
F_START_TIME <- 7

F_END_URL <- 2
F_END_TITLE <- 4
F_END_TIME <- 8

F_SIMILARITY <- 5
F_TIME_DISTANCE <- 6

edgeList <- read.csv("distance-wiretap.csv",header=F) 
index <- order(edgeList[,F_START_TIME])
indexLength <- length(index)

edgeList <- as.matrix(edgeList)

# NOTE that we order the edges by START_TIME. this only works because start and
#      end are right next to each other in the input file
g <- graph.edgelist(edgeList[index,F_START_URL:F_END_URL], directed = T) 

# We then add the edge weights to this network
weight=as.numeric(edgeList[,F_TIME_DISTANCE])
width=(as.numeric(edgeList[,F_SIMILARITY])*10)-5

V(g)$label.cex=0.5
E(g)$weight=weight
E(g)$start = edgeList[,F_START_TIME]
E(g)$end = edgeList[,F_END_TIME]

l <- layout.fruchterman.reingold(g)

#palette = colorRampPalette(c('red','green'))

V(g)$color <- "orange"
V(g)$color[which(degree(g, mode="in")==0)] <- "green"
V(g)$color[which(degree(g, mode="in")>0 && degree(g, mode="out")==0)] <- "red"

# find the name of the destination node with the latest time
latestNode <- get.edges(g, length(E(g)))[2]
V(g)$color[latestNode] <- "red"


## Some code to make an animation... 
edgeCount <- length(index)


#maxtime = as.numeric(edgeList[index[indexLength],F_END_TIME])
#mintime = as.numeric(edgeList[index[1],F_START_TIME])
#interval <- (maxtime-mintime)/30

animation::saveGIF({
#  for (i in seq(mintime+interval, maxtime, interval))
  for (i in seq(2, edgeCount, 1))
  {
#    pg <- graph.data.frame(edgeList[which(E(g)$start < i),F_START_URL:F_END_URL], vertices=V(g)$name, directed = T) 
    pg <- graph.data.frame(edgeList[1:i,F_START_URL:F_END_URL], vertices=V(g)$name, directed = T) 
    
    V(pg)$color <- "orange"
    V(pg)$color[which(degree(pg, mode="in")==0 && degree(pg, mode="out") > 0)] <- "green"
    V(pg)$color[which(degree(pg, mode="in")>0 && degree(pg, mode="out")==0)] <- "red"
    
    plot(pg,layout=l,vertex.size=5,rescale=T)
#    plot(pg,layout=l,vertex.size=5,rescale=F,edge.arrow.size=0.25)
  }
}, movie.name="er.gif", interval=0.1, ani.width=1000, ani.height=1000, ani.loop=2)

#plot(g,layout=layout.fruchterman.reingold,edge.width=(100000 - E(g)$weight)/10000,vertex.size=5)
#plot(g,layout=layout.fruchterman.reingold,vertex.size=5,edge.width=width,rescale=T)
