package sachin.springframework.data.redis.connection.jedis;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.MultiKeyPipelineBase;
import sachin.springframework.data.redis.connection.RedisListCommands;

import java.util.List;

public class JedisListCommands implements RedisListCommands {

    /**
     * Command 中持有 connection
     */
    private final JedisConnection connection;

    public JedisListCommands(JedisConnection connection) {
        this.connection = connection;
    }

    /**
     *
     * @param key
     * @param values
     * @return
     */
    @Override
    public Long rPush(byte[] key, byte[]... values) {
        return null;
    }

    @Override
    public List<Long> lPos(byte[] key, byte[] element, Integer rank, Integer count) {
        return null;
    }

    @Override
    public Long lInsert(byte[] key, Position where, byte[] pivot, byte[] value) {


        return connection.invoker().just(BinaryJedis::linsert, MultiKeyPipelineBase::linsert,
                key,JedisConverters.toListPosition(where),pivot,value);
    }
}
