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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.lang.Nullable;

/**
 * Invocation handler that suppresses close calls on {@link RedisConnection}.
 *
 * @see RedisConnection#close()
 * @author Costin Leau
 * @author Christoph Strobl
 */
class CloseSuppressingInvocationHandler implements InvocationHandler {

	private static final String CLOSE = "close";
	private static final String HASH_CODE = "hashCode";
	private static final String EQUALS = "equals";

	private final Object target;

	public CloseSuppressingInvocationHandler(Object target) {
		this.target = target;
	}

	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		if (method.getName().equals(EQUALS)) {
			// Only consider equal when proxies are identical.
			return (proxy == args[0]);
		} else if (method.getName().equals(HASH_CODE)) {
			// Use hashCode of PersistenceManager proxy.
			return System.identityHashCode(proxy);
		} else if (method.getName().equals(CLOSE)) {
			// Handle close method: suppress, not valid.

			/**
			 * 如果exposeConnection为true则表示对外暴露connection，此时 connToExpose=conntoUse
			 *
			 * 否则将会执行createRedisConnectionProxy 为connTouse 创建代理对象，在创建代理对象的是时候我们制定的代理接口是 RedisConnection
			 * 使用CloseSuppressingInvocationHandler 作为InvocationHandler
			 *
			 * 柑橘这个CloseSuppressingInvocationHandler中并没有什么重要的实现，只是在判断 当前调用的方法是不是 RedisConnection的close方法，如果是
			 * 则不会执行任何操作
			 * if (method.getName().equals(CLOSE)) {
			 * 			// Handle close method: suppress, not valid.
			 * 			return null;
			 * }
			 *
			 */

			return null;
		}

		// Invoke method on target RedisConnection.
		try {
			Object retVal = method.invoke(this.target, args);
			return retVal;
		} catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}
}
