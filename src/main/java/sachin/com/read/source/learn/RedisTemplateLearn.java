package sachin.com.read.source.learn;

import kotlin.reflect.jvm.internal.impl.protobuf.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RedisTemplateLearn {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTemplateLearn.class);
    private static final String SORTED_SET_KEY = "alerts:frozen:sorted";
    private static final String LAST_SYNC_KEY = "agent:alert:lastSync";
    public static final String REDIS_MEMBER_TOKEN = "#";


    RedisTemplate redisTemplate;

    /**
     * RedisTemplate操作Redis事务会出现各种问题，如乐观锁失效
     *
     * RedisTemplate主要有两种方式：
     * 1，直接使用redisTemplate.multi()进行开启
     * 2，使用SessionCallback接口
     *
     * 直接使用redisTemplate.multi()方式出现问题网上有很多解决方式：
     *
     * 1，标记@Transaction
     * 2，template.setEnableTransactionSupport(true)：开启事务控制
     *
     * 这里主要说我在使用SessionCallback接口实现乐观锁时遇到的失效问题：先说解决方法
     *
     * 所有redis的操作命令都必须放在SessionCallback内部方法中
     * redisTemplate.watch()监听必须放在所有redis操作的最前面
     * 原因：
     *
     * 1，SessionCallback可以确保操作者为同一个线程，高并发情况下必须防止争抢
     * 2，watch()放在所有操作的最前面，是为了防止它失效
     *
     * 作者：WQL空想家
     * 链接：https://www.jianshu.com/p/fcc82a3b4192
     * 来源：简书
     * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
     * @param commodityid
     * @param userId
     * @return
     */
    public boolean seckKill_Service(String commodityid, int userId) {
        // 这个对象一次被一个线程调用，所有操作redisTemplate 都必须放在这个。
        SessionCallback<List> sessionCallback = new SessionCallback<List>() {
            @Override
            public List execute(RedisOperations operations) throws DataAccessException {

                // 1 判断商品id和用户id是否为空
                if (StringUtils.isEmpty(commodityid) || StringUtils.isEmpty(userId)) {
                    return null;
                }
                String commodity = "commodit" + ":" + commodityid;
                String user = "user:size";

                // 监听方法必须放在所有redis方法的最前面，不然高并发时会被其他方法干扰
                redisTemplate.watch(commodity);
                // 3 判断redis中commodityid是否存在，不存在则秒杀没有开始
                // todo redis 中放入value是Integer的时候才能 get强转为Integer
                Integer quality = (Integer) redisTemplate.opsForValue().get(commodity);
                if (quality == null) {
                    // 秒杀未开始
                    return null;
                }
                if (quality < 0) {
                    // 库存不够
                    return null;
                }
                // 判断用户是否重复操作
                if (redisTemplate.opsForSet().isMember(user, userId)) {
                    // 用户重复操作
                    return null;
                }
                // 开启事务
                operations.multi();
                // 6秒杀库存，库粗-1
                operations.opsForValue().decrement(commodity);
                // 秒杀成功的用户添加到set集合中
                operations.opsForSet().add(user, userId);
                List exec = operations.exec();
                return exec;
            }
        };

        redisTemplate.multi();
        List execute = (List) redisTemplate.execute(sessionCallback);
        if (execute == null || execute.size() == 0) {
            // 秒杀失败
            return false;
        } else {
            return true;
        }

    }

    public void removeDeprecatedAlerts(final Collection<InternalAlert> frozenAlerts){
        Cursor<ZSetOperations.TypedTuple<String>> scanCursor = redisTemplate.opsForZSet().scan(SORTED_SET_KEY, ScanOptions.NONE);
        final List<String> memebersToRemove = new ArrayList<>();
        while (scanCursor.hasNext()) {
           final ZSetOperations.TypedTuple<String> member = scanCursor.next();
           final  String[] tokens = member.getValue().split(REDIS_MEMBER_TOKEN);
            if (!frozenAlerts.contains(new InternalAlert())) {
                memebersToRemove.add(member.getValue());
            }
        }
        closeCursor(scanCursor);
        if (!memebersToRemove.isEmpty()) {
            final int blockSize = 20;
            int fromIndex=0;
            int toIndex = memebersToRemove.size() < blockSize ? memebersToRemove.size() : blockSize;
            do {
                // toIndex 元素不包含在内
                List<String> subListToRemove = memebersToRemove.subList(fromIndex, toIndex);
                redisTemplate.opsForZSet().remove(SORTED_SET_KEY, subListToRemove.toArray(new Object[subListToRemove.size()]));
                fromIndex = toIndex;
                toIndex = toIndex + blockSize < memebersToRemove.size() ? toIndex + blockSize : memebersToRemove.size();


            } while (fromIndex < memebersToRemove.size());
        }
    }

    private <E> void closeCursor(final Cursor<E> cursor) {
        try {
            if (!cursor.isClosed()) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
