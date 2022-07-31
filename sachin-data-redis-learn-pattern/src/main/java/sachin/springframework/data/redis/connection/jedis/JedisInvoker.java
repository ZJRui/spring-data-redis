package sachin.springframework.data.redis.connection.jedis;

import org.springframework.core.convert.converter.Converter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.MultiKeyPipelineBase;
import redis.clients.jedis.Response;
import sachin.springframework.data.redis.connection.convert.Converters;

import java.util.function.Function;
import java.util.function.Supplier;

public class JedisInvoker {


    /**
     * 每一个JedisInvoker 对象都有一个 Synchronizer
     *
     * JedisInvoker  作为JedisConnection的属性 被创建， 在创建JedisInvoker的时候
     * 提供了 一个Synchronizer的实现类对象。
     *
     * Synchronizer对象的doInvoker  会去执行 jedisConnection的doInvoke 方法
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
    private final  Synchronizer synchronizer;

    public JedisInvoker(Synchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    /**
     *
     * @param function
     * @param pipelineFunction
     * @param t1
     * @param t2
     * @param t3
     * @param t4
     * @return
     * @param <R>
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     */

     <R,T1,T2,T3,T4> R just(ConnectionFunction4<T1,T2,T3,T4,R>function,
                          PipelineFunction4<T1,T2,T3,T4,R> pipelineFunction,
                            T1 t1,T2 t2, T3 t3,T4 t4){

         return synchronizer.invoke(it -> function.apply(it, t1, t2, t3, t4)
                 , it -> pipelineFunction.apply(it, t1, t2, t3, t4));

     }

    @FunctionalInterface
    interface ConnectionFunction4<T1,T2,T3,T4,R>{

        /**
         *
         *
         * apply this function to the arguments and return a  response.
         *
         * @param connection
         * @param t1
         * @param t2
         * @param t3
         * @param t4
         * @return
         */
        R apply(Jedis  connection, T1 t1, T2 t2, T3 t3,T4 t4);

    }
    @FunctionalInterface
    interface PipelineFunction4<T1,T2,T3,T4,R>{

        /**
         *
         * @param connection
         * @param t1
         * @param t2
         * @param t3
         * @param t4
         * @return
         */
        Response<R> apply(MultiKeyPipelineBase connection,T1 t1, T2 t2, T3 t3,T4 t4);

    }


    @FunctionalInterface
    interface Synchronizer{//同步器

        /**
         * 因为 是FunctionalInterface  所以接口内只能有一个没有实现的方法。
         * 方法的第一个参数 callFunction： 是一个函数，这个函数接收 一个jedis，返回一个object
         * 方法的内部逻辑可以是使用这个jedis操作 一些命令，最终返回一个操作结果。
         *
         * 方法的第二个参数 pipelineFunction 是一个函数，这个函数接收一个MultiKeyPipelineBase 对象，返回一个Response
         *
         * 上图为Transaction和Pipeline两个类的类结构，可以看到Pipeline和Transaction都继承MultikeyPipelineBase，
         * 其中，MultiKeyPipelineBase和PipelineBase的区别在于处理的命令不同，内部均调用Client发送命令，Pipeline
         * 有一个内部类对象MultiResponseBuilder,当Pipeline开启事务后，其用于存储所有返回结果。
         * Queable用一个LinkedList装入每个命令的返回结果，Response<T>是一个泛型，set(Object data)
         * 方法传入格式化之前的结果，get()方法返回格式化之后的结果。
         *
         *
         * @param callFunction
         * @return
         */

        Object doInvoke(Function<Jedis,Object> callFunction,
                         Function<MultiKeyPipelineBase, Response<Object>> pipelineFunction,
                         Converter<Object,Object> converter,
                         Supplier<Object> nullDefault
        );


        /**
         * 上面的参数太多了，我们来定义 额外 的两个 函数
         *
         * 下面这个函数约定了 第一函数的入参是Jedis， I表示 callFunction内部的业务逻辑执行之后的返回值。
         */
        default <I, T> T invoke(Function<Jedis, I> callFunction,
                                Function<MultiKeyPipelineBase, Response<I>> pipelineBaseResponseFunction) {

            return (T) doInvoke((Function)callFunction,(Function)pipelineBaseResponseFunction,
                    Converters.identityConverter(),()->null);
        }

        default <I,T> T invoke(Function<Jedis,I> callFunction,
                               Function<MultiKeyPipelineBase,Response<I>> pipelineBaseResponseFunction,
                               Converter<I,T> converter,
                               Supplier<T> nullDefault){
            return (T) doInvoke((Function) callFunction, (Function) pipelineBaseResponseFunction,
                    (Converter<Object, Object>) converter,
                    (Supplier<Object>) nullDefault);
        }
    }

}
