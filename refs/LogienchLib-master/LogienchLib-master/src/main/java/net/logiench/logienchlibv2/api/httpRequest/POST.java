package net.logiench.logienchlibv2.api.httpRequest;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public final class POST {
	private final URI uri;
	private final JsonObject json;

	public POST(@NotNull URI uri, @NotNull JsonObject json) {
		this.uri = uri;
		this.json = json;
	}

	public POST(@NotNull String rawUrl, @NotNull JsonObject json) {
		this(URI.create(rawUrl), json);
	}

	public POST(@NotNull String rawUrl) {
		this(rawUrl, new JsonObject());
	}

	public POST editJSON(Consumer<JsonObject> edit) {
		edit.accept(json);
		return this;
	}

	@NotNull
	public String send() {
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest request =
				HttpRequest.newBuilder()
					.uri(uri)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(json.getAsString(), StandardCharsets.UTF_8))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response.body();
		} catch (IOException | InterruptedException ignored) {
			return "";
		}
	}
}
