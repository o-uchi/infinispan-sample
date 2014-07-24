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

public class TransactionMultiCache {
    public static void main(String[] args) throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        Configuration configuration = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).build();
        DefaultCacheManager cacheManager = new DefaultCacheManager(configuration);
        Cache<Object, Object> cache = cacheManager.getCache();
        TransactionManager transactionManager = cache.getAdvancedCache().getTransactionManager();

        //cacheManager.defineConfiguration("2", new ConfigurationBuilder().build());
        Cache<Object, Object> cache2 = cacheManager.getCache("2");

        transactionManager.begin();
        cache.put("key", "value");
        cache2.put("key2", "value2");
        transactionManager.rollback();
        System.out.println(cache.containsKey("key"));
        System.out.println(cache2.containsKey("key2"));

        transactionManager.begin();
        cache.put("key", "value");
        cache2.put("key2", "value2");
        transactionManager.commit();
        System.out.println(cache.containsKey("key"));
        System.out.println(cache2.containsKey("key2"));

        transactionManager.begin();
        cache2.put("1", "value1");
        cache2.put("2", "value2");
        transactionManager.rollback();
        System.out.println(cache2.containsKey("1"));
        System.out.println(cache2.containsKey("2"));
    }
}
