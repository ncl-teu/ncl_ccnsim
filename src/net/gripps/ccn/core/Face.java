package net.gripps.ccn.core;

/**
 * Created by kanemih on 2018/11/02.
 */
public class Face {

    /**
     * Face ID．
     */
    private Long faceID;

    /**
     * このFaceが紐付いている先のID
     * ルータ，もしくはノードのIDを記載する．
     */
    private Long pointerID;

    /**
     * pointaerIDが指し示す先がルータなのか，もしくはノードなのか
     * を意味します．
     */
    private int type;

    public Face(Long faceID, Long pointerID, int type) {
        this.faceID = faceID;
        this.pointerID = pointerID;
        this.type = type;
    }

    public Long getFaceID() {
        return faceID;
    }

    public void setFaceID(Long faceID) {
        this.faceID = faceID;
    }

    public Long getPointerID() {
        return pointerID;
    }

    public void setPointerID(Long pointerID) {
        this.pointerID = pointerID;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
