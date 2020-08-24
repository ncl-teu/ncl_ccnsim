# ncl_ccnsim
CCN (Contents-Centric Networking) Simulator
# Manuals (in Japanese)
- [here](https://github.com/ncl-teu/ncl_ccnsim/tree/master/manuals). 
# How to use
- Please build all sources using **ant**. Then you can build by `ant build` in the home directory. 
- Double click `ccnrun.bat` for windows or run `./ccnrun.sh` for Linux. 
- The configuration file is **ccn.properties** [click here](https://github.com/ncl-teu/ncl_ccnsim/blob/master/ccn.properties)
- The all log is written to `ccn/ccnlog.csv` as overwritten mode. The format of the log is as follows: 
~~~
TIMESATMP, type, prefix,DataSize(MB), StartTime,FinishTime,duration(ms),Interest_senderID,Data(Cache)holdingNodeID, Hop#,# of SharedConnections,ContentsFound/Not,ByBC?,Memo
~~~
- As for **type** field, we have the following value:
- 1: Interest is arrived at the **original server** that has the original content (non-cached data). 
- 2: Original data is returned to the client from the **orignal server**. 
- 13: Interest is arrived at the cache holding router (i.e., cach hit). 
- 3: Cached data is return to the client from the cache holding router. 
- 4: Interest is arrived at the router by BreadCrumbs pointer.
- 5: A new CCN router joined. 
- 6: An CCN router leaves. 

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
# BreadCrumbs algorithm
- Please create a new class that extends `net.gripps.ccn.breadcrumbs.BaseBreadCrumbsAlgorithm`. 
- Currently, we have **BreadCrumbsAlgorithm**. 
## How to create a new BC algorithm
1. In ccn.properties, please set `ccn_bc_allnum=2` to `ccn_bc_allnum=3`(i.e., change the used index no). And please set `ccn_bc_enable=1` to `ccn_bc_enable=2` (i.e., increment it as the number of candidate BC algorithms). 
2. In `ccn.net.gripps.ccn.core.CCNRouter', please change as follows: 

Before: 
~~~
 this.bcs[0] = new NoBreadCrumbsAlgorithm();
 this.bcs[1] = new BreadCrumbsAlgorithm();
 this.usedBC = this.bcs[CCNUtil.ccn_bc_enable];
~~~

After:
~~~
this.bcs[0] = new NoBreadCrumbsAlgorithm();
this.bcs[1] = new BreadCrumbsAlgorithm();
this.bcs[2] = new NEW_CLASS()
~~~
# Churn resilience algorithm in CCN
- Please create a new class that extends `net.gripps.ccn.churn.BaseChurnResilienceAlgorithm`. 
- Currently, we have **ChordDHTCRAlgorithm**. 
## How to create a new Churn Resilience (CR) algorithm
1. In ccn.properties, please set `ccn_churn_enable=0` to `ccn_churn_enable=2`(i.e., change the used index no). And please set `ccn_churn_allnum=2` to `ccn_churn_allnum=3` (i.e., increment it as the number of candidate CR algorithms). 
2. In `ccn.net.gripps.ccn.process.CCNMgr', please change as follows: 

Before: 
~~~
this.churns = new BaseChurnResilienceAlgorithm[CCNUtil.ccn_churn_allnum];
this.churns[0] = new NoChurnAlgorithm(this.usedRouting);
this.churns[1] = new ChordDHTCRAlgorithm((ChordDHTRouting)this.usedRouting);
this.usedChurn = this.churns[CCNUtil.ccn_churn_enable];
~~~

After:
~~~
this.churns = new BaseChurnResilienceAlgorithm[CCNUtil.ccn_churn_allnum];
this.churns[0] = new NoChurnAlgorithm(this.usedRouting);
this.churns[1] = new ChordDHTCRAlgorithm((ChordDHTRouting)this.usedRouting);
this.churns[2] = new NEW_CLASS();
this.usedChurn = this.churns[CCNUtil.ccn_churn_enable];
~~~
