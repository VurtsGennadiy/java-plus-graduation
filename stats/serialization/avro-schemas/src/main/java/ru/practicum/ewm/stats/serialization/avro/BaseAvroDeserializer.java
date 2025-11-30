package ru.practicum.ewm.stats.serialization.avro;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Arrays;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<SpecificRecordBase> {
    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final DatumReader<T> reader;

    public BaseAvroDeserializer(Schema schema) {
        reader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        Decoder decoder = decoderFactory.binaryDecoder(data, null);
        try {
            if (data == null) {
                return null;
            }
            return reader.read(null, decoder);
        } catch (IOException ex) {
            throw new SerializationException(
                    String.format("Deserializing data error. Data: %s, for topic: %s", Arrays.toString(data), topic),
                    ex);
        }
    }
}
