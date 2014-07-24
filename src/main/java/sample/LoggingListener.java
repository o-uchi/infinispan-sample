package sample;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.annotation.TransactionCompleted;
import org.infinispan.notifications.cachelistener.annotation.TransactionRegistered;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;

import org.infinispan.notifications.cachelistener.event.TransactionCompletedEvent;
import org.infinispan.notifications.cachelistener.event.TransactionRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener(sync = false)
public class LoggingListener {
    //private Logger log = Logger.getLogger(LoggingListener.class);

    private Logger log = LoggerFactory.getLogger(LoggingListener.class);

    @CacheEntryCreated
    public void observeAdd(CacheEntryCreatedEvent<String, String> event) {
        if (event.isPre())
            return;
        log.info("Created: {} in cache {}", event.getKey(), event.getCache());
    }

    @CacheEntryModified
    public void observeUpdate(CacheEntryModifiedEvent<String, String> event) {
        if (event.isPre())
            return;
        log.info("Modified: {}={} in cache {}", event.getKey(), event.getValue(), event.getCache());
    }

    @CacheEntryRemoved
    public void observeRemove(CacheEntryRemovedEvent<String, String> event) {
        if (event.isPre())
            return;
        log.info("Removed: {} in cache {}", event.getKey(), event.getCache());
    }

    @CacheEntryVisited
    public void observeVisit(CacheEntryVisitedEvent<String, String> event) {
        if (event.isPre())
            return;
        log.info("Visited: {} in cache {}", event.getKey(), event.getCache());
    }

    @CacheEntriesEvicted
    public void observeEvict(CacheEntriesEvictedEvent<String, String> event) {
        if (event.isPre())
            return;
        log.info("Evicted: {} in cache {}", event.getEntries(), event.getCache());
    }

    @CacheEntryActivated
    public void observeActivate(CacheEntryActivatedEvent<String, String> event) {
        if (event.isPre())
            return;
        log.info("Activated: {} in cache {}", event.getKey(), event.getCache());
    }

    @TopologyChanged
    public void observeTopologyChange(TopologyChangedEvent<String, String> event) {
        if (event.isPre())
            return;

        log.info("Cache {} topology changed, new membership is {}", event.getCache().getName(), event.getConsistentHashAtEnd().getMembers());
    }

    @TransactionRegistered
    public void transactionRegistered(TransactionRegisteredEvent event) {
        log.info("Transaction Registered: {}", event);
    }

    @TransactionCompleted
    public void transactionCompleted(TransactionCompletedEvent event) {
        log.info("Transaction Completed: {}", event);
    }
}
