package benchmark;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Bench {
    public static void main(String[] args) throws IOException {
        int itr = 0;
        String hostAddress = null;
        for (String arg : args) {
            if (arg.matches("\\d*")) {
                itr = Integer.valueOf(arg);
            } else {
                hostAddress = arg;
            }
        }
        if (hostAddress == null) {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        }

        System.setProperty("jgroups.bind_addr", hostAddress);
        DefaultCacheManager cacheManager = new DefaultCacheManager("infinispan.xml");
        Cache<Object, Object> cache = cacheManager.getCache();

        System.out.println("Start " + cacheManager.getNodeAddress());
        if (itr > 0) {
            //WarmUp
            IntStream.range(0, 3).forEach(i -> IntStream.range(0, 100_000).count());
            bench(itr, cache);
        }

        System.out.println("Press Enter to print the cache contents, Ctrl+D/Ctrl+Z to stop.");
        while (System.in.read() > 0) {
            System.out.printf("size:%d%n", cache.size());
        }
        cacheManager.stop();
    }

    @SafeVarargs
    public static void bench(int itr, Cache<Object, Object>... caches) {
        Stream.of(caches).forEach(c -> {
            long start = System.currentTimeMillis();
            IntStream.range(0, itr).forEach(i -> c.put(i, "abc"));
            System.out.printf("%7d: %6d ms%n", itr, System.currentTimeMillis() - start);
        });
    }
}
