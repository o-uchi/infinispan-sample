package benchmark;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.util.stream.IntStream;

import static org.infinispan.transaction.TransactionMode.*;

public class SimpleBench {
    public static void main(String[] args) {
        DefaultCacheManager cacheManager = new DefaultCacheManager();
        cacheManager.defineConfiguration("tran", new ConfigurationBuilder().transaction().transactionMode(TRANSACTIONAL).build());

        Cache<Object, Object> def = cacheManager.getCache();
        Cache<Object, Object> tran = cacheManager.getCache("tran");

        //WarmUp
        IntStream.range(0, 3).forEach(i -> IntStream.range(0, 100_000).count());

        System.out.println("DefaultCache:");
        bench(10_000, def);
        bench(100_000, def);
        bench(1_000_000, def);

        System.out.println("Transaction:");
        bench(10_000, tran);
        bench(100_000, tran);
        bench(1_000_000, tran);
    }

    public static void bench(int itr, Cache<Object, Object> cache) {
        long start = System.currentTimeMillis();
        IntStream.range(0, itr).forEach(i -> cache.put(i, "abc"));
        System.out.printf("%7d: %6d ms%n", itr, System.currentTimeMillis() - start);
        cache.clear();
    }
}
