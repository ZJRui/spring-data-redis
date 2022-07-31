package sachin.springframework.data.redis.connection;

import java.util.List;

@SuppressWarnings("all")
public interface RedisListCommands {


    /**
     * 在设计上  redis 数据结构的命令 被设计为各种 command 接口，   每一个command中持有 JedisConnection
     *
     *
     */






    /**
     * LINSERT 的时候需要再 指定的 元素之前或之后 插入 某一个元素
     */
    enum Position{
        BEFORE,AFTER
    }


    /**
     *RPUSH KEY_NAME VALUE1..VALUEN
     *
     * @param key
     * @param values
     * @return
     */
    Long rPush(byte[] key, byte[]... values);


    /**
     * LPOS mylist c RANK 2  6   表示 返回 key为mylist的 redis数据结构中 第2个元素开始，总共6个元素
     * @param key
     * @param element
     * @param rank
     * @param count
     * @return
     */
    List<Long> lPos(byte[]key, byte[] element, Integer rank, Integer count);

    /**
     *
     * Redis命令 LINSERT key BEFORE|AFTER pivot value
     *
     * @param key
     * @param where
     * @param pivot
     * @param value
     * @return
     */

    Long lInsert(byte[] key, Position where, byte[] pivot ,byte[] value);


}
