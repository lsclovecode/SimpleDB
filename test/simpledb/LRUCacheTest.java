/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simpledb;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hrishi
 */
public class LRUCacheTest {

    private LRUCache c;

    public LRUCacheTest() {
        this.c = new LRUCache<Integer, Integer>(2);
    }

    @Test
    public void testCacheStartsEmpty() {
        assertEquals(c.get(1), null);
    }

    @Test
    public void testSetBelowCapacity() {
        c.set(1, 1);
        assertEquals(c.get(1), 1);
        assertEquals(c.get(2), null);
        c.set(2, 4);
        assertEquals(c.get(1), 1);
        assertEquals(c.get(2), 4);
    }

    @Test
    public void testCapacityReachedOldestRemoved() {
        c.set(1, 1);
        c.set(2, 4);
        c.set(3, 9);
        assertEquals(c.get(1), null);
        assertEquals(c.get(2), 4);
        assertEquals(c.get(3), 9);
    }

    @Test
    public void testGetRenewsEntry() {
        c.set(1, 1);
        c.set(2, 4);
        assertEquals(c.get(1), 1);
        c.set(3, 9);
        assertEquals(c.get(1), 1);
        assertEquals(c.get(2), null);
        assertEquals(c.get(3), 9);
    }
}