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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.lang.Nullable;

/**
 * Callback interface for Redis 'low level' code. To be used with {@link RedisTemplate} execution methods, often as
 * anonymous classes within a method implementation. Usually, used for chaining several operations together (
 * {@code get/set/trim etc...}.
 *回调接口的Redis '低级'代码。与RedisTemplate执行方法一起使用，通常作为方法实现中的匿名类。通常，用于将多个操作链接在一起(get/set/trim等....)
 * @author Costin Leau
 */
public interface RedisCallback<T> {

	/**
	 * Gets called by {@link RedisTemplate} with an active Redis connection. Does not need to care about activating or
	 * closing the connection or handling exceptions.
	 * 通过活跃的Redis连接被RedisTemplate调用。不需要关心激活或关闭连接或处理异常。
	 *
	 * 问题：这个方法什么时候被调用？在哪里调用
	 *
	 * @param connection active Redis connection
	 * @return a result object or {@code null} if none
	 * @throws DataAccessException
	 */
	@Nullable
	T doInRedis(RedisConnection connection) throws DataAccessException;
}
