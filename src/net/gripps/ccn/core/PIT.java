package net.gripps.ccn.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by kanemih on 2018/11/02.
 */
public class PIT extends FIB{

    public PIT(HashMap<String, LinkedList<Face>> table) {
        super(table);
    }

    public PIT() {
        super();
    }

    public boolean removeFace(String prefix, Long id){
        if(this.getTable().containsKey(prefix)){
            LinkedList<Face> fList = this.getTable().get(prefix);
            Face retFace = null;
            Iterator<Face> fIte = fList.iterator();
            while(fIte.hasNext()){
                Face face = fIte.next();
                if(face.getPointerID().equals(id)){
                    retFace = face;
                    break;

                }
            }
            fList.remove(retFace);
            return true;

        }else{
            return false;
        }
    }
}
