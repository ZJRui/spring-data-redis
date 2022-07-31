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
package org.springframework.data.redis.serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * {@link RedisSerializer} that can read and write JSON using
 * <a href="https://github.com/FasterXML/jackson-core">Jackson's</a> and
 * <a href="https://github.com/FasterXML/jackson-databind">Jackson Databind</a> {@link ObjectMapper}.
 * <p>
 * This converter can be used to bind to typed beans, or untyped {@link java.util.HashMap HashMap} instances.
 * <b>Note:</b>Null objects are serialized as empty arrays and vice versa.
 *
 * @author Thomas Darimont
 * @since 1.2
 */
@SuppressWarnings("all")
public class Jackson2JsonRedisSerializer<T> implements RedisSerializer<T> {
	/**
	 * 可以使用Jackson’s和Jackson Databind objectapper读写JSON的RedisSerializer。
	 * 此转换器可用于绑定到类型化bean或非类型化HashMap实例。注意:Null对象被序列化为空数组，反之亦然。
	 *
	 *
	 * JacksonJsonRedisSerializer和GenericJackson2JsonRedisSerializer，两者都能系列化成json，但是后者会在json中加入@class属性，
	 * 类的全路径包名，方便反系列化。前者如果存放了List则在反系列化的时候如果没指定TypeReference则会报错java.util.LinkedHashMap
	 * cannot be cast to 。
	 *
	 * 序列化的要求是 需要实现 Serializable，但是 如果 Student没有实现 Seriializer 接口，也可以使用Jackson2JsonRedisSerializer 进行序列化。
	 * 因为 Jackson2JsonRedisSerializer 这类序列化的实现 是先讲 对象转为 String，然后进行 write。
	 * 问题的关键如何转成String。
	 *
	 *------------------------------
	 * RedisTemplate中序列化方式GenericJackson2JsonRedisSerializer和Jackson2JsonRedisSerializer的区别
	 * Jackson2JsonRedisSerializer和GenericJackson2JsonRedisSerializer都是序列化为json格式。
	 * 不同：
	 *
	 *   如果存储的类型为List等带有泛型的对象，反序列化的时候 Jackson2JsonRedisSerializer序列化方式会报错，而GenericJackson2JsonRedisSerializer序列化方式是成功的，
	 *
	 * 原因：
	 *
	 *    Jackson2JsonRedisSerializer序列化方式数据：
	 *
	 * [
	 *     {
	 *         "userId": null,
	 *         "userName": "你好",
	 *         "password": "22222222222",
	 *         "phone": null
	 *     }
	 * ]
	 *    GenericJackson2JsonRedisSerializer序列化方式数据：
	 *
	 * [
	 *     "java.util.ArrayList",
	 *     [
	 *         {
	 *             "@class": "com.winterchen.model.User",
	 *             "userId": null,
	 *             "userName": "你好",
	 *             "password": "22222222222",
	 *             "phone": null
	 *         }
	 *     ]
	 * ]
	 * 当反序列化的时候 Jackson2JsonRedisSerializer方式的list中放的是LinkedHashMap，而我们是强转为User类型的所以报错
	 *
	 * GenericJackson2JsonRedisSerializer方式中有@class字段保存有类型的包路径，可以顺利的转换为
	 * 我们需要的User类型，比如：{"@class":"com.yupaopao.hug.chatroom.zjr.redis.SerializerTest$Student","name":"a"}
	 * ————————————————
	 * 版权声明：本文为CSDN博主「after_you」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
	 * 原文链接：https://blog.csdn.net/after_you/article/details/81086904
	 *
	 * ------------
	 *
	 *
	 *
	 */

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final JavaType javaType;

	private ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Creates a new {@link Jackson2JsonRedisSerializer} for the given target {@link Class}.
	 *
	 * @param type
	 */
	public Jackson2JsonRedisSerializer(Class<T> type) {
		this.javaType = getJavaType(type);
	}

	/**
	 * Creates a new {@link Jackson2JsonRedisSerializer} for the given target {@link JavaType}.
	 *
	 * @param javaType
	 */
	public Jackson2JsonRedisSerializer(JavaType javaType) {
		this.javaType = javaType;
	}

	@SuppressWarnings("unchecked")
	public T deserialize(@Nullable byte[] bytes) throws SerializationException {

		if (SerializationUtils.isEmpty(bytes)) {
			return null;
		}
		try {
			return (T) this.objectMapper.readValue(bytes, 0, bytes.length, javaType);
		} catch (Exception ex) {
			throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	public byte[] serialize(@Nullable Object t) throws SerializationException {

		if (t == null) {
			return SerializationUtils.EMPTY_ARRAY;
		}
		try {

			return this.objectMapper.writeValueAsBytes(t);
		} catch (Exception ex) {
			throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Sets the {@code ObjectMapper} for this view. If not set, a default {@link ObjectMapper#ObjectMapper() ObjectMapper}
	 * is used.
	 * <p>
	 * Setting a custom-configured {@code ObjectMapper} is one way to take further control of the JSON serialization
	 * process. For example, an extended {@link SerializerFactory} can be configured that provides custom serializers for
	 * specific types. The other option for refining the serialization process is to use Jackson's provided annotations on
	 * the types to be serialized, in which case a custom-configured ObjectMapper is unnecessary.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {

		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * Returns the Jackson {@link JavaType} for the specific class.
	 * <p>
	 * Default implementation returns {@link TypeFactory#constructType(java.lang.reflect.Type)}, but this can be
	 * overridden in subclasses, to allow for custom generic collection handling. For instance:
	 *
	 * <pre class="code">
	 * protected JavaType getJavaType(Class&lt;?&gt; clazz) {
	 * 	if (List.class.isAssignableFrom(clazz)) {
	 * 		return TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, MyBean.class);
	 * 	} else {
	 * 		return super.getJavaType(clazz);
	 * 	}
	 * }
	 * </pre>
	 *
	 * @param clazz the class to return the java type for
	 * @return the java type
	 */
	protected JavaType getJavaType(Class<?> clazz) {
		return TypeFactory.defaultInstance().constructType(clazz);
	}
}
