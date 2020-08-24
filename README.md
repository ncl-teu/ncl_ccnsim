# ncl_ccnsim
CCN (Contents-Centric Networking) Simulator
# Manuals (in Japanese)
- [here](https://github.com/ncl-teu/ncl_ccnsim/tree/master/manuals). 
# How to use
- Double click `ccnrun.bat' for windows or run `./ccnrun.sh` for Linux. 
- The configuration file is **ccn.properties**. 
- The all log if written to `ccn/ccnlog.csv` as overwritten mode. The format of the log is as follows: 
~~~
TIMESATMP, type, prefix,DataSize(MB), StartTime,FinishTime,duration(ms),Interest_senderID,Data(Cache)holdingNodeID, Hop#,# of SharedConnections,ContentsFound/Not,ByBC?,Memo
~~~
- As for **type** field, we have 
- 1 Interest is arrived at the **original server** that has the original content (non-cached data). 
- 2 Original data is returned to the client from the **orignal server**. 
- 13 Interest is arrived at the cache holding router (i.e., cach hit). 
- 3 Cached data is return to the client from the cache holding router. 
- 4 Interest is arrived at the router by BreadCrumbs pointer.
- 5 A new CCN router joined. 
- 6 An CCN router leaves. 

# Cache algorithm
- Since the simulator is based on Java, please create a new class that extends `net.gripps.ccn.caching.BaseCachingAlgorithm`. 
- Example caching algorithm is implemented as **OnPathCaching.java**. Please refer to it. 
## How to create a new caching algorithm
1. In ccn.properties, please set `ccn_caching_no=0` to `ccn_caching_no=1`(i.e., change the used index no). And please set `ccn_caching_allnum=1` to `ccn_caching_allnum=2` (i.e., increment it as the number of candidate caching algorithms). 
2. In `ccn.net.gripps.ccn.core.CCNRouter', please change as follows: 
Before: 
~~~
this.cachings[0] = new OnPathCaching();
this.cachings[1] = new NoCaching();
this.usedCaching = this.cachings[CCNUtil.ccn_caching_no];
~~~

After:
~~~
this.cachings[0] = new OnPathCaching();
this.cachings[1] = new NoCaching();
this.cachings[2] = new NEW_CLASS();
this.usedCaching = this.cachings[CCNUtil.ccn_caching_no];
~~~
# FIB routing algorithm
- Please create a new class that extends `net.gripps.ccn.fibrouting.BaseRouting`. 
- Currently, we have **ChordDHTRouting**, **ChordDHTBARouging(Chord with Barabasi-Albert Network)**, and **LongestMatchRouting**. 
## How to create a new routing algorithm
1. In ccn.properties, please set `ccn_routing_no=0` to `ccn_routing_no=1`(i.e., change the used index no). And please set `ccn_routing_allnum=1` to `ccn_routing_allnum=2` (i.e., increment it as the number of candidate caching algorithms). 
2. In `ccn.net.gripps.ccn.process.CCNMgr', please change as follows: 
Before: 
~~~
this.routings[0] = new ChordDHTRouting(this.nodeMap, this.routerMap);
this.usedRouting = this.routings[CCNUtil.ccn_routing_no];
~~~

After:
~~~
this.routings[0] = new ChordDHTRouting(this.nodeMap, this.routerMap);
this.routings[1] = new NEW_CLASS(this.nodeMap, this.routerMap);
~~~
