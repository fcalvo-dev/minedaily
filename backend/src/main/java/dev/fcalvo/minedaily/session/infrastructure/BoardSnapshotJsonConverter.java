package dev.fcalvo.minedaily.session.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fcalvo.minedaily.session.domain.BoardSnapshot;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
/**
 * Stores the visible board snapshot as a single serialized column so the session
 * can be rehydrated later without introducing per-cell persistence yet.
 */
public class BoardSnapshotJsonConverter implements AttributeConverter<BoardSnapshot, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

	@Override
	public String convertToDatabaseColumn(BoardSnapshot boardSnapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(boardSnapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Could not serialize board snapshot", exception);
		}
	}

	@Override
	public BoardSnapshot convertToEntityAttribute(String value) {
		try {
			return OBJECT_MAPPER.readValue(value, BoardSnapshot.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Could not deserialize board snapshot", exception);
		}
	}

}
