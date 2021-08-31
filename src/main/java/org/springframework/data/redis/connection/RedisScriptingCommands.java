/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.redis.connection;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Scripting commands.
 *
 * @author Costin Leau
 * @author Christoph Strobl
 * @author David Liu
 * @author Mark Paluch
 */
public interface RedisScriptingCommands {

	/**
	 * Flush lua script cache.
	 * 刷新lua脚本缓存。
	 *
	 * Available since 2.6.0.
	 *
	 * Time complexity: O(N) with N being the number of scripts in cache
	 *
	 * Flush the Lua scripts cache.
	 *
	 * Please refer to the EVAL documentation for detailed information about Redis Lua scripting.
	 *
	 * By default, SCRIPT FLUSH will synchronously flush the cache. Starting with Redis 6.2, setting the lazyfree-lazy-user-flush configuration directive to "yes" changes the default flush mode to asynchronous.
	 *
	 * It is possible to use one of the following modifiers to dictate the flushing mode explicitly:
	 *
	 * ASYNC: flushes the cache asynchronously
	 * SYNC: flushes the cache synchronously
	 * Return value
	 * Simple string reply
	 *
	 * History
	 * >= 6.2.0: Added the ASYNC and SYNC flushing mode modifiers, as well as the lazyfree-lazy-user-flush configuration directive.
	 *
	 *
	 *
	 *
	 * 刷新 Lua 脚本缓存。
	 *
	 * 有关 Redis Lua 脚本的详细信息，请参阅EVAL文档。
	 *
	 * 默认情况下，SCRIPT FLUSH将同步刷新缓存。从 Redis 6.2 开始，将lazyfree-lazy-user-flush配置指令设置为“yes”会将默认刷新模式更改为异步。
	 *
	 * 可以使用以下修饰符之一来明确指定刷新模式：
	 *
	 * ASYNC: 异步刷新缓存
	 * SYNC: 同步刷新缓存
	 * 返回值
	 * 简单的字符串回复
	 *
	 * 历史
	 * >= 6.2.0：添加了ASYNC和SYNC刷新模式修饰符，以及 lazyfree-lazy-user-flush配置指令。
	 *
	 * @see <a href="https://redis.io/commands/script-flush">Redis Documentation: SCRIPT FLUSH</a>
	 */
	void scriptFlush();

	/**
	 * Kill current lua script execution.
	 *
	 * @see <a href="https://redis.io/commands/script-kill">Redis Documentation: SCRIPT KILL</a>
	 */
	void scriptKill();

	/**
	 * Load lua script into scripts cache, without executing it.<br>
	 * Execute the script by calling {@link #evalSha(byte[], ReturnType, int, byte[]...)}.
	 *
	 * 加载lua脚本到脚本缓存中，而不执行它。通过调用evalSha(byte[]， ReturnType, int, byte[]…)来执行脚本。
	 * @param script must not be {@literal null}.
	 * @return {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/script-load">Redis Documentation: SCRIPT LOAD</a>
	 */
	@Nullable
	String scriptLoad(byte[] script);

	/**
	 * Check if given {@code scriptShas} exist in script cache.
	 *
	 * @param scriptShas
	 * @return one entry per given scriptSha in returned {@link List} or {@literal null} when used in pipeline /
	 *         transaction.
	 * @see <a href="https://redis.io/commands/script-exists">Redis Documentation: SCRIPT EXISTS</a>
	 */
	@Nullable
	List<Boolean> scriptExists(String... scriptShas);

	/**
	 * Evaluate given {@code script}.
	 *
	 * @param script must not be {@literal null}.
	 * @param returnType must not be {@literal null}.
	 * @param numKeys
	 * @param keysAndArgs must not be {@literal null}.
	 * @return script result. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/eval">Redis Documentation: EVAL</a>
	 */
	@Nullable
	<T> T eval(byte[] script, ReturnType returnType, int numKeys, byte[]... keysAndArgs);

	/**
	 * Evaluate given {@code scriptSha}.
	 *
	 * @param scriptSha must not be {@literal null}.
	 * @param returnType must not be {@literal null}.
	 * @param numKeys
	 * @param keysAndArgs must not be {@literal null}.
	 * @return script result. {@literal null} when used in pipeline / transaction.
	 * @see <a href="https://redis.io/commands/evalsha">Redis Documentation: EVALSHA</a>
	 */
	@Nullable
	<T> T evalSha(String scriptSha, ReturnType returnType, int numKeys, byte[]... keysAndArgs);

	/**
	 * Evaluate given {@code scriptSha}.
	 *
	 * @param scriptSha must not be {@literal null}.
	 * @param returnType must not be {@literal null}.
	 * @param numKeys
	 * @param keysAndArgs must not be {@literal null}.
	 * @return script result. {@literal null} when used in pipeline / transaction.
	 * @since 1.5
	 * @see <a href="https://redis.io/commands/evalsha">Redis Documentation: EVALSHA</a>
	 */
	@Nullable
	<T> T evalSha(byte[] scriptSha, ReturnType returnType, int numKeys, byte[]... keysAndArgs);
}
