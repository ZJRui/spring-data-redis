/*
 * Copyright 2021 the original author or authors.
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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.MultiKeyPipelineBase;
import redis.clients.jedis.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.connection.convert.Converters;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility for functional invocation of Jedis methods. Typically used to express the method call as method reference and
 * passing method arguments through one of the {@code just} or {@code from} methods.
 * <p>
 * {@code just} methods record the method call and evaluate the method result immediately. {@code from} methods allows
 * composing a functional pipeline to transform the result using a {@link Converter}.
 * <p>
 * Usage example:
 *
 * <pre class="code">
 * JedisInvoker invoker = …;
 *
 * Long result = invoker.just(BinaryJedisCommands::geoadd, RedisPipeline::geoadd, key, point.getX(), point.getY(), member);
 *
 * List&lt;byte[]&gt; result = invoker.from(BinaryJedisCommands::geohash, RedisPipeline::geohash, key, members)
 * 				.get(JedisConverters.bytesListToStringListConverter());
 * </pre>
 * <p>
 * The actual translation from {@link Response} is delegated to {@link Synchronizer} which can either await completion
 * or record the response along {@link Converter} for further processing.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.5
 */
@SuppressWarnings("all")
class JedisInvoker {
	/**
	 * Jedis方法的函数调用实用程序。通常用于将方法调用表示为方法引用，
	 * 并通过一个just或from方法传递方法参数。
	 * 只有方法记录方法调用并立即计算方法结果。from方法允许组合一个函数管道来使用Converter转换结果。
	 * 使用的例子:
	 *
	 *
	 */



	/**
	 * 每一个JedisInvoker 对象都有一个 Synchronizer
	 *
	 * JedisInvoker  作为JedisConnection的属性 被创建， 在创建JedisInvoker的时候
	 * 提供了 一个Synchronizer的实现类对象。
	 *
	 * Synchronizer对象的doInvoker  会去执行 jedisConnection的doInvoke 方法,  并且Synchronizer对象的doInvoker方法会透传到JedisConnection的doInvoke
	 *
	 * JedisConnection的 JedisInvoker 属性，下面的doInvoke方法 本质就是调用当前对象（JedisConnection）的doInvoke
	 * 	private final JedisInvoker invoker = new JedisInvoker((directFunction, pipelineFunction, converter,
	 *   nullDefault) -> doInvoke(false, directFunction, pipelineFunction, converter, nullDefault));
	 *
	 *
	 * 那么问题就是JedisConnection 通过 JedisInvoker  会最终执行 Synchronizer的 doInvoke，而Synchronizer的doInvoke
	 * 最终又会执行 JedisConnection的doInvoke。
	 *
	 * 那么这个方法的参数是怎么来的呢？方法是如何被触发的呢？
	 *
	 * 答案就是 JedisConnection 通过 其invoke方法 返回 JedisInvoker, JedisInvoker 对象内部 存在各种 just方法。
	 *
	 *
	 * just方法的作用就是 接收一个函数，将这个函数进行包装 之后交给 synchronizer执行
	 *
	 *
	 */
	private final Synchronizer synchronizer;

	JedisInvoker(Synchronizer synchronizer) {
		this.synchronizer = synchronizer;
	}

	/**
	 * Invoke the {@link ConnectionFunction0} and return its result.
	 *
	 * @param function must not be {@literal null}.
	 */
	@Nullable
	<R> R just(ConnectionFunction0<R> function) {

		Assert.notNull(function, "ConnectionFunction must not be null!");

		return synchronizer.invoke(function::apply, it -> {
			throw new UnsupportedOperationException("Operation not supported in pipelining/transaction mode");
		}, Converters.identityConverter(), () -> null);
	}

	/**
	 * Invoke the {@link ConnectionFunction0} and return its result.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 */
	@Nullable
	<R> R just(ConnectionFunction0<R> function, PipelineFunction0<R> pipelineFunction) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		/**
		 * 这个地方注意
		 * function 是一个函数ConnectionFunction0<R>,执行这个函数的apply 就可以得到一个R。
		 *
		 * 但是下面的 function::apply 不是执行 ConnectionFunction0的 apply函数的意思。
		 * 而是再次进行了封装 。
		 *
		 * 而且值得注意的是  ConnectionFunction0 中定义的apply函数 是一个输入Jedis ，返回R的函数。
		 * R apply(Jedis connection);
		 *
		 * synchronizer的ivoke方法的第一个参数  也是一个输入Jedis的函数。 但是我们并不是讲 function直接传递，
		 * 而是传递了 function::apply  。
		 * 原因是 function是一个 ConnectionFunction0 对象， 而invoke接受的是一个Function 对象。
		 *
		 */
		return synchronizer.invoke(function::apply, pipelineFunction::apply, Converters.identityConverter(), () -> null);
	}

	/**
	 * Invoke the {@link ConnectionFunction1} and return its result.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 */
	@Nullable
	<R, T1> R just(ConnectionFunction1<T1, R> function, PipelineFunction1<T1, R> pipelineFunction, T1 t1) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return synchronizer.invoke(it -> function.apply(it, t1), it -> pipelineFunction.apply(it, t1));
	}

	/**
	 * Invoke the {@link ConnectionFunction2} and return its result.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 */
	@Nullable
	<R, T1, T2> R just(ConnectionFunction2<T1, T2, R> function, PipelineFunction2<T1, T2, R> pipelineFunction, T1 t1,
			T2 t2) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return synchronizer.invoke(it -> function.apply(it, t1, t2), it -> pipelineFunction.apply(it, t1, t2));
	}

	/**
	 * Invoke the {@link ConnectionFunction3} and return its result.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 */
	@Nullable
	<R, T1, T2, T3> R just(ConnectionFunction3<T1, T2, T3, R> function, PipelineFunction3<T1, T2, T3, R> pipelineFunction,
			T1 t1, T2 t2, T3 t3) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return synchronizer.invoke(it -> function.apply(it, t1, t2, t3), it -> pipelineFunction.apply(it, t1, t2, t3));
	}

	/**
	 * Invoke the {@link ConnectionFunction4} and return its result.
	 * 调用 ConnectionFunction4。并返回其结果。
	 *
	 *这个 just 是 针对接收4个参数 ，返回一个值的 redis 命令， 比如
	 * lpos: > LPOS mylist c RANK -1 COUNT 2   返回[7,6]，含义：其中rank和count是lpos的固定语法，参数是mylist，c ，-1，2 这四个.
	 * We can combine COUNT and RANK, so that COUNT will try to return up to the specified number of matches, but starting from the Nth match, as specified by the RANK option.
	 *
	 *
	 * linsert: LINSERT key BEFORE|AFTER pivot value 列如：linsert mylist before "world" "There"
	 *
	 *
	 * lMove:
	 *
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 */
	@Nullable
	<R, T1, T2, T3, T4> R just(ConnectionFunction4<T1, T2, T3, T4, R> function,
			PipelineFunction4<T1, T2, T3, T4, R> pipelineFunction, T1 t1, T2 t2, T3 t3, T4 t4) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		/**
		 *1.
		 * just函数的作用 是什么？ 比如当前的这个just 会在 JedisListCommand的
		 * public Long lInsert(byte[] key, Position where, byte[] pivot, byte[] value) 方法中 通过
		 *  connection.invoke().just(BinaryJedis::linsert, MultiKeyPipelineBase::linsert, key,
		 * 				JedisConverters.toListPosition(where), pivot, value);
		 *
		 * 方式被调用到，  这里我们可以看到  just方法的function参数实际上 对应着 BinaryJedis::linsert， BinaryJedis 是底层Jedis包的实现。
		 * 他就对应着 一条redis命令：
		 *  linsert(final byte[] key, final ListPosition where, final byte[] pivot,
		 *       final byte[] value)
		 *
		 *
		 *  linsert 命令有四个参数， 因此我们设计一个 输入4个参数返回一个Value的 函数接口ConnectionFunction4<T1, T2, T3, T4, R>
		 *
		 *  这个函数接口的4个参数 在just方法中 通过入参标记， 因此我们需要将  T1  t2 t3 t4 交给ConnectionFunction4。
		 *
		 *  交付的方式 就是  function.apply(it, t1, t2, t3, t4), 那么问题是 function的apply为什么会有5个参数 呢,it是什么呢？ 这是因为
		 *
		 *  函数是接口ConnectionFunction4  在设计的时候就约定了 他的第一个参数是Jedis， 另外有4个不固定的参数
		 *  R apply(Jedis connection, T1 t1, T2 t2, T3 t3, T4 t4);
		 *
		 *
		 *  下面的代码中   it -> function.apply(it, t1, t2, t3, t4)  是定义了一个 输入it返回一个值的函数，  这个函数的内部实现是去执行 just的入参function
		 *  也就是相当于对函数再做一次封装， 为什么要封装呢？因为 synchronizer的invoke方法第一个参数就是接收 T输出R的 函数。
		 *
		 *
		 *  那么问题是 just方法中提供了 t1 t2 t3 t4 参数，因此 t1 -t4 是确定的。 但是
		 *
		 *  it -> function.apply(it, t1, t2, t3, t4) 定义的函数 it对象是哪个对象是不确定的，这里只是定义了一个 输入it 返回某一个值的函数，
		 *  it对象实际是哪个对象需要等到 这个函数真正执行的时候才确定。 那么 it -> function.apply(it, t1, t2, t3, t4) 这个函数什么时候执行呢？
		 *  显然 是在 synchronizer的invoke方法 中， 但是实际 synchronizer的invoke方法又将函数it -> function.apply(it, t1, t2, t3, t4)
		 *  透传到 JedisConnection的doinvoke方法中，也就是JedisConnection的doInvoke方法中的directFunction  参数，
		 *  directFunction函数的执行是
		 *  Object result = directFunction.apply(getJedis());
		 *  getJedis()会获取JedisConnection中的jedis属性。
		 *
		 *
		 * 2. invoke 的第一个参数 是一个 Function<Jedis, I>， 也就是这个函数到时候执行的时候 我会给他一个Jedis作为入参，他返回给我一个I
		 *
		 * 注意 it->function.apply(it,t1-t4)  入参it 也就是jedis 被 应用到 function 的apply函数中， function是ConnectionFunction4，也就是说
		 * ConnectionFunction4的 apply 接收的第一个参数就是Jedis
		 *
		 * it->function.apply(it,t1,t2,t3,t4)被封装成 synchronizer的invoke方法的 Function<Jedis, I> callFunction参数，
		 * 这个callFunction 被透传到JedisConnection的doInvoke方法中directFunction， it->function.apply被看做是一个 需要提供Jedis的函数
		 * 在JedisConnection的doInvoke 中，directFunction的执行 就会提供Jedis
		 *  Object result = directFunction.apply(getJedis());
		 *
		 */
		return synchronizer.invoke(it -> function.apply(it, t1, t2, t3, t4),
				it -> pipelineFunction.apply(it, t1, t2, t3, t4));
	}

	/**
	 * Invoke the {@link ConnectionFunction5} and return its result.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 * @param t5 fifth argument.
	 */
	@Nullable
	<R, T1, T2, T3, T4, T5> R just(ConnectionFunction5<T1, T2, T3, T4, T5, R> function,
			PipelineFunction5<T1, T2, T3, T4, T5, R> pipelineFunction, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return synchronizer.invoke(it -> function.apply(it, t1, t2, t3, t4, t5),
				it -> pipelineFunction.apply(it, t1, t2, t3, t4, t5));
	}

	/**
	 * Invoke the {@link ConnectionFunction5} and return its result.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 * @param t5 fifth argument.
	 * @param t6 sixth argument.
	 */
	@Nullable
	<R, T1, T2, T3, T4, T5, T6> R just(ConnectionFunction6<T1, T2, T3, T4, T5, T6, R> function,
			PipelineFunction6<T1, T2, T3, T4, T5, T6, R> pipelineFunction, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return synchronizer.invoke(it -> function.apply(it, t1, t2, t3, t4, t5, t6),
				it -> pipelineFunction.apply(it, t1, t2, t3, t4, t5, t6));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction0} and return a {@link SingleInvocationSpec} for
	 * further composition.
	 *
	 * @param function must not be {@literal null}.
	 */
	<R> SingleInvocationSpec<R> from(ConnectionFunction0<R> function) {

		Assert.notNull(function, "ConnectionFunction must not be null!");

		return from(function, connection -> {
			throw new UnsupportedOperationException("Operation not supported in pipelining/transaction mode");
		});
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction0} and return a {@link SingleInvocationSpec} for
	 * further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 */
	<R> SingleInvocationSpec<R> from(ConnectionFunction0<R> function, PipelineFunction0<R> pipelineFunction) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return new DefaultSingleInvocationSpec<>(function::apply, pipelineFunction::apply, synchronizer);
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction1} and return a {@link SingleInvocationSpec} for
	 * further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 */
	<R, T1> SingleInvocationSpec<R> from(ConnectionFunction1<T1, R> function, PipelineFunction1<T1, R> pipelineFunction,
			T1 t1) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return from(it -> function.apply(it, t1), it -> pipelineFunction.apply(it, t1));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction2} and return a {@link SingleInvocationSpec} for
	 * further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 */
	<R, T1, T2> SingleInvocationSpec<R> from(ConnectionFunction2<T1, T2, R> function,
			PipelineFunction2<T1, T2, R> pipelineFunction, T1 t1, T2 t2) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return from(it -> function.apply(it, t1, t2), it -> pipelineFunction.apply(it, t1, t2));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction3} and return a {@link SingleInvocationSpec} for
	 * further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 */
	<R, T1, T2, T3> SingleInvocationSpec<R> from(ConnectionFunction3<T1, T2, T3, R> function,
			PipelineFunction3<T1, T2, T3, R> pipelineFunction, T1 t1, T2 t2, T3 t3) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return from(it -> function.apply(it, t1, t2, t3), it -> pipelineFunction.apply(it, t1, t2, t3));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction4} and return a {@link SingleInvocationSpec} for
	 * further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 */
	<R, T1, T2, T3, T4> SingleInvocationSpec<R> from(ConnectionFunction4<T1, T2, T3, T4, R> function,
			PipelineFunction4<T1, T2, T3, T4, R> pipelineFunction, T1 t1, T2 t2, T3 t3, T4 t4) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return from(it -> function.apply(it, t1, t2, t3, t4), it -> pipelineFunction.apply(it, t1, t2, t3, t4));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction5} and return a {@link SingleInvocationSpec} for
	 * further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 * @param t5 fifth argument.
	 */
	<R, T1, T2, T3, T4, T5> SingleInvocationSpec<R> from(ConnectionFunction5<T1, T2, T3, T4, T5, R> function,
			PipelineFunction5<T1, T2, T3, T4, T5, R> pipelineFunction, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return from(it -> function.apply(it, t1, t2, t3, t4, t5), it -> pipelineFunction.apply(it, t1, t2, t3, t4, t5));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction6} and return a {@link SingleInvocationSpec} for
	 * further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 * @param t5 fifth argument.
	 * @param t6 sixth argument.
	 */
	<R, T1, T2, T3, T4, T5, T6> SingleInvocationSpec<R> from(ConnectionFunction6<T1, T2, T3, T4, T5, T6, R> function,
			PipelineFunction6<T1, T2, T3, T4, T5, T6, R> pipelineFunction, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return from(it -> function.apply(it, t1, t2, t3, t4, t5, t6),
				it -> pipelineFunction.apply(it, t1, t2, t3, t4, t5, t6));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction0} that returns a {@link Collection}-like result
	 * and return a {@link ManyInvocationSpec} for further composition.
	 *
	 * @param function must not be {@literal null}.
	 */
	<R extends Collection<E>, E> ManyInvocationSpec<E> fromMany(ConnectionFunction0<R> function) {

		Assert.notNull(function, "ConnectionFunction must not be null!");

		return fromMany(function, connection -> {
			throw new UnsupportedOperationException("Operation not supported in pipelining/transaction mode");
		});
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction0} that returns a {@link Collection}-like result
	 * and return a {@link ManyInvocationSpec} for further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 */
	<R extends Collection<E>, E> ManyInvocationSpec<E> fromMany(ConnectionFunction0<R> function,
			PipelineFunction0<R> pipelineFunction) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return new DefaultManyInvocationSpec<>((Function<Jedis, R>) function::apply, pipelineFunction::apply, synchronizer);
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction1} that returns a {@link Collection}-like result
	 * and return a {@link ManyInvocationSpec} for further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 */
	<R extends Collection<E>, E, T1> ManyInvocationSpec<E> fromMany(ConnectionFunction1<T1, R> function,
			PipelineFunction1<T1, R> pipelineFunction, T1 t1) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return fromMany(it -> function.apply(it, t1), it -> pipelineFunction.apply(it, t1));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction2} that returns a {@link Collection}-like result
	 * and return a {@link ManyInvocationSpec} for further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 */
	<R extends Collection<E>, E, T1, T2> ManyInvocationSpec<E> fromMany(ConnectionFunction2<T1, T2, R> function,
			PipelineFunction2<T1, T2, R> pipelineFunction, T1 t1, T2 t2) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return fromMany(it -> function.apply(it, t1, t2), it -> pipelineFunction.apply(it, t1, t2));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction3} that returns a {@link Collection}-like result
	 * and return a {@link ManyInvocationSpec} for further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 */
	<R extends Collection<E>, E, T1, T2, T3> ManyInvocationSpec<E> fromMany(ConnectionFunction3<T1, T2, T3, R> function,
			PipelineFunction3<T1, T2, T3, R> pipelineFunction, T1 t1, T2 t2, T3 t3) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return fromMany(it -> function.apply(it, t1, t2, t3), it -> pipelineFunction.apply(it, t1, t2, t3));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction4} that returns a {@link Collection}-like result
	 * and return a {@link ManyInvocationSpec} for further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 */
	<R extends Collection<E>, E, T1, T2, T3, T4> ManyInvocationSpec<E> fromMany(
			ConnectionFunction4<T1, T2, T3, T4, R> function, PipelineFunction4<T1, T2, T3, T4, R> pipelineFunction, T1 t1,
			T2 t2, T3 t3, T4 t4) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return fromMany(it -> function.apply(it, t1, t2, t3, t4), it -> pipelineFunction.apply(it, t1, t2, t3, t4));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction5} that returns a {@link Collection}-like result
	 * and return a {@link ManyInvocationSpec} for further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 * @param t5 fifth argument.
	 */
	<R extends Collection<E>, E, T1, T2, T3, T4, T5> ManyInvocationSpec<E> fromMany(
			ConnectionFunction5<T1, T2, T3, T4, T5, R> function, PipelineFunction5<T1, T2, T3, T4, T5, R> pipelineFunction,
			T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return fromMany(it -> function.apply(it, t1, t2, t3, t4, t5), it -> pipelineFunction.apply(it, t1, t2, t3, t4, t5));
	}

	/**
	 * Compose a invocation pipeline from the {@link ConnectionFunction6} that returns a {@link Collection}-like result
	 * and return a {@link ManyInvocationSpec} for further composition.
	 *
	 * @param function must not be {@literal null}.
	 * @param pipelineFunction must not be {@literal null}.
	 * @param t1 first argument.
	 * @param t2 second argument.
	 * @param t3 third argument.
	 * @param t4 fourth argument.
	 * @param t5 fifth argument.
	 * @param t6 sixth argument.
	 */
	<R extends Collection<E>, E, T1, T2, T3, T4, T5, T6> ManyInvocationSpec<E> fromMany(
			ConnectionFunction6<T1, T2, T3, T4, T5, T6, R> function,
			PipelineFunction6<T1, T2, T3, T4, T5, T6, R> pipelineFunction, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {

		Assert.notNull(function, "ConnectionFunction must not be null!");
		Assert.notNull(pipelineFunction, "PipelineFunction must not be null!");

		return fromMany(it -> function.apply(it, t1, t2, t3, t4, t5, t6),
				it -> pipelineFunction.apply(it, t1, t2, t3, t4, t5, t6));
	}

	/**
	 * Represents an element in the invocation pipleline allowing consuming the result by applying a {@link Converter}.
	 *
	 * @param <S>
	 */
	interface SingleInvocationSpec<S> {

		/**
		 * Materialize the pipeline by invoking the {@code ConnectionFunction} and returning the result after applying
		 * {@link Converter}.
		 *
		 * @param converter must not be {@literal null}.
		 * @param <T> target type.
		 * @return the converted result, can be {@literal null}.
		 */
		@Nullable
		<T> T get(Converter<S, T> converter);

		/**
		 * Materialize the pipeline by invoking the {@code ConnectionFunction} and returning the result after applying
		 * {@link Converter} or return the {@literal nullDefault} value if not present.
		 *
		 * @param converter must not be {@literal null}.
		 * @param nullDefault can be {@literal null}.
		 * @param <T> target type.
		 * @return the converted result, can be {@literal null}.
		 */
		@Nullable
		default <T> T orElse(Converter<S, T> converter, @Nullable T nullDefault) {
			return getOrElse(converter, () -> nullDefault);
		}

		/**
		 * Materialize the pipeline by invoking the {@code ConnectionFunction} and returning the result after applying
		 * {@link Converter} or return the {@literal nullDefault} value if not present.
		 *
		 * @param converter must not be {@literal null}.
		 * @param nullDefault must not be {@literal null}.
		 * @param <T> target type.
		 * @return the converted result, can be {@literal null}.
		 */
		@Nullable
		<T> T getOrElse(Converter<S, T> converter, Supplier<T> nullDefault);
	}

	/**
	 * Represents an element in the invocation pipleline for methods returning {@link Collection}-like results allowing
	 * consuming the result by applying a {@link Converter}.
	 *
	 * @param <S>
	 */
	interface ManyInvocationSpec<S> {

		/**
		 * Materialize the pipeline by invoking the {@code ConnectionFunction} and returning the result.
		 *
		 * @return the result as {@link List}.
		 */
		default List<S> toList() {
			return toList(Converters.identityConverter());
		}

		/**
		 * Materialize the pipeline by invoking the {@code ConnectionFunction} and returning the result after applying
		 * {@link Converter}.
		 *
		 * @param converter must not be {@literal null}.
		 * @param <T> target type.
		 * @return the converted {@link List}.
		 */
		<T> List<T> toList(Converter<S, T> converter);

		/**
		 * Materialize the pipeline by invoking the {@code ConnectionFunction} and returning the result.
		 *
		 * @return the result as {@link Set}.
		 */
		default Set<S> toSet() {
			return toSet(Converters.identityConverter());
		}

		/**
		 * Materialize the pipeline by invoking the {@code ConnectionFunction} and returning the result after applying
		 * {@link Converter}.
		 *
		 * @param converter must not be {@literal null}.
		 * @param <T> target type.
		 * @return the converted {@link Set}.
		 */
		<T> Set<T> toSet(Converter<S, T> converter);
	}

	/**
	 * A function accepting {@link Jedis} with 0 arguments.
	 *
	 * @param <R>
	 */
	@FunctionalInterface
	interface ConnectionFunction0<R> {

		/**
		 * Apply this function to the arguments and return a response.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 */
		R apply(Jedis connection);
	}

	/**
	 * A function accepting {@link Jedis} with 1 argument.
	 *
	 * @param <T1>
	 * @param <R>
	 */
	@FunctionalInterface
	interface ConnectionFunction1<T1, R> {

		/**
		 * Apply this function to the arguments and return a response.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 */
		R apply(Jedis connection, T1 t1);
	}

	/**
	 * A function accepting {@link Jedis} with 2 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <R>
	 */
	@FunctionalInterface
	interface ConnectionFunction2<T1, T2, R> {

		/**
		 * Apply this function to the arguments and return a response.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 */
		R apply(Jedis connection, T1 t1, T2 t2);
	}

	/**
	 * A function accepting {@link Jedis} with 3 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <T3>
	 * @param <R>
	 */
	@FunctionalInterface
	interface ConnectionFunction3<T1, T2, T3, R> {

		/**
		 * Apply this function to the arguments and return a response.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 * @param t3 third argument.
		 */
		R apply(Jedis connection, T1 t1, T2 t2, T3 t3);
	}

	/**
	 * A function accepting {@link Jedis} with 4 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <T3>
	 * @param <T4>
	 * @param <R>
	 */
	@FunctionalInterface
	interface ConnectionFunction4<T1, T2, T3, T4, R> {

		/**
		 * Apply this function to the arguments and return a response.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 * @param t3 third argument.
		 * @param t4 fourth argument.
		 */
		R apply(Jedis connection, T1 t1, T2 t2, T3 t3, T4 t4);
	}

	/**
	 * A function accepting {@link Jedis} with 5 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <T3>
	 * @param <T4>
	 * @param <T5>
	 * @param <R>
	 */
	@FunctionalInterface
	interface ConnectionFunction5<T1, T2, T3, T4, T5, R> {

		/**
		 * Apply this function to the arguments and return a response.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 * @param t3 third argument.
		 * @param t4 fourth argument.
		 * @param t5 fifth argument.
		 */
		R apply(Jedis connection, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
	}

	/**
	 * A function accepting {@link Jedis} with 6 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <T3>
	 * @param <T4>
	 * @param <T5>
	 * @param <T6>
	 * @param <R>
	 */
	@FunctionalInterface
	interface ConnectionFunction6<T1, T2, T3, T4, T5, T6, R> {

		/**
		 * Apply this function to the arguments and return a response.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 * @param t3 third argument.
		 * @param t4 fourth argument.
		 * @param t5 fifth argument.
		 * @param t6 sixth argument.
		 */
		R apply(Jedis connection, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
	}

	/**
	 * A function accepting {@link MultiKeyPipelineBase} with 0 arguments.
	 *
	 * @param <R>
	 */
	@FunctionalInterface
	interface PipelineFunction0<R> {

		/**
		 * Apply this function to the arguments and return a {@link Response}.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 */
		Response<R> apply(MultiKeyPipelineBase connection);
	}

	/**
	 * A function accepting {@link MultiKeyPipelineBase} with 1 argument.
	 *
	 * @param <T1>
	 * @param <R>
	 */
	@FunctionalInterface
	interface PipelineFunction1<T1, R> {

		/**
		 * Apply this function to the arguments and return a {@link Response}.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 */
		Response<R> apply(MultiKeyPipelineBase connection, T1 t1);
	}

	/**
	 * A function accepting {@link MultiKeyPipelineBase} with 2 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <R>
	 */
	@FunctionalInterface
	interface PipelineFunction2<T1, T2, R> {

		/**
		 * Apply this function to the arguments and return a {@link Response}.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 */
		Response<R> apply(MultiKeyPipelineBase connection, T1 t1, T2 t2);
	}

	/**
	 * A function accepting {@link MultiKeyPipelineBase} with 3 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <T3>
	 * @param <R>
	 */
	@FunctionalInterface
	interface PipelineFunction3<T1, T2, T3, R> {

		/**
		 * Apply this function to the arguments and return a {@link Response}.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 * @param t3 third argument.
		 */
		Response<R> apply(MultiKeyPipelineBase connection, T1 t1, T2 t2, T3 t3);
	}

	/**
	 * A function accepting {@link MultiKeyPipelineBase} with 4 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <T3>
	 * @param <T4>
	 * @param <R>
	 */
	@FunctionalInterface
	interface PipelineFunction4<T1, T2, T3, T4, R> {

		/**
		 * Apply this function to the arguments and return a {@link Response}.
		 *
		 * 将此函数应用于参数并返回一个Response。
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 * @param t3 third argument.
		 * @param t4 fourth argument.
		 */
		Response<R> apply(MultiKeyPipelineBase connection, T1 t1, T2 t2, T3 t3, T4 t4);
	}

	/**
	 * A function accepting {@link MultiKeyPipelineBase} with 5 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <T3>
	 * @param <T4>
	 * @param <T5>
	 * @param <R>
	 */
	@FunctionalInterface
	interface PipelineFunction5<T1, T2, T3, T4, T5, R> {

		/**
		 * Apply this function to the arguments and return a {@link Response}.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 * @param t3 third argument.
		 * @param t4 fourth argument.
		 * @param t5 fifth argument.
		 */
		Response<R> apply(MultiKeyPipelineBase connection, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
	}

	/**
	 * A function accepting {@link MultiKeyPipelineBase} with 6 arguments.
	 *
	 * @param <T1>
	 * @param <T2>
	 * @param <T3>
	 * @param <T4>
	 * @param <T5>
	 * @param <T6>
	 * @param <R>
	 */
	@FunctionalInterface
	interface PipelineFunction6<T1, T2, T3, T4, T5, T6, R> {

		/**
		 * Apply this function to the arguments and return a {@link Response}.
		 *
		 * @param connection the connection in use. Never {@literal null}.
		 * @param t1 first argument.
		 * @param t2 second argument.
		 * @param t3 third argument.
		 * @param t4 fourth argument.
		 * @param t5 fifth argument.
		 * @param t6 sixth argument.
		 */
		Response<R> apply(MultiKeyPipelineBase connection, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
	}

	static class DefaultSingleInvocationSpec<S> implements SingleInvocationSpec<S> {

		private final Function<Jedis, S> parentFunction;
		private final Function<MultiKeyPipelineBase, Response<S>> parentPipelineFunction;
		private final Synchronizer synchronizer;

		DefaultSingleInvocationSpec(Function<Jedis, S> parentFunction,
				Function<MultiKeyPipelineBase, Response<S>> parentPipelineFunction, Synchronizer synchronizer) {

			this.parentFunction = parentFunction;
			this.parentPipelineFunction = parentPipelineFunction;
			this.synchronizer = synchronizer;
		}

		@Override
		public <T> T get(Converter<S, T> converter) {

			Assert.notNull(converter, "Converter must not be null");

			return synchronizer.invoke(parentFunction, parentPipelineFunction, converter, () -> null);
		}

		@Nullable
		@Override
		public <T> T getOrElse(Converter<S, T> converter, Supplier<T> nullDefault) {

			Assert.notNull(converter, "Converter must not be null!");

			return synchronizer.invoke(parentFunction, parentPipelineFunction, converter, nullDefault);
		}
	}

	static class DefaultManyInvocationSpec<S> implements ManyInvocationSpec<S> {

		private final Function<Jedis, Collection<S>> parentFunction;
		private final Function<MultiKeyPipelineBase, Response<Collection<S>>> parentPipelineFunction;
		private final Synchronizer synchronizer;

		DefaultManyInvocationSpec(Function<Jedis, ? extends Collection<S>> parentFunction,
				Function<MultiKeyPipelineBase, Response<? extends Collection<S>>> parentPipelineFunction,
				Synchronizer synchronizer) {

			this.parentFunction = (Function) parentFunction;
			this.parentPipelineFunction = (Function) parentPipelineFunction;
			this.synchronizer = synchronizer;
		}

		@Override
		public <T> List<T> toList(Converter<S, T> converter) {

			Assert.notNull(converter, "Converter must not be null!");

			return synchronizer.invoke(parentFunction, parentPipelineFunction, source -> {

				if (source.isEmpty()) {
					return Collections.emptyList();
				}

				List<T> result = new ArrayList<>(source.size());

				for (S s : source) {
					result.add(converter.convert(s));
				}

				return result;
			}, Collections::emptyList);
		}

		@Override
		public <T> Set<T> toSet(Converter<S, T> converter) {

			Assert.notNull(converter, "Converter must not be null!");

			return synchronizer.invoke(parentFunction, parentPipelineFunction, source -> {

				if (source.isEmpty()) {
					return Collections.emptySet();
				}

				Set<T> result = new LinkedHashSet<>(source.size());

				for (S s : source) {
					result.add(converter.convert(s));
				}

				return result;
			}, Collections::emptySet);
		}
	}

	/**
	 * Interface to define a synchronization function to evaluate the actual call.
	 * 接口来定义一个同步函数来计算实际调用。
	 */
	@FunctionalInterface
	interface Synchronizer {//同步器

		@Nullable
		@SuppressWarnings({ "unchecked", "rawtypes" })
		default <I, T> T invoke(Function<Jedis, I> callFunction,
				Function<MultiKeyPipelineBase, Response<I>> pipelineFunction) {
			return (T) doInvoke((Function) callFunction, (Function) pipelineFunction, Converters.identityConverter(), () -> null);
		}

		@Nullable
		@SuppressWarnings({ "unchecked", "rawtypes" })
		default <I, T> T invoke(Function<Jedis, I> callFunction,
				Function<MultiKeyPipelineBase, Response<I>> pipelineFunction, Converter<I, T> converter,
				Supplier<T> nullDefault) {

			return (T) doInvoke((Function) callFunction, (Function) pipelineFunction, (Converter<Object, Object>) converter,
					(Supplier<Object>) nullDefault);
		}

		@Nullable
		Object doInvoke(Function<Jedis, Object> callFunction,
				Function<MultiKeyPipelineBase, Response<Object>> pipelineFunction, Converter<Object, Object> converter,
				Supplier<Object> nullDefault);
	}
}
