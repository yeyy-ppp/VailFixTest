package gson.internal.bind;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import gson.TypeAdapter;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationDelegatingTypeAdapterTest {

    @Test
    void testGetSerializationDelegate() {
        SerializationDelegatingTypeAdapter<String> adapter = new SerializationDelegatingTypeAdapter<String>() {
            @Override
            public void write(gson.stream.JsonWriter out, String value) throws IOException {

            }

            @Override
            public String read(gson.stream.JsonReader in) throws IOException {
                return null;
            }

            @Override
            public TypeAdapter<String> getSerializationDelegate() {
                return new TypeAdapter<String>() {
                    @Override
                    public void write(gson.stream.JsonWriter out, String value) throws IOException {

                    }

                    @Override
                    public String read(gson.stream.JsonReader in) throws IOException {
                        return null;
                    }

                    @Override
                    public void write(JsonWriter out, String value) {}
                    @Override
                    public String read(JsonReader in) { return null; }
                };
            }

            @Override
            public void write(JsonWriter out, String value) {}

            @Override
            public String read(JsonReader in) { return null; }
        };

        TypeAdapter<String> delegate = adapter.getSerializationDelegate();
        assertNotNull(delegate);
        assertTrue(delegate instanceof TypeAdapter);
    }
}