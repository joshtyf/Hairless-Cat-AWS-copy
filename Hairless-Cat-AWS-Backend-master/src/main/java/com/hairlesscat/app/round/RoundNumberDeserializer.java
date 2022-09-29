package com.hairlesscat.app.round;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;

import java.util.HashMap;
import java.util.Map;

public class RoundNumberDeserializer extends JsonDeserializer<Map<TournamentTimeslot, Integer>> {
	@Override
	public Map<TournamentTimeslot, Integer> deserialize(JsonParser p, DeserializationContext ctx) {
		return new HashMap<>();
	}
}
