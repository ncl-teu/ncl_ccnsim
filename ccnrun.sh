#!/bin/bash
java   -cp ./classes:ccn/:lib/commons-math-2.0.jar:lib/log4j-api-2.11.1.jar:lib/log4j-core-2.11.1.jar:lib/ncl-taskschedsim.jar:lib/ncl-sfc.jar:lib/ncl-taskschedsim.jar net.gripps.ccn.main.CCNTest ccn.properties
