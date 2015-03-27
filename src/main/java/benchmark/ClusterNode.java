package benchmark;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

import static org.infinispan.configuration.cache.CacheMode.*;
import static org.infinispan.transaction.TransactionMode.*;

public class ClusterNode {
    public static void main(String[] args) throws IOException {
        System.setProperty("jgroups.bind_addr", InetAddress.getLocalHost().getHostAddress());

        DefaultCacheManager cacheManager = new DefaultCacheManager(
                GlobalConfigurationBuilder
                        .defaultClusteredBuilder()
                        .transport()
                        .addProperty("configurationFile", "jgroups-tcp.xml")
                        .build(),
                new ConfigurationBuilder()
                        .transaction().transactionMode(TRANSACTIONAL)
                        .clustering().cacheMode(DIST_SYNC)
                        .hash().capacityFactor(0).numOwners(1)
                        .build()
        );
//        cacheManager.defineConfiguration("repl", new ConfigurationBuilder()
//                .transaction().transactionMode(TRANSACTIONAL)
//                .clustering().cacheMode(REPL_SYNC).build());
//        cacheManager.defineConfiguration("dist", new ConfigurationBuilder()
//                .transaction().transactionMode(TRANSACTIONAL)
//                .clustering().cacheMode(DIST_SYNC).hash().numOwners(2).build());
//        cacheManager.startCaches("repl", "dist");
        Cache<Object, Object> cache = cacheManager.getCache();
        cache.addListener(new TListener());

        System.out.println("Start " + cacheManager.getNodeAddress());
        System.out.println("Press Enter to print the cache contents, Ctrl+D/Ctrl+Z to stop.");
        while (System.in.read() > 0) {
//            System.out.printf("repl size:%d%n", cacheManager.getCache("repl").size());
//            System.out.printf("dist size:%d%n", cacheManager.getCache("dist").size());
            System.out.printf("size:%d%n", cache.size());
        }
        cacheManager.stop();
    }

    @Listener(sync = false)
    public static class TListener {

        private static final Logger logger = LoggerFactory.getLogger(TListener.class);

        //@CacheEntryCreated
        @CacheEntryModified
        public void created(Event<String, String> event){
            if (event.isPre()){
                return;
            }
            logger.info("created. {}  isOriginLocal:{}", event.getType());
        }
    }
}
