package benchmark;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.infinispan.configuration.cache.CacheMode.*;
import static org.infinispan.transaction.TransactionMode.*;

public class ClusteredCache {
    public static void main(String[] args) {
        DefaultCacheManager cacheManager = new DefaultCacheManager(
                GlobalConfigurationBuilder
                        .defaultClusteredBuilder()
                        .transport()
                        .addProperty("configurationFile", "jgroups-tcp.xml")
                        .build()
//                "_infinispan.xml"
        );
        cacheManager.defineConfiguration("repl", new ConfigurationBuilder()
                .transaction().transactionMode(TRANSACTIONAL)
                .clustering().cacheMode(REPL_SYNC).build());
        cacheManager.defineConfiguration("dist", new ConfigurationBuilder()
                .transaction().transactionMode(TRANSACTIONAL)
                .clustering().cacheMode(DIST_SYNC).hash().numOwners(2).build());

        Cache<Object, Object> repl = cacheManager.getCache("repl");
        Cache<Object, Object> dist = cacheManager.getCache("dist");

        //WarmUp
        IntStream.range(0, 3).forEach(i -> IntStream.range(0, 100_000).count());

//        bench(10_000, repl, dist);
        bench(100_000, repl, dist);

//        cacheManager.stop();
    }

    @SafeVarargs
    public static void bench(int itr, Cache<Object, Object>... caches) {
        Stream.of(caches).forEach(c -> {
            long start = System.currentTimeMillis();
            IntStream.range(0, itr).forEach(i -> c.put(i, "abc"));
            System.out.printf("[%5s] %7d: %6d ms%n", c.getName(),itr, System.currentTimeMillis() - start);
        });
    }
}
