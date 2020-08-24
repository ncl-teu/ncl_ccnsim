# ncl_ccnsim
CCN (Contents-Centric Networking) Simulator
# Manuals (in Japanese)
- [here](https://github.com/ncl-teu/ncl_ccnsim/tree/master/manuals). 
# How to use
- Double click `ccnrun.bat' for windows or run `./ccnrun.sh` for Linux. 
- The configuration file is **ccn.properties**. 
- The all log if written to `ccn/ccnlog.csv` as overwritten mode. 
# Cache algorithm
- Since the simulator is based on Java, please create a new class that extends `net.gripps.ccn.caching.BaseCachingAlgorithm`. 
- Example caching algorithm is implemented as **OnPathCaching.java**. Please refer to it. 
## How to create a new caching algorithm
1. In ccn.properties, please set `ccn_caching_no=0` to `ccn_caching_no=1`(i.e., change the used index no). And please set `ccn_caching_allnum=1` to `ccn_caching_allnum=2` (i.e., increment it as the number of candidate caching algorithms). 
2. In `ccn.net.gripps.ccn.core.CCNRouter', please change as follows: 
