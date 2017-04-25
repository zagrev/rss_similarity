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

type="anim"
type="pdf"
type="svg"

filename = "distance.aclu.csv"
#filename = "distance.clinton-uranium.csv"
#filename = "distance.csv.csv"
#filename = "distance.dragnet.csv"
#filename = "distance.evelyn-farkas.csv"
#filename = "distance.flag.csv"
#filename = "distance.kaepernick.csv"
#filename = "distance.katy.csv"
#filename = "distance.labeouf.csv"
#filename = "distance.nsa.csv"
#filename = "distance.rice.csv"
#filename = "distance.seth-rich.csv"
#filename = "distance.susan-rice.csv"
#filename = "distance.syria-false.csv"
#filename = "distance.tax.csv"
#filename = "distance.titles-flag-ben.csv"
#filename = "distance.vault-7.csv"
#filename = "distance.wiretap.csv"
#filename = "distance.youtube.csv"
#filename = "distance.deep-state.csv"
#filename = "distance.maddow-tax.csv"
#filename = "distance.syria-missile.csv"

edgeList <- read.csv(filename,header=F) 
edgeList <- as.matrix(edgeList)

nodeTypes <- read.csv("websiteTypes.csv", header=F)
nodeTypes <- as.matrix(nodeTypes)

index <- order(edgeList[,F_START_TIME])
indexLength <- length(index)

# NOTE that we order the edges by START_TIME. this only works because start and
#      end are right next to each other in the input file
g <- graph.edgelist(edgeList[index,F_START_URL:F_END_URL], directed = T) 

# We then add the edge weights to this network
maxTimeDifference = as.numeric(which.max(edgeList[,F_TIME_DISTANCE]))
weight=maxTimeDifference - as.numeric(edgeList[,F_TIME_DISTANCE])
width=(as.numeric(edgeList[,F_SIMILARITY])*10)-5

V(g)$label.cex=0.5
E(g)$weight=weight
E(g)$start = edgeList[,F_START_TIME]
E(g)$end = edgeList[,F_END_TIME]

l <- layout.fruchterman.reingold(g)

colorNodes <- function(Xgraph)
{
  V(Xgraph)$color <- "black" #"orange"
  V(Xgraph)$color[which(degree(Xgraph, mode="in")==0 && degree(Xgraph, mode="out") > 0)] <- "green"
  V(Xgraph)$color[which(degree(Xgraph, mode="in")>0 && degree(Xgraph, mode="out")==0)] <- "red"
}

edgeCount <- length(index)


maxtime = as.numeric(edgeList[index[indexLength],F_END_TIME])
mintime = as.numeric(edgeList[index[1],F_START_TIME])
interval <- (maxtime-mintime)/30

if (type == "pdf")
{
  pdf(paste0(filename,".pdf"), width = 8, height = 11, pointsize = 6)
  pg <- graph.data.frame(edgeList[1:indexLength,F_START_URL:F_END_URL], vertices=V(g)$name, directed = T) 
  
  colorNodes(pg)
  
  plot(pg,layout=l,vertex.size=5,rescale=T, edge.arrow.size=.7)
  dev.off()
}
if (type=="anim")
{
  animation::saveGIF(
  {
  #  for (i in seq(mintime+interval, maxtime, interval))
    for (i in seq(2, edgeCount, 1))
    {
  #    pg <- graph.data.frame(edgeList[which(E(g)$start < i),F_START_URL:F_END_URL], vertices=V(g)$name, directed = T) 
      pg <- graph.data.frame(edgeList[1:i,F_START_URL:F_END_URL], vertices=V(g)$name, directed = T) 
      
      colorNodes(pg)
      
      plot(pg,layout=l,vertex.size=5,rescale=T, edge.arrow.size=1)
  #    plot(pg,layout=l,vertex.size=5,rescale=F,edge.arrow.size=0.25)
    }
  }, movie.name=paste0(filename,".gif"), interval=0.1, ani.width=1000, ani.height=1000, clean = F)
}
if (type == "svg")
{
  svg(paste0(filename,".svg"), width = 6, height = 6, pointsize = 6)
  pg <- graph.data.frame(edgeList[1:indexLength,F_START_URL:F_END_URL], vertices=V(g)$name, directed = T) 
  
  colorNodes(pg)
  V(pg)$type <- nodeTypes[match(V(pg)$name, nodeTypes[,1]),2]
  V(pg)$color <- ifelse(V(pg)$type=="Alt", "orange", "slategray3")
  
  
  plot(pg,layout=l,vertex.size=5,rescale=T, edge.arrow.size=.7)
  dev.off()
}

#plot(g,layout=layout.fruchterman.reingold,edge.width=(100000 - E(g)$weight)/10000,vertex.size=5)
#plot(g,layout=layout.fruchterman.reingold,vertex.size=5,edge.width=width,rescale=T)
