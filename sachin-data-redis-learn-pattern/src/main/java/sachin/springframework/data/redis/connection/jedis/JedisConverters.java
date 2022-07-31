package sachin.springframework.data.redis.connection.jedis;

import org.springframework.util.Assert;
import redis.clients.jedis.ListPosition;
import sachin.springframework.data.redis.connection.RedisListCommands;

public class JedisConverters {
    public static ListPosition toListPosition(RedisListCommands.Position source) {
        Assert.notNull(source, "list positions are mandatory");
        return (RedisListCommands.Position.AFTER.equals(source) ? ListPosition.AFTER : ListPosition.BEFORE);
    }
}
