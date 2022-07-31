/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.core;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.query.SortQuery;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Interface that specified a basic set of Redis operations, implemented by {@link RedisTemplate}. Not often used but a
 * useful option for extensibility and testability (as it can be easily mocked or stubbed).
 *
 * 接口指定了一组基本的Redis操作，由RedisTemplate实现。不经常使用，但对于可扩展性和可测试性来说是一个有用的选择(因为它很容易被嘲笑或存根)。
 * @author Costin Leau
 * @author Christoph Strobl
 * @author Ninad Divadkar
 * @author Mark Paluch
 * @author ihaohong
 */
public interface RedisOperations<K, V> {

	/**
	 * Executes the given action within a Redis connection. Application exceptions thrown by the action object get
	 * propagated to the caller (can only be unchecked) whenever possible. Redis exceptions are transformed into
	 * appropriate DAO ones. Allows for returning a result object, that is a domain object or a collection of domain
	 * objects. Performs automatic serialization/deserialization for the given objects to and from binary data suitable
	 * for the Redis storage. Note: Callback code is not supposed to handle transactions itself! Use an appropriate
	 * transaction manager. Generally, callback code must not touch any Connection lifecycle methods, like close, to let
	 * the template do its work.
	 *
	 * 在Redis连接中执行给定的动作。只要可能，由操作对象抛出的应用程序异常就会传播给调用者(只能不检查)。Redis异常被转换成适当的DAO异常。
	 * 允许返回一个结果对象，这是一个域对象或域对象的集合。对适合Redis存储的二进制数据的给定对象执行自动序列化/反序列化。
	 * 注意:回调代码本身不应该处理事务!使用适当的事务管理器。通常，回调代码不能触及任何Connection生命周期方法，比如close，以便让模板完成它的工作。
	 * @param <T> return type
	 * @param action callback object that specifies the Redis action. Must not be {@literal null}.
	 * @return a result object returned by the action or <tt>null</tt>
	 */
	@Nullable
	<T> T execute(RedisCallback<T> action);

	/**
	 * Executes a Redis session. Allows multiple operations to be executed in the same session enabling 'transactional'
	 * capabilities through {@link #multi()} and {@link #watch(Collection)} operations.
	 *
	 * 执行一个Redis会话。允许在同一个会话中执行多个操作，通过multi()和watch(Collection)操作启用“事务性”功能。
	 *
	 * @param <T> return type
	 * @param session session callback. Must not be {@literal null}.
	 * @return result object returned by the action or <tt>null</tt>
	 */
	@Nullable
	<T> T execute(SessionCallback<T> session);

	/**
	 * Executes the given action object on a pipelined connection, returning the results. Note that the callback
	 * <b>cannot</b> return a non-null value as it gets overwritten by the pipeline. This method will use the default
	 * serializers to deserialize results
	 * 在管道连接上执行给定的操作对象，返回结果。注意，回调不能返回一个非空值，因为它被管道覆盖了。此方法将使用默认的序列化器对结果进行反序列化
	 *
	 * --------------
	 * 使用pipeline可以减少与redis通信次数，在一次通信中执行一系列命令
	 *  Spring中通过RedisTemplate.executePipelined使用流水线执行命令
	 *
	 * 函数提供两种回调方式SessionCalback/RedisCallback
	 * 与RedisTemplate.execute不同，executePipelined会自动将回调中每个命令的执行结果存入数组中返回，参数回调必须返回null，
	 * 否则将抛出异常
	 *  org.springframework.dao.InvalidDataAccessApiUsageException: Callback cannot return a non-null value as it
	 *  gets overwritten by the pipeline
	 *
	 * pipeline不是原子操作
	 *
	 * @param action callback object to execute
	 * @return list of objects returned by the pipeline
	 */
	List<Object> executePipelined(RedisCallback<?> action);

	/**
	 * Executes the given action object on a pipelined connection, returning the results using a dedicated serializer.
	 * Note that the callback <b>cannot</b> return a non-null value as it gets overwritten by the pipeline.
	 * 在管道连接上执行给定的操作对象，使用专用的序列化器返回结果。注意，回调不能返回一个非空值，因为它被管道覆盖了。
	 *
	 * @param action callback object to execute
	 * @param resultSerializer The Serializer to use for individual values or Collections of values. If any returned
	 *          values are hashes, this serializer will be used to deserialize both the key and value
	 * @return list of objects returned by the pipeline
	 */
	List<Object> executePipelined(final RedisCallback<?> action, final RedisSerializer<?> resultSerializer);

	/**
	 * Executes the given Redis session on a pipelined connection. Allows transactions to be pipelined. Note that the
	 * callback <b>cannot</b> return a non-null value as it gets overwritten by the pipeline.
	 *
	 * @param session Session callback
	 * @return list of objects returned by the pipeline
	 * 在管道连接上执行给定的Redis会话。允许事务被流水线处理。注意，回调不能返回一个非空值，因为它被管道覆盖了。
	 */
	List<Object> executePipelined(final SessionCallback<?> session);

	/**
	 * Executes the given Redis session on a pipelined connection, returning the results using a dedicated serializer.
	 * Allows transactions to be pipelined. Note that the callback <b>cannot</b> return a non-null value as it gets
	 * overwritten by the pipeline.
	 *
	 * 在管道连接上执行给定的Redis会话，使用专用的序列化器返回结果。允许事务被流水线处理。注意，回调不能返回一个非空值，因为它被管道覆盖了。
	 *
	 *
	 * 使用pipeline可以减少与redis通信次数，在一次通信中执行一系列命令
	 *  Spring中通过RedisTemplate.executePipelined使用流水线执行命令
	 *
	 * 函数提供两种回调方式SessionCalback/RedisCallback
	 * 与RedisTemplate.execute不同，executePipelined会自动将回调中每个命令的执行结果存入数组中返回，参数回调必须返回null，否则将抛出异常
	 *  org.springframework.dao.InvalidDataAccessApiUsageException: Callback cannot return a non-null value as it gets overwritten by the pipeline
	 *
	 * SessionCallback 接口中的返回值必须要是null，否则会报错
	 *
	 *
	 * 注意： pipeline不是原子操作，也就是说 pipeline 中的多个命令可能只有前一部分成功执行，后面的没有执行
	 *
	 *
	 * @param session Session callback
	 * @param resultSerializer
	 * @return list of objects returned by the pipeline
	 */
	List<Object> executePipelined(final SessionCallback<?> session, final RedisSerializer<?> resultSerializer);

	/**
	 * Executes the given {@link RedisScript}
	 *
	 * @param script The script to execute
	 * @param keys Any keys that need to be passed to the script
	 * @param args Any args that need to be passed to the script
	 * @return The return value of the script or null if {@link RedisScript#getResultType()} is null, likely indicating a
	 *         throw-away status reply (i.e. "OK")
	 */
	@Nullable
	<T> T execute(RedisScript<T> script, List<K> keys, Object... args);

	/**
	 * Executes the given {@link RedisScript}, using the provided {@link RedisSerializer}s to serialize the script
	 * arguments and result.
	 *
	 * @param script The script to execute
	 * @param argsSerializer The {@link RedisSerializer} to use for serializing args
	 * @param resultSerializer The {@link RedisSerializer} to use for serializing the script return value
	 * @param keys Any keys that need to be passed to the script
	 * @param args Any args that need to be passed to the script
	 * @return The return value of the script or null if {@link RedisScript#getResultType()} is null, likely indicating a
	 *         throw-away status reply (i.e. "OK")
	 */
	@Nullable
	<T> T execute(RedisScript<T> script, RedisSerializer<?> argsSerializer, RedisSerializer<T> resultSerializer,
			List<K> keys, Object... args);

	/**
	 * Allocates and binds a new {@link RedisConnection} to the actual return type of the method. It is up to the caller
	 * to free resources after use.
	 *分配并绑定一个新的RedisConnection到方法的实际返回类型。调用方有权在使用后释放资源。
	 *
	 * ------------
	 *
	 * execute(RedisCallback<?> action) 和 executePipelined(final SessionCallback<?> session)：执行一系列 Redis 命令，
	 * 是所有方法的基础，里面使用的连接资源会在执行后自动释放。
	 *
	 * executePipelined(RedisCallback<?> action) 和 executePipelined(final SessionCallback<?> session)：使用 PipeLine
	 * 执行一系列命令，连接资源会在执行后自动释放。
	 *
	 * executeWithStickyConnection(RedisCallback<T> callback)：执行一系列 Redis 命令，连接资源不会自动释放，各种 Scan
	 * 命令就是通过这个方法实现的，因为 Scan 命令会返回一个 Cursor，这个 Cursor 需要保持连接（会话），同时交给用户决定什么时候关闭。
	 *
	 * @param callback must not be {@literal null}.
	 * @return
	 * @since 1.8
	 */
	@Nullable
	<T extends Closeable> T executeWithStickyConnection(RedisCallback<T> callback);

	// -------------------------------------------------------------------------
	// Methods dealing with Redis Keys
	// -------------------------------------------------------------------------

	/**
	 * Copy given {@code sourceKey} to {@code targetKey}.
	 *
	 * @param sourceKey must not be {@literal null}.
	 * @param targetKey must not be {@literal null}.
	 * @param replace whether the key was copied. {@literal null} when used in pipeline / transaction.
	 * @return
	 * @see <a href="https://redis.io/commands/copy">Redis Documentation: COPY</a>
	 * @since 2.6
	 */
	@Nullable
	Boolean copy(K sourceKey, K targetKey, boolean replace);

	/**
	 * Determine if given {@code key} exists.
	 *
	 * @param key must not be {@literal null}.
	 * @return
	 * @see <a href="https://redis.io/commands/exists">Redis Documentation: EXISTS</a>
	 */
	@Nullable
	Boolean hasKey(K key);

	/**
	 * Count the number of {@code keys} that exist.
	 *
	 * @param keys must not be {@literal null}.
	 * @return The number of keys existing among the ones specified as arguments. Keys mentioned multiple times and
	 *         existing are counted multiple times.
	 * @see <a href="https://redis.io/commands/exists">Redis Documentation: EXISTS</a>
	 * @since 2.1
	 */
	@Nullable
	Long countExistingKeys(Collection<K> keys);


	/**
	 * Delete given {@code key}.
	 *
	 * @param key must not be {@literal null}.
	 * @return {@literal true} if the key was removed.
	 * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
	 */
	@Nullable
	Boolean delete(K key);

	/**
	 * Delete given {@code keys}.
	 *
	 * @param keys must not be {@literal null}.
	 * @return The number of keys that were removed. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
	 */
	@Nullable
	Long delete(Collection<K> keys);

	/**
	 * Unlink the {@code key} from the keyspace. Unlike with {@link #delete(Object)} the actual memory reclaiming here
	 * happens asynchronously.
	 * 从键空间中解除键的链接。与delete(Object)不同，这里实际的内存回收是异步发生的。
	 *
	 * 自 4.0.0 起可用。
	 *
	 * 时间复杂度：对于删除的每个键，无论其大小如何，均为 O(1)。然后该命令在不同的线程中执行 O(N) 工作以回收内存，其中 N 是组成已删除对象的分配数。
	 *
	 * 此命令与DEL非常相似：它删除指定的键。就像DEL一样，如果键不存在，则会被忽略。然而，该命令在不同的线程中执行实际的内存回收，因此它不会阻塞，而DEL会阻塞。这就是命令名称的来源：该命令只是从键空间中取消键的链接。实际的删除将在稍后异步进行。
	 *
	 * 返回值
	 * 整数回复：未链接的键数。
	 *
	 * 例子
	 * Redis> SET key “你好”
	 * “好的”
	 * Redis> SET key2“世界”
	 * “好的”
	 * Redis> UNLINK key1 key2 key3
	 * （整数）2
	 *
	 * @param key must not be {@literal null}.
	 * @return The number of keys that were removed. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/unlink">Redis Documentation: UNLINK</a>
	 * @since 2.1
	 */
	@Nullable
	Boolean unlink(K key);

	/**
	 * Unlink the {@code keys} from the keyspace. Unlike with {@link #delete(Collection)} the actual memory reclaiming
	 * here happens asynchronously.
	 *
	 * 从键空间中解除键的链接。与delete(Collection)不同，这里实际的内存回收是异步进行的。
	 *
	 * @param keys must not be {@literal null}.
	 * @return The number of keys that were removed. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/unlink">Redis Documentation: UNLINK</a>
	 * @since 2.1
	 */
	@Nullable
	Long unlink(Collection<K> keys);

	/**
	 * Determine the type stored at {@code key}.
	 *
	 * @param key must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/type">Redis Documentation: TYPE</a>
	 */
	@Nullable
	DataType type(K key);

	/**
	 * Find all keys matching the given {@code pattern}.
	 *
	 * @param pattern must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/keys">Redis Documentation: KEYS</a>
	 */
	@Nullable
	Set<K> keys(K pattern);

	/**
	 * Return a random key from the keyspace.
	 *
	 * 自 1.0.0 起可用。
	 *
	 * 时间复杂度： O(1)
	 *
	 * 从当前选择的数据库中返回一个随机密钥。
	 *
	 * 返回值
	 * 批量字符串回复：随机密钥，或nil当数据库为空时。
	 *
	 * @return {@literal null} no keys exist or when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/randomkey">Redis Documentation: RANDOMKEY</a>
	 */
	@Nullable
	K randomKey();

	/**
	 * Rename key {@code oldKey} to {@code newKey}.
	 *
	 * @param oldKey must not be {@literal null}.
	 * @param newKey must not be {@literal null}.
	 * @see <a href="https://redis.io/commands/rename">Redis Documentation: RENAME</a>
	 */
	void rename(K oldKey, K newKey);

	/**
	 * Rename key {@code oleName} to {@code newKey} only if {@code newKey} does not exist.
	 *
	 * @param oldKey must not be {@literal null}.
	 * @param newKey must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/renamenx">Redis Documentation: RENAMENX</a>
	 */
	@Nullable
	Boolean renameIfAbsent(K oldKey, K newKey);

	/**
	 * Set time to live for given {@code key}.
	 *
	 * @param key must not be {@literal null}.
	 * @param timeout
	 * @param unit must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 */
	@Nullable
	Boolean expire(K key, long timeout, TimeUnit unit);

	/**
	 * Set time to live for given {@code key}.
	 *
	 * @param key must not be {@literal null}.
	 * @param timeout must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @throws IllegalArgumentException if the timeout is {@literal null}.
	 * @since 2.3
	 */
	@Nullable
	default Boolean expire(K key, Duration timeout) {

		Assert.notNull(timeout, "Timeout must not be null");

		return TimeoutUtils.hasMillis(timeout) ? expire(key, timeout.toMillis(), TimeUnit.MILLISECONDS)
				: expire(key, timeout.getSeconds(), TimeUnit.SECONDS);
	}

	/**
	 * Set the expiration for given {@code key} as a {@literal date} timestamp.
	 *
	 * @param key must not be {@literal null}.
	 * @param date must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 */
	@Nullable
	Boolean expireAt(K key, Date date);

	/**
	 * Set the expiration for given {@code key} as a {@literal date} timestamp.
	 *
	 * @param key must not be {@literal null}.
	 * @param expireAt must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @throws IllegalArgumentException if the instant is {@literal null} or too large to represent as a {@code Date}.
	 * @since 2.3
	 */
	@Nullable
	default Boolean expireAt(K key, Instant expireAt) {

		Assert.notNull(expireAt, "Timestamp must not be null");

		return expireAt(key, Date.from(expireAt));
	}

	/**
	 * Remove the expiration from given {@code key}.
	 *
	 * @param key must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/persist">Redis Documentation: PERSIST</a>
	 *
	 *
	 * ==
	 *自 2.2.0 起可用。
	 *
	 * 时间复杂度： O(1)
	 *
	 * 删除 上的现有超时key，将密钥从volatile（设置了过期时间的密钥）变为持久性（一个永不过期的密钥，因为没有关联超时）。
	 *
	 * 返回值
	 * 整数回复，特别是：
	 *
	 * 1 如果超时被删除。
	 * 0如果key不存在或没有关联的超时。
	 *
	 * Examples
	 * redis> SET mykey "Hello"
	 * "OK"
	 * redis> EXPIRE mykey 10
	 * (integer) 1
	 * redis> TTL mykey
	 * (integer) 10
	 * redis> PERSIST mykey
	 * (integer) 1
	 * redis> TTL mykey
	 * (integ
	 */
	@Nullable
	Boolean persist(K key);

	/**
	 * Move given {@code key} to database with {@code index}.
	 *
	 * @param key must not be {@literal null}.
	 * @param dbIndex
	 * @return {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/move">Redis Documentation: MOVE</a>
	 */
	@Nullable
	Boolean move(K key, int dbIndex);

	/**
	 * Retrieve serialized version of the value stored at {@code key}.
	 * 检索存储在{@code key}的值的序列化版本。
	 *
	 * @param key must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/dump">Redis Documentation: DUMP</a>
	 */
	@Nullable
	byte[] dump(K key);

	/**
	 * Create {@code key} using the {@code serializedValue}, previously obtained using {@link #dump(Object)}.
	 * 使用先前使用dump(Object)获得的serializedValue创建键。
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @param timeToLive
	 * @param unit must not be {@literal null}.
	 * @see <a href="https://redis.io/commands/restore">Redis Documentation: RESTORE</a>
	 *
	 * ==========
	 *自 2.6.0 起可用。
	 *
	 * 时间复杂度： O(1) 来创建新键和额外的 O(N*M) 来重建序列化值，其中 N 是组成值的 Redis 对象的数量，M 是它们的平均大小。对于小字符串值，时间复杂度为 O(1)+O(1*M)，其中 M 很小，所以简单为 O(1)。然而，对于排序集合值，复杂度是 O(N*M*log(N))，因为将值插入到排序集合中是 O(log(N))。
	 *
	 * 创建与通过反序列化提供的序列化值（通过DUMP获得）获得的值关联的键。
	 *
	 * 如果ttl为 0，则创建的密钥没有任何过期，否则设置指定的过期时间（以毫秒为单位）。
	 *
	 * 如果使用了ABSTTL修饰符，ttl则应表示密钥将过期的绝对 Unix 时间戳（以毫秒为单位）。（Redis 5.0 或更高版本）。
	 *
	 * 出于驱逐目的，您可以使用IDLETIME或FREQ修饰符。有关更多信息，请参阅 OBJECT（Redis 5.0 或更高版本）。
	 *
	 * key除非您使用REPLACE修饰符（Redis 3.0 或更高版本），否则RESTORE将在已存在时返回“目标键名称正忙”错误。
	 *
	 * RESTORE检查 RDB 版本和数据校验和。如果它们不匹配，则返回错误。
	 *
	 *
	 * Examples
	 * redis> DEL mykey
	 * 0
	 * redis> RESTORE mykey 0 "\n\x17\x17\x00\x00\x00\x12\x00\x00\x00\x03\x00\
	 *                         x00\xc0\x01\x00\x04\xc0\x02\x00\x04\xc0\x03\x00\
	 *                         xff\x04\x00u#<\xc0;.\xe9\xdd"
	 * OK
	 * redis> TYPE mykey
	 * list
	 * redis> LRANGE mykey 0 -1
	 * 1) "1"
	 * 2) "2"
	 * 3) "3"
	 *
	 *
	 */
	default void restore(K key, byte[] value, long timeToLive, TimeUnit unit) {
		restore(key, value, timeToLive, unit, false);
	}

	/**
	 * Create {@code key} using the {@code serializedValue}, previously obtained using {@link #dump(Object)}.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @param timeToLive
	 * @param unit must not be {@literal null}.
	 * @param replace use {@literal true} to replace a potentially existing value instead of erroring.
	 * @since 2.1
	 * @see <a href="https://redis.io/commands/restore">Redis Documentation: RESTORE</a>
	 */
	void restore(K key, byte[] value, long timeToLive, TimeUnit unit, boolean replace);

	/**
	 * Get the time to live for {@code key} in seconds.
	 *
	 * @param key must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
	 */
	@Nullable
	Long getExpire(K key);

	/**
	 * Get the time to live for {@code key} in and convert it to the given {@link TimeUnit}.
	 *
	 * @param key must not be {@literal null}.
	 * @param timeUnit must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @since 1.8
	 */
	@Nullable
	Long getExpire(K key, TimeUnit timeUnit);

	/**
	 * Sort the elements for {@code query}.
	 *
	 * @param query must not be {@literal null}.
	 * @return the results of sort. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
	 */
	@Nullable
	List<V> sort(SortQuery<K> query);

	/**
	 * Sort the elements for {@code query} applying {@link RedisSerializer}.
	 *
	 * @param query must not be {@literal null}.
	 * @return the deserialized results of sort. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
	 */
	@Nullable
	<T> List<T> sort(SortQuery<K> query, RedisSerializer<T> resultSerializer);

	/**
	 * Sort the elements for {@code query} applying {@link BulkMapper}.
	 *
	 * @param query must not be {@literal null}.
	 * @return the deserialized results of sort. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
	 */
	@Nullable
	<T> List<T> sort(SortQuery<K> query, BulkMapper<T, V> bulkMapper);

	/**
	 * Sort the elements for {@code query} applying {@link BulkMapper} and {@link RedisSerializer}.
	 *
	 * @param query must not be {@literal null}.
	 * @return the deserialized results of sort. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
	 */
	@Nullable
	<T, S> List<T> sort(SortQuery<K> query, BulkMapper<T, S> bulkMapper, RedisSerializer<S> resultSerializer);

	/**
	 * Sort the elements for {@code query} and store result in {@code storeKey}.
	 *
	 * @param query must not be {@literal null}.
	 * @param storeKey must not be {@literal null}.
	 * @return number of values. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/sort">Redis Documentation: SORT</a>
	 */
	@Nullable
	Long sort(SortQuery<K> query, K storeKey);

	// -------------------------------------------------------------------------
	// Methods dealing with Redis Transactions
	// -------------------------------------------------------------------------

	/**
	 * Watch given {@code key} for modifications during transaction started with {@link #multi()}.
	 *
	 *
	 * wath的用法参考org.springframework.data.redis.core.RedisTemplateIntegrationTests#testWatch()
	 * 就是说 watch 需要在  事务开始multi方法之前执行， 如果在watch之后，事务开始之前有其他线程修改了指定的key，那么事务将不会执行
	 * 因为 watch命令是单独执行的 ，multi方法开启事务 之后的命令是一起执行的，这是两个阶段。
	 *
	 * 有些场景是 watch之后 通过redis get来获取 被watch key的值，这个值会在multi方法中的事务中被使用。 那么问题就是如何保证 get 指定
	 * watch的key得到的值 在事务执行的时候 再次 get 仍然是不变的，如果发生了变化那么就会影响事务的逻辑。 比如说
	 *
	 * step 1 wahtc key1
	 * step2 get key1 =value1
	 *
	 * step 3 if(value1=="A"){
	 *     step 4开启事务 multi
	 *          step5  使用value1
	 *     step 6 事务提价
	 * }else{
	 *     //不开启事务
	 * }
	 *
	 *
	 *
	 * @param key must not be {@literal null}.
	 * @see <a href="https://redis.io/commands/watch">Redis Documentation: WATCH</a>
	 */
	void watch(K key);

	/**
	 * Watch given {@code keys} for modifications during transaction started with {@link #multi()}.
	 *在使用 multi() 开始的事务期间观察给定的键以进行修改。
	 * Marks the given keys to be watched for conditional execution of a transaction.
	 * 标记要监视的给定键以有条件地执行事务。
	 *
	 *
	 *
	 *   List list = (List) redisTemplate.execute((RedisOperations res) ->
	 *         {
	 *             //设置监控key,在exec执行前如果这个key对应的值，发生了变化，事务不执行
	 *             //通常监控的key可以是ID，也可以是一个对象
	 *             res.watch("wanwan");
	 *             // 其实watch可以注释掉，或者设置成不监控
	 *             res.unwatch();
	 *             //开启事务，在exec执行前
	 *             res.multi();
	 *             res.opsForValue().increment("wanwan", 1);
	 *             res.opsForValue().set("wanwan2", "我的小兔兔1");
	 *             Object value2 = res.opsForValue().get("wanwan2");
	 *             System.out.println("命令在队列，所以取值为空" + value2 + "----");
	 *             res.opsForValue().set("wanwan3", "我的小兔兔3");
	 *             Object value3 = res.opsForValue().get("wanwan3");
	 *             System.out.println("命令在队列，所以取值为空" + value3 + "----");
	 *             return res.exec();
	 *         });
	 *
	 *     发现其实事务就是基于SessionCallback实现了一个watch如果被监控的键发生了变化就会取消事务，
	 *     没有变化九执行事务（注意：即使被赋予了相同的值，同样视为发生变化，不予执行事务）
	 *     redis通过watch来监测数据，在执行exec前，监测的数据被其他人更改会抛出错误，取消执行。而exec执行时，
	 *     redis保证不会插入其他人语句来实现隔离。（可以预见到此机制如果事务中包裹过多的执行长指令，可能导致长时间阻塞其他人）
	 *
	 *
	 *
	 *
	 * @param keys must not be {@literal null}.
	 * @see <a href="https://redis.io/commands/watch">Redis Documentation: WATCH</a>
	 */
	void watch(Collection<K> keys);

	/**
	 * Flushes all the previously {@link #watch(Object)} keys.
	 *
	 * @see <a href="https://redis.io/commands/unwatch">Redis Documentation: UNWATCH</a>
	 */
	void unwatch();

	/**
	 * Mark the start of a transaction block. <br>
	 * Commands will be queued and can then be executed by calling {@link #exec()} or rolled back using {@link #discard()}
	 * <p>
	 *     标记事务块的开始。命令将被排队，然后可以通过调用exec()或使用discard()回滚来执行命令
	 *
	 * @see <a href="https://redis.io/commands/multi">Redis Documentation: MULTI</a>
	 */
	void multi();

	/**
	 * Discard all commands issued after {@link #multi()}.
	 *
	 * @see <a href="https://redis.io/commands/discard">Redis Documentation: DISCARD</a>
	 */
	void discard();

	/**
	 * Executes all queued commands in a transaction started with {@link #multi()}. <br>
	 * If used along with {@link #watch(Object)} the operation will fail if any of watched keys has been modified.
	 *执行以multi()开始的事务中所有排队的命令。如果与watch(Object)一起使用，则如果任何被监视的键被修改，操作将失败。
	 *
	 * @return List of replies for each executed command.
	 * @see <a href="https://redis.io/commands/exec">Redis Documentation: EXEC</a>
	 */
	List<Object> exec();

	/**
	 * Execute a transaction, using the provided {@link RedisSerializer} to deserialize any results that are byte[]s or
	 * Collections of byte[]s. If a result is a Map, the provided {@link RedisSerializer} will be used for both the keys
	 * and values. Other result types (Long, Boolean, etc) are left as-is in the converted results. Tuple results are
	 * automatically converted to TypedTuples.
	 *
	 * 执行一个事务，使用提供的RedisSerializer来反序列化任何字节[]s或字节[]s的集合的结果。如果结果是Map，
	 * 则提供的RedisSerializer将同时用于键和值。其他结果类型(Long、Boolean等)在转换后的结果中保持原样。
	 * 元组结果会自动转换为typedtuple。
	 *
	 * @param valueSerializer The {@link RedisSerializer} to use for deserializing the results of transaction exec
	 * @return The deserialized results of transaction exec
	 */
	List<Object> exec(RedisSerializer<?> valueSerializer);

	// -------------------------------------------------------------------------
	// Methods dealing with Redis Server Commands
	// -------------------------------------------------------------------------

	/**
	 * Request information and statistics about connected clients.
	 *
	 * @return {@link List} of {@link RedisClientInfo} objects.
	 * @since 1.3
	 */
	@Nullable
	List<RedisClientInfo> getClientList();

	/**
	 * Closes a given client connection identified by {@literal ip:port} given in {@code client}.
	 *
	 * @param host of connection to close.
	 * @param port of connection to close
	 * @since 1.3
	 */
	void killClient(String host, int port);

	/**
	 * Change redis replication setting to new master.
	 *
	 * @param host must not be {@literal null}.
	 * @param port
	 * @since 1.3
	 * @see <a href="https://redis.io/commands/slaveof">Redis Documentation: SLAVEOF</a>
	 */
	void slaveOf(String host, int port);

	/**
	 * Change server into master.
	 *
	 * @since 1.3
	 * @see <a href="https://redis.io/commands/slaveof">Redis Documentation: SLAVEOF</a>
	 */
	void slaveOfNoOne();

	/**
	 * Publishes the given message to the given channel.
	 *
	 * @param destination the channel to publish to, must not be {@literal null}.
	 * @param message message to publish
	 * @return the number of clients that received the message
	 * @see <a href="https://redis.io/commands/publish">Redis Documentation: PUBLISH</a>
	 */
	void convertAndSend(String destination, Object message);

	// -------------------------------------------------------------------------
	// Methods to obtain specific operations interface objects.
	// -------------------------------------------------------------------------

	// operation types

	/**
	 * Returns the cluster specific operations interface.
	 *
	 * @return never {@literal null}.
	 * @since 1.7
	 */
	ClusterOperations<K, V> opsForCluster();

	/**
	 * Returns geospatial specific operations interface.
	 *
	 * @return never {@literal null}.
	 * @since 1.8
	 */
	GeoOperations<K, V> opsForGeo();

	/**
	 * Returns geospatial specific operations interface bound to the given key.
	 *
	 * @param key must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 1.8
	 */
	BoundGeoOperations<K, V> boundGeoOps(K key);

	/**
	 * Returns the operations performed on hash values.
	 *
	 * @param <HK> hash key (or field) type
	 * @param <HV> hash value type
	 * @return hash operations
	 */
	<HK, HV> HashOperations<K, HK, HV> opsForHash();

	/**
	 * Returns the operations performed on hash values bound to the given key. * @param <HK> hash key (or field) type
	 *
	 * @param <HV> hash value type
	 * @param key Redis key
	 * @return hash operations bound to the given key.
	 */
	<HK, HV> BoundHashOperations<K, HK, HV> boundHashOps(K key);

	/**
	 * @return
	 * @since 1.5
	 */
	HyperLogLogOperations<K, V> opsForHyperLogLog();

	/**
	 * Returns the operations performed on list values.
	 *
	 * @return list operations
	 */
	ListOperations<K, V> opsForList();

	/**
	 * Returns the operations performed on list values bound to the given key.
	 *
	 * @param key Redis key
	 * @return list operations bound to the given key
	 */
	BoundListOperations<K, V> boundListOps(K key);

	/**
	 * Returns the operations performed on set values.
	 *
	 * @return set operations
	 */
	SetOperations<K, V> opsForSet();

	/**
	 * Returns the operations performed on set values bound to the given key.
	 * 返回对绑定到给定键的集合值所执行的操作。
	 *
	 * @param key Redis key
	 * @return set operations bound to the given key
	 */
	BoundSetOperations<K, V> boundSetOps(K key);

	/**
	 * Returns the operations performed on Streams.
	 *
	 * @return stream operations.
	 * @since 2.2
	 */
	<HK, HV> StreamOperations<K, HK, HV> opsForStream();

	/**
	 * Returns the operations performed on Streams.
	 *
	 * @param hashMapper the {@link HashMapper} to use when converting {@link ObjectRecord}.
	 * @return stream operations.
	 * @since 2.2
	 */
	<HK, HV> StreamOperations<K, HK, HV> opsForStream(HashMapper<? super K, ? super HK, ? super HV> hashMapper);

	/**
	 * Returns the operations performed on Streams bound to the given key.
	 *
	 * @return stream operations.
	 * @since 2.2
	 */
	<HK, HV> BoundStreamOperations<K, HK, HV> boundStreamOps(K key);

	/**
	 * Returns the operations performed on simple values (or Strings in Redis terminology).
	 *返回对简单值(Redis术语为string)所执行的操作。
	 * @return value operations
	 */
	ValueOperations<K, V> opsForValue();

	/**
	 * Returns the operations performed on simple values (or Strings in Redis terminology) bound to the given key.
	 * 返回对绑定到给定键的简单值(Redis术语为string)所执行的操作。
	 * 返回对对给定密钥的简单值（或redis术语中的字符串）执行的操作
	 * @param key Redis key
	 * @return value operations bound to the given key
	 */
	BoundValueOperations<K, V> boundValueOps(K key);

	/**
	 * Returns the operations performed on zset values (also known as sorted sets).
	 *
	 * @return zset operations
	 */
	ZSetOperations<K, V> opsForZSet();

	/**
	 * Returns the operations performed on zset values (also known as sorted sets) bound to the given key.
	 *
	 * @param key Redis key
	 * @return zset operations bound to the given key.
	 */
	BoundZSetOperations<K, V> boundZSetOps(K key);

	/**
	 * @return the key {@link RedisSerializer}.
	 */
	RedisSerializer<?> getKeySerializer();

	/**
	 * @return the value {@link RedisSerializer}.
	 */
	RedisSerializer<?> getValueSerializer();

	/**
	 * @return the hash key {@link RedisSerializer}.
	 */
	RedisSerializer<?> getHashKeySerializer();

	/**
	 * @return the hash value {@link RedisSerializer}.
	 */
	RedisSerializer<?> getHashValueSerializer();

}
