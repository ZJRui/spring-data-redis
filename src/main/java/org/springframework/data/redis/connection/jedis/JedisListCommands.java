/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.redis.connection.jedis;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.MultiKeyPipelineBase;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.params.LPosParams;

import java.util.Collections;
import java.util.List;

import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author dengliming
 * @since 2.0
 */
@SuppressWarnings("all")
class JedisListCommands implements RedisListCommands {

	/**
	 * command 中持有 jedisConnection
	 */
	private final JedisConnection connection;

	JedisListCommands(JedisConnection connection) {
		this.connection = connection;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#rPush(byte[], byte[][])
	 *
	 *
	 *
	 *   redis 中就是： RPUSH KEY_NAME VALUE1..VALUEN
	 */
	@Override
	public Long rPush(byte[] key, byte[]... values) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(BinaryJedis::rpush, MultiKeyPipelineBase::rpush, key, values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#l{lPos(byte[], byte[], java.lang.Integer, java.lang.Integer)
	 *
	 *
	 * redis中就是：LPOS mylist c RANK 2  6   表示 返回 key为mylist的 redis数据结构中 第2个元素开始，总共6个元素
	 */
	@Override
	public List<Long> lPos(byte[] key, byte[] element, @Nullable Integer rank, @Nullable Integer count) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(element, "Element must not be null!");

		LPosParams params = new LPosParams();
		if (rank != null) {
			params.rank(rank);
		}

		if (count != null) {
			return connection.invoke().just(BinaryJedis::lpos, MultiKeyPipelineBase::lpos, key, element, params, count);
		}

		return connection.invoke().from(BinaryJedis::lpos, MultiKeyPipelineBase::lpos, key, element, params)
				.getOrElse(Collections::singletonList, Collections::emptyList);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lPush(byte[], byte[][])
	 */
	@Override
	public Long lPush(byte[] key, byte[]... values) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(values, "Values must not be null!");
		Assert.noNullElements(values, "Values must not contain null elements!");

		return connection.invoke().just(BinaryJedis::lpush, MultiKeyPipelineBase::lpush, key, values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#rPushX(byte[], byte[])
	 */
	@Override
	public Long rPushX(byte[] key, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return connection.invoke().just(BinaryJedis::rpushx, MultiKeyPipelineBase::rpushx, key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lPushX(byte[], byte[])
	 */
	@Override
	public Long lPushX(byte[] key, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return connection.invoke().just(BinaryJedis::lpushx, MultiKeyPipelineBase::lpushx, key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lLen(byte[])
	 */
	@Override
	public Long lLen(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(BinaryJedis::llen, MultiKeyPipelineBase::llen, key);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lRange(byte[], long, long)
	 */
	@Override
	public List<byte[]> lRange(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(BinaryJedis::lrange, MultiKeyPipelineBase::lrange, key, start, end);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lTrim(byte[], long, long)
	 */
	@Override
	public void lTrim(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		connection.invokeStatus().just(BinaryJedis::ltrim, MultiKeyPipelineBase::ltrim, key, start, end);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lIndex(byte[], long)
	 */
	@Override
	public byte[] lIndex(byte[] key, long index) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(BinaryJedis::lindex, MultiKeyPipelineBase::lindex, key, index);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lInsert(byte[], org.springframework.data.redis.connection.RedisListCommands.Position, byte[], byte[])
	 *
	 *
	 * redis 命令： LINSERT key BEFORE|AFTER pivot value
	 *
	 */
	@Override
	public Long lInsert(byte[] key, Position where, byte[] pivot, byte[] value) {

		Assert.notNull(key, "Key must not be null!");

		/**
		 * JedisInvoke 承载执行逻辑，
		 * 我们对命令的抽象就是 输入一个或者多个参数 返回一个值。 因此 JedisInvoke 中定义了 函数式接口。
		 * 每个命令 的方法作为函数是接口的实现。
		 *
		 * BinaryJedis::linsert表示对函数式接口 ConnectionFunction4<T1, T2, T3, T4, R> function 的实现。
		 *
		 * 他对应着 list的insert。 需要的参数在方法后面定义。
		 *
		 * 因为JedisInvoke 通过方法参数来承载 各个命令的执行逻辑， 谁来执行 这个命令呢 就是JedisInvoker 中的
		 * Synchronizer属性。
		 *
		 *
		 */
		return connection.invoke().just(BinaryJedis::linsert, MultiKeyPipelineBase::linsert, key,
				JedisConverters.toListPosition(where), pivot, value);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lMove(byte[], byte[], org.springframework.data.redis.connection.RedisListCommands.Direction, org.springframework.data.redis.connection.RedisListCommands.Direction)
	 */
	@Override
	public byte[] lMove(byte[] sourceKey, byte[] destinationKey, Direction from, Direction to) {

		Assert.notNull(sourceKey, "Source key must not be null!");
		Assert.notNull(destinationKey, "Destination key must not be null!");
		Assert.notNull(from, "From direction must not be null!");
		Assert.notNull(to, "To direction must not be null!");

		return connection.invoke().just(BinaryJedis::lmove, MultiKeyPipelineBase::lmove, sourceKey, destinationKey,
				ListDirection.valueOf(from.name()), ListDirection.valueOf(to.name()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#bLMove(byte[], byte[], org.springframework.data.redis.connection.RedisListCommands.Direction, org.springframework.data.redis.connection.RedisListCommands.Direction, double)
	 */
	@Override
	public byte[] bLMove(byte[] sourceKey, byte[] destinationKey, Direction from, Direction to, double timeout) {

		Assert.notNull(sourceKey, "Source key must not be null!");
		Assert.notNull(destinationKey, "Destination key must not be null!");
		Assert.notNull(from, "From direction must not be null!");
		Assert.notNull(to, "To direction must not be null!");

		return connection.invoke().just(BinaryJedis::blmove, MultiKeyPipelineBase::blmove, sourceKey, destinationKey,
				ListDirection.valueOf(from.name()), ListDirection.valueOf(to.name()), timeout);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lSet(byte[], long, byte[])
	 */
	@Override
	public void lSet(byte[] key, long index, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		connection.invokeStatus().just(BinaryJedis::lset, MultiKeyPipelineBase::lset, key, index, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lRem(byte[], long, byte[])
	 */
	@Override
	public Long lRem(byte[] key, long count, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return connection.invoke().just(BinaryJedis::lrem, MultiKeyPipelineBase::lrem, key, count, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lPop(byte[])
	 */
	@Override
	public byte[] lPop(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(BinaryJedis::lpop, MultiKeyPipelineBase::lpop, key);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#lPop(byte[], long)
	 */
	@Override
	public List<byte[]> lPop(byte[] key, long count) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(BinaryJedis::lpop, MultiKeyPipelineBase::lpop, key, (int) count);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#rPop(byte[])
	 */
	@Override
	public byte[] rPop(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(BinaryJedis::rpop, MultiKeyPipelineBase::rpop, key);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#rPop(byte[], long)
	 */
	@Override
	public List<byte[]> rPop(byte[] key, long count) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(BinaryJedis::rpop, MultiKeyPipelineBase::rpop, key, (int) count);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#bLPop(int, byte[][])
	 */
	@Override
	public List<byte[]> bLPop(int timeout, byte[]... keys) {

		Assert.notNull(keys, "Key must not be null!");
		Assert.noNullElements(keys, "Keys must not contain null elements!");

		return connection.invoke().just(BinaryJedis::blpop, MultiKeyPipelineBase::blpop, bXPopArgs(timeout, keys));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#bRPop(int, byte[][])
	 */
	@Override
	public List<byte[]> bRPop(int timeout, byte[]... keys) {

		Assert.notNull(keys, "Key must not be null!");
		Assert.noNullElements(keys, "Keys must not contain null elements!");

		return connection.invoke().just(BinaryJedis::brpop, MultiKeyPipelineBase::brpop, bXPopArgs(timeout, keys));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#rPopLPush(byte[], byte[])
	 */
	@Override
	public byte[] rPopLPush(byte[] srcKey, byte[] dstKey) {

		Assert.notNull(srcKey, "Source key must not be null!");
		Assert.notNull(dstKey, "Destination key must not be null!");

		return connection.invoke().just(BinaryJedis::rpoplpush, MultiKeyPipelineBase::rpoplpush, srcKey, dstKey);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisListCommands#bRPopLPush(int, byte[], byte[])
	 */
	@Override
	public byte[] bRPopLPush(int timeout, byte[] srcKey, byte[] dstKey) {

		Assert.notNull(srcKey, "Source key must not be null!");
		Assert.notNull(dstKey, "Destination key must not be null!");

		return connection.invoke().just(BinaryJedis::brpoplpush, MultiKeyPipelineBase::brpoplpush, srcKey, dstKey, timeout);
	}

	private static byte[][] bXPopArgs(int timeout, byte[]... keys) {

		byte[][] args = new byte[keys.length + 1][];
		System.arraycopy(keys, 0, args, 0, keys.length);

		args[args.length - 1] = Protocol.toByteArray(timeout);
		return args;
	}

}
