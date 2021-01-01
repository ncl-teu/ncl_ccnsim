package net.gripps.ccn.chordDB;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.process.CCNMgr;

public class UpdateThread implements Runnable{

    protected Long routeID;
    protected double lambda;

    public UpdateThread(Long routeID) {
        this.routeID = routeID;
        this.lambda = CCNUtil.genDouble(CCNUtil.ccn_update_exp_dist_lambda_min, CCNUtil.ccn_update_exp_dist_lambda_max);;

    }

    @Override
    public void run() {
        try{
            double t = 1;
            double comulative_p = 0.0d;
            while(true){

                while (true) {
                    //指数分布による，累積の確率密度を算出．
                    comulative_p = 1 - Math.pow(Math.E, (-1) * t * this.lambda);
                    //1秒だけ待つ．
                    // System.out.println("NodeID:"+this.nodeID);
                    Thread.sleep(1000);
                    double randomValue = Math.random();
                    //Update可能状態となった
                    if (randomValue <= comulative_p) {
                        // System.out.println("NodeID:"+this.nodeID+ "Start Interest");
                        break;
                    }
                    t++;
                }
                //update処理を書く
                CCNRouter router = CCNMgr.getIns().getRouterMap().get(this.routeID);
                //当該ルータにて、コンテンツをupdateする。
                //そのために、コンテンツリストを取得する。
                //router.getCSEntry().getCacheMap().get(prefix);

            }


        }catch(Exception e){
            e.printStackTrace();
        }

    }

}
