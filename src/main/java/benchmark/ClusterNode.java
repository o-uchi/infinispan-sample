package benchmark;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.io.IOException;

import static org.infinispan.configuration.cache.CacheMode.*;
import static org.infinispan.transaction.TransactionMode.*;

public class ClusterNode {
    public static void main(String[] args) throws IOException {
        DefaultCacheManager cacheManager = new DefaultCacheManager(
                GlobalConfigurationBuilder
                        .defaultClusteredBuilder()
                        .transport()
                        .addProperty("configurationFile", "jgroups-tcp.xml")
                        .build()
        );
        cacheManager.defineConfiguration("repl", new ConfigurationBuilder()
                .transaction().transactionMode(TRANSACTIONAL)
                .clustering().cacheMode(REPL_SYNC).build());
        cacheManager.defineConfiguration("dist", new ConfigurationBuilder()
                .transaction().transactionMode(TRANSACTIONAL)
                .clustering().cacheMode(DIST_SYNC).hash().numOwners(2).build());
        cacheManager.startCaches("repl", "dist");

        System.out.println("Start " + cacheManager.getNodeAddress());
        System.out.println("Press Enter to print the cache contents, Ctrl+D/Ctrl+Z to stop.");
        while (System.in.read() > 0) {
            System.out.printf("repl size:%d%n", cacheManager.getCache("repl").size());
            System.out.printf("dist size:%d%n", cacheManager.getCache("dist").size());
        }
        cacheManager.stop();
    }
}
