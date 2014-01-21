package com.funintended.morsepals;

import com.squareup.otto.Bus;

/**
 * Created with IntelliJ IDEA.
 * User: dusting
 * Date: 6/6/13
 * Time: 8:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class BusProvider {
    private static final Bus BUS = new Bus();

    public static Bus getInstance() {
        return BUS;
    }

    private BusProvider() {
        // No instances.
    }
}
