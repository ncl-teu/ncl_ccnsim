package net.gripps.ccn.process;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.churn.BaseChurnResilienceAlgorithm;
import net.gripps.ccn.churn.ChordDHTCRAlgorithm;
import net.gripps.ccn.churn.NoChurnAlgorithm;
import net.gripps.ccn.core.*;
import net.gripps.ccn.fibrouting.BaseRouting;
import net.gripps.ccn.fibrouting.ChordDHTRouting;


import java.util.*;

/**
 * Created by kanem on 2018/11/14.
 */
public class CCNMgr implements Runnable{
    /**
     * 自分自身
     */
    public static CCNMgr own = null;

    /**
     * AbstractNodeを格納するためのプール
     */
    private HashMap<Long, CCNNode> nodeMap;

    /**
     * CCNルータを格納するためのプール
     */
    private HashMap<Long, CCNRouter> routerMap;

    /**
     * コンテンツのDB．見つかるたびに，削除されるものとする．
     */
    private HashMap<String, CCNContents> cMap;

    //破棄されたInterestpacketリスト
    private LinkedList<InterestPacket> discardPacketList;

    /**
     * 何だったか忘れた．
     */
    private long maxInterestID;

    private BaseRouting[] routings;

    private BaseRouting usedRouting;

    private BaseChurnResilienceAlgorithm[] churns;

    private BaseChurnResilienceAlgorithm usedChurn;



    //private LinkedBlockingQueue<InterestPacket> reqQueue;


    /**
     *
     * @return
     */
    public  static CCNMgr getIns(){
        if(CCNMgr.own == null){
            CCNMgr.own = new CCNMgr();
        }else{

        }

        return CCNMgr.own;
    }


    /**
     * コンストラクタです．
     * スレッドを起動する前の準備を行います．
     * 例えば，ノード，ルータを生成してMapへ格納します．
     * そして，コンテンツを配備し，Interestパケットを指定数分だけノードのキューへ入れます．
     * これらが終われば，ルータのスレッド起動→ノードの起動をします．
     *
     */
    private CCNMgr(){
        this.maxInterestID = 0;
        this.nodeMap = new HashMap<Long, CCNNode>();
        this.routerMap = new HashMap<Long, CCNRouter>();
        this.cMap = new HashMap<String, CCNContents>(100);
        this.discardPacketList = new LinkedList<InterestPacket>();
      //  this.reqQueue = new LinkedBlockingQueue<InterestPacket>(1000);
        this.routings = new BaseRouting[CCNUtil.ccn_routing_allnum];
        /***ここに，ルーティングアルゴリズムを列挙してください***/
        this.routings[0] = new ChordDHTRouting(this.nodeMap, this.routerMap);

        /*************ここまで*************************/
        this.usedRouting = this.routings[CCNUtil.ccn_routing_no];

        //**********Churningアルゴリズム***************/
        this.churns = new BaseChurnResilienceAlgorithm[CCNUtil.ccn_churn_allnum];
        this.churns[0] = new NoChurnAlgorithm(this.usedRouting);
        this.churns[1] = new ChordDHTCRAlgorithm((ChordDHTRouting)this.usedRouting);
        this.usedChurn = this.churns[CCNUtil.ccn_churn_enable];
        //************:ここまで*************************/

        this.initialize();
//数を増やす余地あり．


        this.buildInterestPackets();
        this.buildFIB();

//System.out.println("test");

    }

    /**
     * コンテンツ保持ノードからFaceをさかのぼってFIBを埋める処理を行います．
     */
    public void buildFIB(){
        //差し替え可能なようにする
        this.usedRouting.buildFIBProcess();
    }

    @Override
    public void run() {

        try {
            //リクエスト送信することが決まったら，指数分布の累積分布関数の確率に従い，
            //Interestパケットを送る．
            double t = 1;
            double comulative_p = 0.0d;

            double k = 1;
            double compulative_pk = 0.0d;

            while (true) {
                //指数分布による，累積の確率密度を算出．
                comulative_p = 1 - Math.pow(Math.E, (-1) * t * CCNUtil.ccn_join_exp_dist_lambda);
                //2秒だけ待つ．
                // System.out.println("NodeID:"+this.nodeID);
                Thread.sleep(2000);
                double randomValue = Math.random();
                //Interestパケット送信可能状態となった
                if (randomValue <= comulative_p) {
                    // System.out.println("NodeID:"+this.nodeID+ "Start Interest");
                   this.usedChurn.ccnJoin(null, this.routerMap );
                }
                compulative_pk = 1 - Math.pow(Math.E, (-1) * k * CCNUtil.ccn_leave_exp_dist_lambda);
                double randomValue2 = Math.random();
                if(randomValue2 <= compulative_pk){
                    this.usedChurn.ccnLeave(null, this.routerMap);
                }
                t++;
                k++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public void initialize(){
        for(int i = 0; i< CCNUtil.ccn_router_num; i++){
            //IDはハッシュ値を計算する．
            long id = this.usedRouting.calcID(i);
            //ルータの配備
            CCNRouter router = new CCNRouter(new Long(id));
            //ルータをputする．
            this.routerMap.put(router.getRouterID(), router);
        }

        //ノードの配備
        for(int j=0;j<CCNUtil.ccn_node_num;j++){
            //今度はノード作成
            CCNNode node = new CCNNode(new Long(j));
            this.nodeMap.put(node.getNodeID(), node);
        }


        //次は，各ルータのfaceを設定する．
        this.buildRouterFaces();
        //ノード<-->ルータの紐づけをする．
        this.buildNodeFaces();
/*
        int content_cnt = CCNUtil.genInt(CCNUtil.ccn_node_num_contents_min, CCNUtil.ccn_node_num_contents_max);
        //コンテンツを生成して配備する．
        for(int i=0;i<content_cnt;i++){
            UUID uuid = UUID.randomUUID();
            String prefix  = uuid.toString();
            long size = CCNUtil.genLong(CCNUtil.ccn_contents_size_min, CCNUtil.ccn_contents_size_max);
            int type = CCNUtil.genInt(CCNUtil.TYPE_MOVIE, CCNUtil.TYPE_DATA);
            CCNContents c = new CCNContents(size, prefix, node.getNodeID(), type, System.currentTimeMillis(), -1, false);

           // node.getOwnContentsMap().put(prefix, c);
            this.cMap.put(prefix, c);
        }
    }
    */


        //次に，コンテンツを各ノードへ配備する．
        Iterator<CCNNode> ite = this.nodeMap.values().iterator();
        while(ite.hasNext()){
            CCNNode node = (CCNNode)ite.next();
            int content_cnt = CCNUtil.genInt(CCNUtil.ccn_node_num_contents_min, CCNUtil.ccn_node_num_contents_max);
            //コンテンツを生成して配備する．
            for(int i=0;i<content_cnt;i++){
                UUID uuid = UUID.randomUUID();
                String prefix  = uuid.toString();
                long size = CCNUtil.genLong(CCNUtil.ccn_contents_size_min, CCNUtil.ccn_contents_size_max);
                int type = CCNUtil.genInt(CCNUtil.TYPE_MOVIE, CCNUtil.TYPE_DATA);
                CCNContents c = new CCNContents(size, prefix, node.getNodeID(), type, System.currentTimeMillis(), -1, false);
                node.getOwnContentsMap().put(prefix, c);
                long hcode = this.usedRouting.calcHashCode(prefix.toString().hashCode());
                c.setCustomID(hcode);
                //ここのIDは，別のID（コンテンツID）であって，chordで使われるcustomID(ハッシュコード）ではない．
                //このprefixをinterestのprefixとして設定しているのが問題．
                this.cMap.put(prefix, c);
            }
        }

    }

    /**
     * インタレストパケットを生成して，各ノードのstartキューへ入れる．
     */
    public void buildInterestPackets(){
        Iterator<String> preIte = this.cMap.keySet().iterator();
        long id = 0;
        ArrayList<String> preList = new ArrayList<String>();

        //コンテンツごとのループ
        while(preIte.hasNext()){

            String prefix = preIte.next();
            preList.add(prefix);
            CCNContents c = this.cMap.get(prefix);
            Long oID = c.getOrgOwnerID();
            long targetID = oID.longValue();

            //以降を指定回数分繰り返す．
           // int dup_num = CCNUtil.genInt(CCNUtil.ccn_node_duplicate_interest_num_min, CCNUtil.ccn_node_duplicate_interest_num_max);
            int dup_num = CCNUtil.genInt2(CCNUtil.ccn_node_duplicate_interest_num_min, CCNUtil.ccn_node_duplicate_interest_num_max,
                    CCNUtil.dist_duplicate_interest, CCNUtil.dist_duplicate_interest_mu);
            for(int i=0;i<dup_num;i++){
                while(targetID == oID.longValue() ){
                    //保持するノードを乱数によって決める．
                    targetID = CCNUtil.genLong(0, CCNUtil.ccn_node_num-1);
                }

                //ノードを取得する．
                CCNNode n = (CCNNode) this.nodeMap.get(targetID);
                int size = n.getStart_interestQueue().size();
                ForwardHistory h = new ForwardHistory(targetID, CCNUtil.NODETYPE_NODE, (long)-1, -1, -1, -1);
                LinkedList<ForwardHistory> hList = new LinkedList<ForwardHistory>();
                hList.add(h);
                //Interestパケットを生成する．
                InterestPacket p = new InterestPacket(prefix, new Long(id), 1500, targetID, size + 1, hList);
                n.getStart_interestQueue().offer(p);
                id++;
            }

        }
        this.maxInterestID = id;

        //次は，ノードごとのループ
        Iterator<CCNNode> nIte = this.nodeMap.values().iterator();
        while(nIte.hasNext()){
            CCNNode n = (CCNNode)nIte.next();
            if(n.getStart_interestQueue().size() < CCNUtil.ccn_node_request_num){
                int remain = CCNUtil.ccn_node_request_num - n.getStart_interestQueue().size();
                for(int i=0;i<remain;i++){
                    //残りの分だけキューへ入れる．
                    ForwardHistory h = new ForwardHistory(n.getNodeID(), CCNUtil.NODETYPE_NODE, (long)-1, -1, -1, -1);
                    LinkedList<ForwardHistory> hList = new LinkedList<ForwardHistory>();
                    hList.add(h);
                    long size = n.getStart_interestQueue().size();
                    //long id = this.start_interestQueue.size() + 1;
                    //Interestパケットを生成する．
                    Iterator<String> pIte = this.cMap.keySet().iterator();
                    int cnt = 0;
                    int index = CCNUtil.genInt(0, this.cMap.size()-1);
                    String prefix = preList.get(index);
                    InterestPacket p = new InterestPacket(prefix, new Long(id), 1500, n.getNodeID(), size + 1, hList);
                    n.getStart_interestQueue().offer(p);
                }
            }
        }

    }

    public long getMaxInterestID() {
        return maxInterestID;
    }

    public void setMaxInterestID(long maxInterestID) {
        this.maxInterestID = maxInterestID;
    }

    /**
     * ノード側の隣接ルータ集合の設定をする．
     * ノード側のRouterMapにルータを設定したあと，
     * ルータ側のFaceには追加する？？
     */
    public void buildNodeFaces(){
       this.usedRouting.buildNeighborRouters();
    }

    /**
     * ルータ同士のFaceを設定する．
     */
    public void buildRouterFaces(){
        CCNRouter r1 =  (CCNRouter)this.routerMap.get(new Long(0));
        for(int i=0;i<=CCNUtil.ccn_router_num-1;i++){
            //CCNRouter r1 = (CCNRouter)this.routerMap.get(new Long(i));
           // Long r1_id = this.usedRouting.getNextID(r1.getRouterID());
            Long nextid = this.usedRouting.getNextID(r1.getRouterID());
            CCNRouter r2 = (CCNRouter)this.routerMap.get(new Long(nextid));
            Face f1 = new Face(null, r2.getRouterID(), CCNUtil.NODETYPE_ROUTER);
            Long id1 = r1.addFace(f1, r1.getFace_routerMap());

            Face f2 = new Face(null, r1.getRouterID(), CCNUtil.NODETYPE_ROUTER);
            Long id2 = r2.addFace(f2, r2.getFace_routerMap());

            r1 = r2;
        }
        //ルータどうしのFaceを埋める（今度はとびとび）
        this.usedRouting.buildFaces();
        //上記で，確実に任意のルータ同士で通信が可能になったので，今度は
        //ランダムに関連付けを行う．
/**
        Iterator<CCNRouter> rIte = this.routerMap.values().iterator();
        while(rIte.hasNext()){
            CCNRouter r = (CCNRouter)rIte.next();
            long id = r.getRouterID().longValue();

            //while(r.getFace_num() > r.getFace_routerMap().size()){
            for(int k=0;k<r.getFace_num()-2;k++){
                //rに対して，相手を探す．
                //0?id-1, id+1~最後から探す．
                //0以外であれば，乱数を生成する．
                if(id == 0){
                    long id2 = CCNUtil.genLong(1, CCNUtil.ccn_router_num-1);
                    //もしすでにFaceにあるのなら，coneinue
                    if(r.containsIDinRouterFaceMap(new Long(id2))){
                        continue;
                    }else{
                        //新規のIDなら，追加する．
                        CCNRouter r2 = (CCNRouter)this.routerMap.get(new Long(id2));
                        this.associateFaces(r, r2);
                        continue;

                    }

                }

                if(id - 0>0){
                    long id2 = -1;
                    double rate = CCNUtil.getRoundedValue((double)id/(double)(CCNUtil.ccn_router_num-1));
                    double val = Math.random();
                    //IDに応じて決められた確率で，サイコロをふる．
                    if(val >= rate){
                        id2 = CCNUtil.genLong(id+1, CCNUtil.ccn_router_num-1);

                    }else{
                        id2 = CCNUtil.genLong(0, id-1);

                    }

                    //もしすでにFaceにあるのなら，coneinue
                    if(r.containsIDinRouterFaceMap(id2)){

                        continue;
                    }else{

                        //新規のIDなら，追加する．
                        CCNRouter r2 = (CCNRouter)this.routerMap.get(new Long(id2));
                        this.associateFaces(r, r2);
                    }
                }
            }

        }
 **/

    }

    public boolean  associateFaces(CCNRouter r1, CCNRouter r2){
        boolean ret = true;
        int num1 = r1.getFace_num();
        int num2 = r2.getFace_num();

        int len1 = r1.getFace_routerMap().size();
        int len2 = r2.getFace_routerMap().size();

        //双方ともに最大値未満である場合のみ，Faceを追加できる．
        if((num1 > len1)&&(num2 > len2)){

            Face f1 = new Face(null, r2.getRouterID(), CCNUtil.NODETYPE_ROUTER);
            Long id1 = r1.addFace(f1, r1.getFace_routerMap());

            Face f2 = new Face(null, r1.getRouterID(), CCNUtil.NODETYPE_ROUTER);
            Long id2 = r2.addFace(f2, r2.getFace_routerMap());
        }else{
            ret = false;
        }

        return ret;
    }



    public void process(){


        //CCNルータの起動
        Iterator<CCNRouter> rIte = this.routerMap.values().iterator();
        while(rIte.hasNext()){
            CCNRouter r = (CCNRouter) rIte.next();
            Thread t = new Thread(r);
            t.start();
        }

        //CCNノードの起動
        Iterator<CCNNode> nIte = this.nodeMap.values().iterator();
        while(nIte.hasNext()){
            CCNNode n = (CCNNode) nIte.next();
            Thread t = new Thread(n);
            t.start();
        }

    }

    public boolean nodeExist(Long id){
        return this.nodeMap.containsKey(id);
    }

    /**
     *
     * @param id
     * @return
     */
    public AbstractNode getNode(Long id){
        return CCNMgr.getIns().getNodeMap().get(id);
    }


    public HashMap<Long, CCNNode> getNodeMap() {
        return nodeMap;
    }

    public void setNodeMap(HashMap<Long, CCNNode> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public HashMap<Long, CCNRouter> getRouterMap() {
        return routerMap;
    }

    public void setRouterMap(HashMap<Long, CCNRouter> routerMap) {
        this.routerMap = routerMap;
    }

    public HashMap<String, CCNContents> getcMap() {
        return cMap;
    }

    public void setcMap(HashMap<String, CCNContents> cMap) {
        this.cMap = cMap;
    }
}
