package fr.fezlight.eventsystem.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;


public class ClassConverters {
    @ReadingConverter
    public static class ClassReadConverter implements Converter<String, Class<?>> {
        @Override
        public Class<?> convert(String source) {
            try {
                return Class.forName(source);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @WritingConverter
    public static class ClassWriteConverter implements Converter<Class<?>, String> {
        @Override
        public String convert(Class<?> source) {
            return source.getName();
        }
    }
}
