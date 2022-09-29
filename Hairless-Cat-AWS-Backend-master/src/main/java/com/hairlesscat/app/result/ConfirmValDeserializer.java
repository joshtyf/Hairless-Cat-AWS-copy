package com.hairlesscat.app.result;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.hairlesscat.app.team.Team;

import java.util.HashMap;
import java.util.Map;

public class ConfirmValDeserializer extends JsonDeserializer<Map<Team, Boolean>> {
	@Override
	public Map<Team, Boolean> deserialize(JsonParser p, DeserializationContext ctx) {
		return new HashMap<>();
	}
}
