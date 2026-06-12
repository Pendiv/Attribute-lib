package net.logiench.logienchlibv2.api.httpRequest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SuppressWarnings("unused")
public final class GET {
	private String response;
	private JsonObject json;

	public GET(@NotNull String rawUrl) {
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest request =
				HttpRequest.newBuilder()
					.uri(URI.create(rawUrl))
					.GET()
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			this.response = response.body();
			this.json = JsonParser.parseString(response.body()).getAsJsonObject();
		} catch (IOException | InterruptedException ignored) {
			this.response = null;
			this.json = null;
		}
	}

	@Nullable
	public String getResponse() {
		return response;
	}

	@Nullable
	public JsonObject getJson() {
		return json;
	}

	@Nullable
	public String getResponseValue(@Nullable String key) {
		if (json == null) {
			return null;
		}
		JsonElement element = json.get(key);
		if (element == null) {
			return null;
		}
		return element.getAsString();
	}
}

