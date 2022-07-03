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

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * Callback executing all operations against a surrogate 'session' (basically against the same underlying Redis
 * connection). Allows 'transactions' to take place through the use of multi/discard/exec/watch/unwatch commands.
 *
 * 回调对代理'session'执行所有操作(基本上是针对相同的底层Redis连接)。允许通过使用多个/discard/exec/watch/unwatch命令来发生“事务”。
 * @author Costin Leau
 */
public interface SessionCallback<T> {

	/**
	 *
	 *应该来说 sessionCallback 中的所有操作 RedisOperation都会在同一个connection中执行
	 *但是并不意味着一起执行，因为一个connection可以间隔 发送多条命令。 sessionCallback 回调方法中拿到的是RedisOperation对象
	 * 我们用这个对象 执行多个操作，底层会使用同一个connection发送多条命令
	 *
	 *
	 * 而RedisCallback 接口回调方法中传入的是connection对象，那就意味着你可以使用同一个connection操作执行多条命令
	 *
	 *
	 *
	 *
	 *
	 */






	/**
	 * Executes all the given operations inside the same session.
	 * 在同一个会话中执行所有给定的操作。
	 *
	 * @param operations Redis operations
	 * @return return value
	 */
	@Nullable
	<K, V> T execute(RedisOperations<K, V> operations) throws DataAccessException;
}
