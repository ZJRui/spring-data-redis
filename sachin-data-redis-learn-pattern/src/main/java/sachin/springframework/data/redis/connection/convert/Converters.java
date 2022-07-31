package sachin.springframework.data.redis.connection.convert;

import org.springframework.core.convert.converter.Converter;

public class Converters {


    /**
     * Returns a {@link Converter} that always returns its input argument.
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     * @since 2.5
     */
    public static <T> Converter<T, T> identityConverter() {
        return t -> t;
    }
}
