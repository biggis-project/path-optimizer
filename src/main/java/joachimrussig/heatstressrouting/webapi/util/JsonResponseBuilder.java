package joachimrussig.heatstressrouting.webapi.util;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

public class JsonResponseBuilder {

	private ResponseStatus status;
	private JsonArrayBuilder messages = Json.createArrayBuilder();
	private JsonObjectBuilder results = Json.createObjectBuilder();

	public JsonResponseBuilder(ResponseStatus status) {
		this.status = status;
	}

	public JsonResponseBuilder addMessage(JsonValue msg) {
		this.messages.add(msg);
		return this;
	}

	public JsonResponseBuilder addMessage(String msg) {
		JsonString m = Json.createObjectBuilder().add("msg", msg).build()
				.getJsonString("msg");
		this.messages.add(m);
		return this;
	}
	
	public JsonResponseBuilder addResult(String name, JsonValue res) {
		this.results.add(name, res);
		return this;
	}

	public JsonResponseBuilder addMessages(Collection<JsonValue> msgs) {
		for (JsonValue msg : msgs) {
			this.messages.add(msg);
		}
		return this;
	}

	public JsonResponseBuilder addStringMessages(Collection<String> msgs) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		msgs.forEach(msg -> builder.add(msg));
		return addMessages(
				builder.build().stream().collect(Collectors.toList()));
	}

	public Response build() {
		JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("status", this.status.toString())
				.add("status_code", this.status.getHttpStatusCode());

		JsonArray messageArray = this.messages.build(); 
		JsonObject resultObject = this.results.build();
		
		if (!messageArray.isEmpty())
			builder.add("messages", messageArray);
		
		if (!resultObject.isEmpty())
			builder.add("results", resultObject);

		JsonObject body = builder.build();
		return Response.status(this.status.toStatus()).entity(JsonUtils.toString(body))
				.build();
	}

	public ResponseStatus getStatus() {
		return status;
	}

	public JsonArray getMessages() {
		return messages.build();
	}
	
	public JsonObject getResults() {
		return results.build();
	}

}
