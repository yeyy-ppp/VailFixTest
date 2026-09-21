package cli;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
@FunctionalInterface
public interface Converter<T, E extends Exception> {
    Converter<?, RuntimeException> DEFAULT = s -> s;
    Converter<Class<?>, ClassNotFoundException> CLASS = Class::forName;
    Converter<File, NullPointerException> FILE = File::new;
    Converter<Path, InvalidPathException> PATH = Paths::get;
    Converter<Number, NumberFormatException> NUMBER =
            s -> s.indexOf('.') != -1 ? (Number) Double.valueOf(s) : (Number) Long.valueOf(s);
    Converter<Object, ReflectiveOperationException> OBJECT =
            s -> CLASS.apply(s).getConstructor().newInstance();
    Converter<URL, MalformedURLException> URL = URL::new;
    Converter<Date, java.text.ParseException> DATE =
            s -> new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(s);
    T apply(String string) throws E;
}

