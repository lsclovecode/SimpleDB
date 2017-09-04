package simpledb;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 */
public class BufferPool {
    /** Bytes per page, including header. */
    public static final int PAGE_SIZE = 4096;

    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50;
    
    private int numPages;
    
    private LRUCache<PageId, Page> pages;
    private HashMap<PageId, Permissions> pagePermissions;
    private HashMap<TransactionId, ArrayList<PageId>> pageTransactions;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        this.numPages = numPages;
        pages = new LRUCache<PageId, Page>(numPages);
        pageTransactions = new HashMap<TransactionId, ArrayList<PageId>>();
        pagePermissions = new HashMap<PageId, Permissions>();
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, an page should be evicted and the new page
     * should be added in its place.
     *
     * @author hrily
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     * @throws simpledb.TransactionAbortedException
     * @throws simpledb.DbException
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        // Check if cache size is greeater than numPages
        if(pages.size() >= numPages){
            this.evictPage();
        }
        // If page already in buffer
        if(pages.containsKey(pid)){
            // Check if transaction can acquire lock
            // TODO: Implement locking mechanism
            // Add transaction id if possible
            pagePermissions.put(pid, perm);
            if(pageTransactions.get(tid) != null && 
                    !pageTransactions.get(tid).contains(pid))
                pageTransactions.get(tid).add(pid);
            // Return the page
            return pages.get(pid);
        }
        // Page not in Buffer
        // Add page to Buffer
        DbFile dbFile = Database.getCatalog().getDbFile(pid.getTableId());
        pages.set(pid, dbFile.readPage(pid));
        // Add transaction id to page
        if(!pageTransactions.containsKey(tid))
            pageTransactions.put(tid, new ArrayList<PageId>());
        pageTransactions.get(tid).add(pid);
        // Update page permissions
        pagePermissions.put(pid, perm);
        // return page
        return pages.get(pid);
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for proj1
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @throws java.io.IOException
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for proj1
    }

    /** 
     * Return true if the specified transaction has a lock on the specified page 
     * @param tid
     * @param p
     * @return 
     */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for proj1
        return false;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     * @throws java.io.IOException
     */
    public void transactionComplete(TransactionId tid, boolean commit)
        throws IOException {
        // some code goes here
        // not necessary for proj1
    }

    /**
     * Add a tuple to the specified table behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to(Lock 
     * acquisition is not needed for lab2). May block if the lock cannot 
     * be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and updates cached versions of any pages that have 
     * been dirtied so that future requests see up-to-date pages. 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     * @throws simpledb.DbException
     * @throws java.io.IOException
     * @throws simpledb.TransactionAbortedException
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        DbFile file = Database.getCatalog().getDbFile(tableId);
        ArrayList<Page> pageList = file.insertTuple(tid, t);
        for(Page page: pageList){
            page.markDirty(true, tid);
            pages.set(page.getId(), page);
            if(!pageTransactions.get(tid).contains(page.getId()))
                pageTransactions.get(tid).add(page.getId());
            pagePermissions.put(page.getId(), Permissions.READ_ONLY);
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from. May block if
     * the lock cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit.  Does not need to update cached versions of any pages that have 
     * been dirtied, as it is not possible that a new page was created during the deletion
     * (note difference from addTuple).
     *
     * @param tid the transaction adding the tuple.
     * @param t the tuple to add
     * @throws simpledb.DbException
     * @throws simpledb.TransactionAbortedException
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, TransactionAbortedException {
        int tableId = t.getRecordId().getPageId().getTableId();
        DbFile file = Database.getCatalog().getDbFile(tableId);
        Page page = file.deleteTuple(tid, t);
        page.markDirty(true, tid);
        pages.set(page.getId(), page);
        if(!pageTransactions.get(tid).contains(page.getId()))
            pageTransactions.get(tid).add(page.getId());
        pagePermissions.put(page.getId(), Permissions.READ_ONLY);
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     * @throws java.io.IOException
     */
    public synchronized void flushAllPages() throws IOException {
        Iterator<PageId> pageIds = this.pages.iterator();
        while(pageIds.hasNext()){
            PageId pageId = pageIds.next();
            Page page = pages.get(pageId);
            if(page.isDirty() != null)
                // Need to flush
                this.flushPage(pageId);
        }
    }

    /** 
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     * @param pid
     */
    public synchronized void discardPage(PageId pid) {
        this.pages.remove(pid);
        this.pagePermissions.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        DbFile dbFile = Database.getCatalog().getDbFile(pid.getTableId());
        dbFile.writePage(pages.get(pid));
    }

    /** 
     * Write all pages of the specified transaction to disk.
     * @param tid
     * @throws java.io.IOException
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        ArrayList<PageId> pageIds = pageTransactions.get(tid);
        for(PageId pid: pageIds)
            if(pages.containsKey(pid))
                flushPage(pid);
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        try{
            Page page = pages.evict();
            if(page.isDirty() != null)
                flushPage(page.getId());
        }catch (IOException ioe){
            throw new DbException("IOException: " + ioe.getMessage());
        }
    }

}