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
}
