package sachin.springframework.data.redis.connection.jedis;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.MultiKeyPipelineBase;
import redis.clients.jedis.Response;

import java.util.function.Function;
import java.util.function.Supplier;

public class JedisConnection {


    private final  JedisInvoker invoker=new JedisInvoker(new JedisInvoker.Synchronizer() {
        @Override
        public Object doInvoke(Function<Jedis, Object> callFunction, Function<MultiKeyPipelineBase, Response<Object>> pipelineFunction, Converter<Object, Object> converter, Supplier<Object> nullDefault) {

            return JedisConnection.this.doInvoke(false,callFunction,
                    pipelineFunction,converter,nullDefault);
        }
    });

    public JedisConnection(Jedis jedis) {
        this.jedis = jedis;
    }

    JedisInvoker invoker(){

        return invoker;
    }
    @Nullable
    private Object doInvoke(boolean status, Function<Jedis, Object> directFunction,
                            Function<MultiKeyPipelineBase, Response<Object>> pipelineFunction, Converter<Object, Object> converter,
                            Supplier<Object> nullDefault) {

        return doWithJedis(it->{

            Object result = directFunction.apply(getJedis());
            if (result == null) {
                return nullDefault.get();
            }
            return converter.convert(result);

        });

    }
    private <T> T doWithJedis(Function<Jedis,T>callback){
        try{
            return  callback.apply(getJedis());
        }catch (Exception exception){
            throw  exception;
        }
    }

    private final Jedis jedis;
    public Jedis getJedis(){
        return jedis;
    }


}
