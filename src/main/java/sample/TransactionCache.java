package sample;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.TransactionMode;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

public class TransactionCache {
    public static void main(String[] args) throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        Configuration configuration = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).build();
        DefaultCacheManager cacheManager = new DefaultCacheManager(configuration);
        Cache<Object, Object> cache = cacheManager.getCache();
        TransactionManager transactionManager = cache.getAdvancedCache().getTransactionManager();

        cache.addListener(new LoggingListener());

        transactionManager.begin();
        cache.put("key", "value");
        cache.put("key2", "value2");
        transactionManager.rollback();
        System.out.println(cache.size());
        System.out.println(cache.containsKey("key"));

        transactionManager.begin();
        cache.put("key", "value");
        cache.put("key2", "value2");
        transactionManager.commit();
        System.out.println(cache.size());
        System.out.println(cache.containsKey("key"));

        transactionManager.begin();
        cache.remove("key");
        cache.put("key2", "value2value2");
        cache.put("key3", "value3");
        transactionManager.rollback();
        System.out.println(cache.containsKey("key"));
        System.out.println(cache.get("key2"));
        System.out.println(cache.get("key3"));

        cache.remove("key");
        System.out.println(cache.containsKey("key"));
    }
}
