package com.hqt.demo.kafka;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;


@Component
public class productStream {
	
	 static <T extends SpecificRecord> SpecificAvroSerde<T> getSpecificAvroSerde(final Map<String, Object> serdeConfig) {
	        final SpecificAvroSerde<T> specificAvroSerde = new SpecificAvroSerde<>();
	        specificAvroSerde.configure(serdeConfig, false);
	        return specificAvroSerde;
	 }
	
    @Autowired
	public void process(StreamsBuilder builder) {
	   
		final Serde<String> stringSerde = Serdes.String();
		final Serde<Long> longSerde = Serdes.Long();

		KStream<String, String> textLines = builder.stream("hobbit", Consumed.with(stringSerde, stringSerde));
		textLines.peek((key, value) -> System.out.println("Incoming record - key " + key + " value " + value));
		KTable<String, Long> wordCounts = textLines
				// Split each text line, by whitespace, into words.
				.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
				// Group the text words as message keys
				.groupBy((key, value) -> value, Grouped.with(stringSerde, stringSerde))
				// Count the occurrences of each word 
				.count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts"));
		wordCounts.toStream()
				  .peek((key, value) -> System.out.println("Incoming record - key " + key + " value " + value))
				  //sysout: Incoming record - key fire value 4
				  .to("streams-wordcount-output", Produced.with(stringSerde, longSerde));
				  // output: received = ? with key fire
		
		
		 textLines.groupByKey() .reduce(((valueone, valuetwo) -> valueone + valuetwo))
	              .toStream() .peek((key, value) -> System.out.println("Incoming record2 - key " + key + " value " + value)) 
	              .to("operation", Produced.with(stringSerde, stringSerde));
		 
	}
}
