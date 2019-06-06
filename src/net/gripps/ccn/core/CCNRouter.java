package net.gripps.ccn.core;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.Logger.CCNLog;
import net.gripps.ccn.breadcrumbs.BC;
import net.gripps.ccn.breadcrumbs.BaseBreadCrumbsAlgorithm;
import net.gripps.ccn.breadcrumbs.BreadCrumbsAlgorithm;
import net.gripps.ccn.breadcrumbs.NoBreadCrumbsAlgorithm;
import net.gripps.ccn.caching.BaseCachingAlgorithm;
import net.gripps.ccn.caching.NoCaching;
import net.gripps.ccn.caching.OnPathCaching;
import net.gripps.ccn.process.CCNMgr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by kanemih on 2018/11/02.
 */
public class CCNRouter extends AbstractNode {


    /**
     * ルータのID
     */
    private Long routerID;

    /**
     * FIB
     */
    private FIB FIBEntry;

    /**
     * PIT
     */
    private PIT PITEntry;

    /**
     * Contents Store
     */
    private CS CSEntry;

    /**
     * Faceのリスト（ルータ宛）
     */
    private HashMap<Long, Face> face_routerMap;

    /**
     * Faceリスト（ノード宛）
     */
    private HashMap<Long, Face> face_nodeMap;

    /**
     * Faceの最大保持数
     */
    private int face_num;

    /**
     * CSの最大保持数
     */
    private int cs_num;

    /**
     * FIBの最大保持数
     */
    private int fib_num;

    /**
     * PITの最大保持数
     */
    private int pit_num;

    /**
     * BreadCrumbs(パンくず）のマップ
     * (prefix, パンくず)のMap構造．
     */
    private HashMap<String, BC> bcMap;


    /**
     * キャッシュアルゴリズムの配列
     */
    private BaseCachingAlgorithm[] cachings;

    /**
     * 実際に使われるキャッシュアルゴリズム
     */
    private BaseCachingAlgorithm usedCaching;

    /**
     * BreadCrumbsアルゴリズムの配列
     */
    private BaseBreadCrumbsAlgorithm[] bcs;

    /**
     * 実際に使われるBreadCrumbsアルゴリズム
     */
    private BaseBreadCrumbsAlgorithm usedBC;


    /**
     * 状態
     */
    private int state;

    /**
     * @param routerID
     */
    public CCNRouter(Long routerID) {
        super(new LinkedBlockingQueue<InterestPacket>());
        //this.contentsQueue = new LinkedBlockingQueue<CCNContents>();
        this.type = CCNUtil.NODETYPE_NODE;
        this.routerID = routerID;
        this.FIBEntry = new FIB();
        this.PITEntry = new PIT();
        this.CSEntry = new CS();
        this.face_routerMap = new HashMap<Long, Face>();
        this.face_nodeMap = new HashMap<Long, Face>();
        this.face_num = CCNUtil.genInt(CCNUtil.ccn_node_face_num_min, CCNUtil.ccn_node_face_num_max);
        this.cs_num = CCNUtil.genInt(CCNUtil.ccn_cs_entry_min, CCNUtil.ccn_cs_entry_max);
        this.fib_num = CCNUtil.genInt(CCNUtil.ccn_fib_entry_min, CCNUtil.ccn_fib_entry_max);
        this.pit_num = CCNUtil.genInt(CCNUtil.ccn_pit_entry_min, CCNUtil.ccn_pit_entry_max);
        this.bcMap = new HashMap<String, BC>();
        this.cachings = new BaseCachingAlgorithm[CCNUtil.ccn_caching_allnum];
        /***ここに，キャッシングアルゴリズムを列挙してください**/
        this.cachings[0] = new OnPathCaching();
        this.cachings[1] = new NoCaching();

        /***ここまで**/
        this.usedCaching = this.cachings[CCNUtil.ccn_caching_no];

        this.bcs = new BaseBreadCrumbsAlgorithm[CCNUtil.ccn_bc_allnum];
        this.bcs[0] = new NoBreadCrumbsAlgorithm();
        this.bcs[1] = new BreadCrumbsAlgorithm();

        this.usedBC = this.bcs[CCNUtil.ccn_bc_enable];
        this.state = CCNUtil.STATE_NODE_NONE;

        //データ受信プロセスを起動
        Thread t = new Thread(this.receiver);
        t.start();


    }

    public CCNRouter() {
        super();
        Thread t = new Thread(this.receiver);
        t.start();
    }

    @Override
    public void run() {
        try {
            /**
             * パケットを転送するためのループ
             */
            while (true) {
                //  System.out.println("RouterID:"+this.routerID);
                Thread.sleep(100);
                if (!this.interestQueue.isEmpty()) {
                    //InterestPacketを取り出す．
                    InterestPacket p = this.interestQueue.poll();
                    //そして中身を見る．
                    this.processInterest(p);

                } else {
                    //Interestパケットがこなければ，何もしない．
                }
                if (!this.contentsQueue.isEmpty()) {
                    //コンテンツが来たら，処理
                    CCNContents c = this.contentsQueue.poll();
                    this.processContents(c);
                }
                if(this.state == CCNUtil.STATE_NODE_END){
                    break;
                }
            }
            System.out.println("[Leave] Router "+this.getRouterID()+" Leaved....");
            CCNLog.getIns().log(",6,"+"-"+","+"-"+","+"-"+","+ "-"+","+
                    "-"+","+"-"+","+"-"+","+"-"+","+"-"+","+"x"+","+"-"+","+this.getRouterID());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * @param c
     */
    public void processContents(CCNContents c) {
        if(c.getHistoryList().size() >= CCNUtil.ccn_interest_ttl){
            ForwardHistory last = c.getHistoryList().getLast();

            CCNLog.getIns().log(",x,"+c.getPrefix()+","+"-"+","+c.getHistoryList().getFirst().getStartTime()+","+ c.getHistoryList().getLast().getArrivalTime()+","+
                    (c.getHistoryList().getLast().getArrivalTime()-c.getHistoryList().getFirst().getStartTime())+","+
                    last.getToID()+","+"-"+","+c.getHistoryList().size()+","+"-"+","+"x"+","+"-");

        }else{
            boolean ret = this.usedBC.forwardBCData(this,c);
            if(ret){


            }else{
                ForwardHistory lastH = c.getHistoryList().getLast();
                lastH.setArrivalTime(System.currentTimeMillis() + CCNUtil.ccn_hop_per_delay);
                //キャッシュでなければ，BCチェックをする．

                //コンテンツが来た時，PITを見る．
                if (this.getPITEntry().getTable().containsKey(c.getPrefix())) {
                    //もしPITにあれば，そのエントリ全てに送る．
                    Iterator<Face> fIte = this.getPITEntry().getTable().get(c.getPrefix()).iterator();
                    while (fIte.hasNext()) {
                        Face f = fIte.next();
                        //fのpinterIDへ送る
                        //PITのpre
                        if (f.getType() == CCNUtil.NODETYPE_NODE) {
                            //ノードへ送る準備．
                            CCNNode node = CCNMgr.getIns().getNodeMap().get(f.getPointerID());
                            //node.getContentsQueue().offer(c);
                            //転送履歴を作成して，追加
                            ForwardHistory f2 = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, node.getNodeID(), CCNUtil.NODETYPE_NODE,
                                    System.currentTimeMillis(), -1);
                            c.getHistoryList().add(f2);
                            //BC作成処理
                            if (!c.isBC()) {
                                this.usedBC.createBC(this, c);
                            }

                            //ノードへデータを転送
                            node.forwardData(c);
                            //System.out.println("要求元ノードへデータ到着:@"+ c.getPrefix());
                        } else {

                            if (f.getPointerID() == this.getRouterID()) {
                                //PITエントリの中に，Faceのターゲットが自分であるもの
                                //System.out.println("test");
                            } else {
                                LinkedList<ForwardHistory> fList = c.getHistoryList();
                                //あて先がルータであれば，ルータに送る．
                                CCNRouter router = CCNMgr.getIns().getRouterMap().get(f.getPointerID());
                                if(router == null){
                                    CCNLog.getIns().log(",3,"+c.getPrefix()+",-"+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                                            (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                                            c.getHistoryList().getFirst().getFromID()+",-"+","+fList.size()+",-"+","+"x"+","+"-");
                                    return;
                                }
                                //転送履歴を作成して，追加
                                ForwardHistory f2 = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, router.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                                        System.currentTimeMillis(), -1);
                                c.getHistoryList().add(f2);
                                if (!c.isBC()) {
                                    this.usedBC.createBC(this, c);
                                }
                                //ルータへデータを転送
                                router.forwardData(c);
                            }

                        }
                        //そしてPITからエントリを削除する．
                        this.getPITEntry().removeByKey(c.getPrefix());
                        CCNContents copyContents = (CCNContents) c.deepCopy();
                        if(this.cs_num <= this.CSEntry.getCacheMap().size()){
                            this.usedCaching.chachingIFCSFULL(c, this);
                        }else{
                            //キャッシング処理
                            this.usedCaching.cachingProcess(copyContents, this);
                        }

                    }
                } else {
                    CCNContents copyContents = (CCNContents) c.deepCopy();
                    //PITになければどうするか
                    //とりあえずキャッシュする？
                    if(this.cs_num <= this.CSEntry.getCacheMap().size()){
                        this.usedCaching.chachingIFCSFULL(c, this);
                    }else {
                        this.usedCaching.chachingProcessIfNoPITEntry(copyContents, this);

                    }
                }
            }
        }


    }

    /**
     * @param id
     * @return
     */
    public boolean containsIDinRouterFaceMap(Long id) {
        boolean ret = false;
        Iterator<Face> fIte = this.getFace_routerMap().values().iterator();
        while (fIte.hasNext()) {
            Face f = fIte.next();
            // if(f.getFaceID().longValue() == id.longValue()){
            if (f.getPointerID().longValue() == id.longValue()) {
                ret = true;
                break;
            }
        }

        return ret;
    }

    /**
     * @param id
     * @return
     */
    public CCNRouter findCCNRouterFromFaceList(Long id) {
        Iterator<Face> fIte = this.face_routerMap.values().iterator();
        Face retFace;
        boolean isfound = false;
        while (fIte.hasNext()) {
            Face f = fIte.next();
            if (f.getPointerID().longValue() == id.longValue()) {
                isfound = true;
                retFace = f;
                return CCNMgr.getIns().getRouterMap().get(retFace.getPointerID());

                //break;
            }
        }
        return null;

    }

    /**
     * @param id
     * @return
     */
    public Face findFaceByID(Long id, HashMap<Long, Face> map) {
        Iterator<Face> fIte = map.values().iterator();
        Face retFace;
        boolean isfound = false;
        while (fIte.hasNext()) {
            Face f = fIte.next();
            if (f.getPointerID().longValue() == id.longValue()) {
                //見つかれば，そのfaceを返す．
                return f;
            }
        }
        return null;

    }


    public boolean processSendData() {
        return true;
    }

    /**
     * InrerestPacketを処理します．
     * 1. CSをみて，コンテンツがないかチェックする．
     * 2. もし一致するPrefixがあれば，データとして返す．なければ，PITを見る．
     * 3. もし一致するPrefixがPITにあれば，PITエントリにFace情報を追加する．なければ何もしない
     * 4. FIBを見て，一致するPrefixがあれば，対応するFaceにかかれてあるあて先へ転送する．
     *
     * @param p
     */
    public void processInterest(InterestPacket p) {
        ForwardHistory h = p.getHistoryList().getLast();
        //とりあえずhの到着時刻を設定
        h.setArrivalTime(System.currentTimeMillis() + CCNUtil.ccn_hop_per_delay);
        Long toID = h.getFromID();
        int toType = h.getFromType();
        p.setCount(p.getCount() + 1);
        //ココに，BCの処理を入れる．
        //もしfalseの場合のみ，↓の処理へ移行する．
        boolean retBC = this.usedBC.forwardRequestByBC(this,p);
        if(retBC){
            return;
        }
        LinkedList<ForwardHistory> fList = p.getHistoryList();

        //もしCSにあれば，データを返す．
        if (this.CSEntry.getCacheMap().containsKey(p.getPrefix())) {
            CCNContents c = this.CSEntry.getCacheMap().get(p.getPrefix());

            //最新の履歴を見て，送信もとを特定する．
            if (h.getFromType() == CCNUtil.NODETYPE_ROUTER) {
                //ルータならば，ルータへCCNContentsを送る．
                CCNRouter r = findCCNRouterFromFaceList(toID);
                if(r == null){
                    CCNLog.getIns().log(",1,"+p.getPrefix()+",-"+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                            (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                            p.getHistoryList().getFirst().getFromID()+",-"+","+fList.size()+",-"+","+"x"+","+"-");
                    return;
                }
                c.getHistoryList().clear();
                // CCNContents c = this.CSEntry.getCacheMap().get(p.getPrefix());
                //後は送信処理だが，分割して送る？
                // System.out.println("<CS内キャッシュを要求元ルータへ返送> from " + "Router" + this.routerID + "-->" + "Router" + toID + " for prefix:" + p.getPrefix());
                ForwardHistory f = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, r.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                        System.currentTimeMillis(), -1);
                c.getHistoryList().add(f);
                r.forwardData(c);
                CCNLog.getIns().log(",13,"+p.getPrefix()+",-"+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                        (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                        p.getHistoryList().getFirst().getFromID()+","+this.getRouterID()+","+fList.size()+",-"+","+"o"+","+"-");


            } else {
                // System.out.println("<CS内キャッシュを要求元ノードへ返送> from " + "Router" + this.routerID + "-->" + "Node" + toID + " for prefix:" + p.getPrefix());
                CCNNode n = CCNMgr.getIns().getNodeMap().get(toID);
                //System.out.println("要求元ノードへデータ到着:@"+ c.getPrefix());
                ForwardHistory f = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, n.getNodeID(), CCNUtil.NODETYPE_NODE,
                        System.currentTimeMillis(), -1);
                c.getHistoryList().add(f);
                n.forwardData(c);
                CCNLog.getIns().log(",13,"+p.getPrefix()+",-"+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                        (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                        p.getHistoryList().getFirst().getFromID()+","+this.getRouterID()+","+fList.size()+",-"+","+"o"+","+"-");


            }
        } else {
            //CSになければ，PITを見る．
            boolean isNotFound = false;
            //PITへの反映
            this.addFacetoPit(p, toID, toType);
            //以降は，FIBに対する操作を行う．FIBを見て，prefixがあれば，そのFaceのpointerへ転送する．
            if (this.FIBEntry.getTable().containsKey(p.getPrefix())) {
                //もしあれば，faceを見る．
                LinkedList<Face> fList2 = this.FIBEntry.getTable().get(p.getPrefix());
                Iterator<Face> fIte = fList2.iterator();
                LinkedList<Face> rList = new LinkedList<Face>();

                //該当エントリに対するループ
                while (fIte.hasNext()) {
                    //当該エントリのfaceList分にだけ，転送する．
                    Face f = fIte.next();
                    if (f.getType() == CCNUtil.NODETYPE_ROUTER) {
                        rList.add(f);
                    }
                    //とりあえず．先頭をみる．
                    // Face f = fList.getFirst();
                    //Faceとpを使って，Interestを転送する．
                    this.forwardInterest(f, p);
                }

            } else {
                //Faceから，Prefixのハッシュに近いものを選択する．
                Long routerID = this.usedRouting.addFaceToFIBAsNewEntry(p.getPrefix(), this);
                Face face2 = this.findFaceByID(routerID, this.getFace_routerMap());
                this.forwardInterest(face2, p);

            }
            // }else{
            //FIBにもなければ，破棄する．

            //     }
        }


    }

    public boolean isInterestForwardable(InterestPacket p, Long toID, int type) {
        boolean ret = true;
        if (p.getHistoryList().size() >= CCNUtil.ccn_interest_ttl) {
            ret = false;
        } else {

        }
        /**
         Iterator<ForwardHistory> dIte = p.getHistoryList().iterator();
         while(dIte.hasNext()){
         ForwardHistory h = dIte.next();
         //もし過去に同じ宛先であれば，false
         if((h.getToID().longValue() == toID.longValue())&&(h.getToType() == type)){
         ret = false;
         break;

         }
         }**/
        return ret;

    }

    /**
     *
     * @param prefix
     * @param f
     * @return
     */
    public boolean isFaceContains(String prefix, Face f){
        boolean ret = false;
        if(this.getFIBEntry().getTable().containsKey(prefix)){
            LinkedList<Face> fList = this.getFIBEntry().getTable().get(prefix);
            Iterator<Face> fIte = fList.iterator();
            while(fIte.hasNext()){
                Face rf = fIte.next();
                if((rf.getPointerID() == f.getPointerID())&&(rf.getType() == f.getType())){
                    ret = true;
                    break;
                }
            }

        }else{
            return false;
        }

        return ret;
    }

    public boolean isIntestLooped(InterestPacket p, Long toID, int type) {
        boolean ret = false;

        Iterator<ForwardHistory> dIte = p.getHistoryList().iterator();
        while (dIte.hasNext()) {
            ForwardHistory h = dIte.next();
            //もし過去に同じ宛先であれば，false
            if ((h.getToID().longValue() == toID.longValue()) && (h.getToType() == type)) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
     * @param f
     * @param p
     * @return
     */
    public boolean forwardInterest(Face f, InterestPacket p) {

        //Interestパケットが指定のあて先(f.getPointerID)へ転送可能かどうかのチェック
        //TTL以内の場合
        if (isInterestForwardable(p, f.getPointerID(), f.getType())) {
            //転送可能であれば，転送する．
            if (f.getType() == CCNUtil.NODETYPE_ROUTER) {
                long retID = -1L;
                //Interestパケットがループしそうなら，ルーティングアルゴリズムに任せる．
                if (this.usedRouting.getRouterMap() == null) {
                    this.usedRouting.setRouterMap(CCNMgr.getIns().getRouterMap());
                }
                if (this.isIntestLooped(p, f.getPointerID(), f.getType())) {
                    retID = this.usedRouting.getNextRouterIDIfInterestLoop(p, f, this);
                } else {
                    retID = f.getPointerID();
                }

                //もしルータなら，ルータへ送る．
                CCNRouter r = CCNMgr.getIns().getRouterMap().get(retID);
                LinkedList<ForwardHistory> fList = p.getHistoryList();
                if(r == null){
                    CCNLog.getIns().log(",1,"+p.getPrefix()+",-"+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                            (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                            p.getHistoryList().getFirst().getFromID()+",-"+","+fList.size()+",-"+","+"x"+","+"-");
                    return false;
                }
                //Interestを追記する．
                ForwardHistory newHistory = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, r.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                        System.currentTimeMillis(), -1);
                long minBW = Math.min(Math.min(this.getBw(), r.getBw()), p.getMinBW());
                p.setMinBW(minBW);

                p.getHistoryList().add(newHistory);
                //System.out.println("【Interest:"+((ChordDHTRouting)this.usedRouting).calcHashCode(p.getPrefix().hashCode())+" をルータ->ルータへ転送】FromID:"+this.routerID+"toID:"+f.getPointerID());

                r.getInterestQueue().offer(p);
            } else {
                // System.out.println("【Interestがルータ->コンテンツ保持ノードへ転送】FromID:"+this.routerID+"toID:"+f.getPointerID());

                CCNNode n = CCNMgr.getIns().getNodeMap().get(f.getPointerID());
                ForwardHistory newHistory = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, n.getNodeID(), CCNUtil.NODETYPE_NODE,
                        System.currentTimeMillis(), -1);
                p.getHistoryList().add(newHistory);
                long minBW = Math.min(Math.min(this.getBw(), n.getBw()), p.getMinBW());
                p.setMinBW(minBW);
                n.getInterestQueue().offer(p);
            }
        } else {
            //何もせず，breakする．
            //破棄する．
            ForwardHistory last = p.getHistoryList().getLast();

            CCNLog.getIns().log(",x,"+p.getPrefix()+","+"-"+","+p.getHistoryList().getFirst().getStartTime()+","+ p.getHistoryList().getLast().getArrivalTime()+","+
                    (p.getHistoryList().getLast().getArrivalTime()-p.getHistoryList().getFirst().getStartTime())+","+
                    last.getToID()+","+"-"+","+p.getHistoryList().size()+","+"-"+","+"x"+","+"-");
           // System.out.println("【Dropped at FIB】fromID:" + f.getPointerID() + "@Router " + this.routerID);
        }


        return true;
    }

    public boolean  addFacetoPit(InterestPacket p, Long toID, int toType) {
        boolean isNotFound = true;
        boolean ret = true;
        //CSになければ，PITを見る．
        //もしあれば，faceListへ追加する．
        HashMap<String, LinkedList<Face>> pitTable = this.PITEntry.getTable();
        Face f;
        /*
        if(pitTable.containsKey(p.getPrefix())){
            //pitにprefixがすでにあれば，faceを追加する．
            //そこで，faceを持っているか探す．
            if (toType == CCNUtil.NODETYPE_ROUTER) {
                f = this.findFaceByID(toID, this.face_routerMap);
            } else {
                f = this.findFaceByID(toID, this.face_nodeMap);
            }
            if(f != null){
                //もしtoIDに該当するfaceを持っていれば，pitTableへ追加
            }else{
                f = new Face(null, toID, toType);
                //もしtoIDに該当するFaceを持っていなければ，新規作成
            }
            //PITへFaceを追加する
            this.getPITEntry().addFace(p.getPrefix(), f);

        }else{
            //prefixがない場合，
        }*/
        if(this.pit_num <= this.getPITEntry().getTable().size()){
            return false;
        }
        if (toType == CCNUtil.NODETYPE_ROUTER) {
            f = this.findFaceByID(toID, this.face_routerMap);
        } else {
            f = this.findFaceByID(toID, this.face_nodeMap);
        }
        if (f != null) {
            //もしtoIDに該当するfaceを持っていれば，pitTableへ追加
        } else {
            f = new Face(null, toID, toType);
            //もしtoIDに該当するFaceを持っていなければ，新規作成
        }
        //PITへFaceを追加する
        //this.getPITEntry().addFace(p.getPrefix(), f);
        //FaceをPITへ追加する
        //System.out.println("【PITへ追加】@"+this.routerID+":Prefix:"+p.getPrefix());
        this.PITEntry.addFace(p.getPrefix(), f);
        //fList.add(f);
        //  isNotFound = false;
        //   }else{
        //もしなければ，追加するのみ．

        // isNotFound = true;
        //   }
        // return isNotFound;
        return true;
    }


    /**
     * IDが付与されていないFaceを追加します．
     * 単に，ルータのFaceListにFaceを追加するのみ．
     *
     * @param f
     */
    public Long addFace(Face f, HashMap<Long, Face> map) {
        if (this.face_num <= this.face_routerMap.size() + this.face_nodeMap.size()) {
            return CCNUtil.MINUS_VAUE;
        }
        if((f.getPointerID() == this.getRouterID())&&(f.getType() == CCNUtil.NODETYPE_ROUTER)){
            return CCNUtil.MINUS_VAUE;
        }
        Iterator<Long> keyIte = map.keySet().iterator();
        long maxValue = 0;
        boolean isExist = false;
        while (keyIte.hasNext()) {
            Long id = keyIte.next();
            if (maxValue <= id) {
                maxValue = id;
            }
            Face orgFace = map.get(id);
            //同じものが存在すれば，抜ける．
            if ((f.getPointerID().longValue() == orgFace.getPointerID().longValue()) &&
                    (f.getType() == orgFace.getType())) {
                isExist = true;
                break;
            }
        }
        if(map.size() >= this.face_num){
            return CCNUtil.MINUS_VAUE;
        }

        if (isExist) {
            return CCNUtil.MINUS_VAUE;
        } else {
            Long newValue = new Long(maxValue + 1);
            f.setFaceID(newValue);
            //新しい値をputする．
            map.put(newValue, f);
            return newValue;
        }
    }

    public boolean sendInterest(InterestPacket p) {
        this.interestQueue.offer(p);
        return true;
    }

    public LinkedBlockingQueue<CCNContents> getContentsQueue() {
        return contentsQueue;
    }

    public void setContentsQueue(LinkedBlockingQueue<CCNContents> contentsQueue) {
        this.contentsQueue = contentsQueue;
    }

    public HashMap<Long, Face> getFace_routerMap() {
        return face_routerMap;
    }

    public void setFace_routerMap(HashMap<Long, Face> face_routerMap) {
        this.face_routerMap = face_routerMap;
    }

    public HashMap<Long, Face> getFace_nodeMap() {
        return face_nodeMap;
    }

    public void setFace_nodeMap(HashMap<Long, Face> face_nodeMap) {
        this.face_nodeMap = face_nodeMap;
    }

    public Long getRouterID() {
        return routerID;
    }

    public void setRouterID(Long routerID) {
        this.routerID = routerID;
    }

    public FIB getFIBEntry() {
        return FIBEntry;
    }

    public void setFIBEntry(FIB FIBEntry) {
        this.FIBEntry = FIBEntry;
    }

    public PIT getPITEntry() {
        return PITEntry;
    }

    public void setPITEntry(PIT PITEntry) {
        this.PITEntry = PITEntry;
    }

    public CS getCSEntry() {
        return CSEntry;
    }

    public void setCSEntry(CS CSEntry) {
        this.CSEntry = CSEntry;
    }

    public int getCs_num() {
        return cs_num;
    }

    public void setCs_num(int cs_num) {
        this.cs_num = cs_num;
    }

    public int getFib_num() {
        return fib_num;
    }

    public void setFib_num(int fib_num) {
        this.fib_num = fib_num;
    }

    public int getPit_num() {
        return pit_num;
    }

    public void setPit_num(int pit_num) {
        this.pit_num = pit_num;
    }

    public HashMap<String, BC> getBcMap() {
        return bcMap;
    }

    public void setBcMap(HashMap<String, BC> bcMap) {
        this.bcMap = bcMap;
    }


    public int getFace_num() {
        return face_num;
    }

    public void setFace_num(int face_num) {
        this.face_num = face_num;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
