package net.logiench.shardCore.core.item.system.module.params;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import net.logiench.shardCore.core.item.base.def.GemItem;
import net.logiench.shardCore.core.itemRequirement.base.ItemRequirement;
import net.logiench.shardCore.core.itemRequirement.base.RequirementType;
import net.logiench.shardCore.data.prefix.Prefix;
import net.logiench.shardCore.register.GemRegistry;
import net.logiench.shardCore.register.PrefixRegistry;
import net.logiench.shardCore.register.RequirementRegistry;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * GenerationParametersで使用しているが、通常ではGson変換ができないパラメータをここで指定することで変換する
 */
public class GenParamsTypeAdapters {
	private GenParamsTypeAdapters() {
	}

	/**
	 * GenerationParametersで使用されているTypeAdapterのリスト
	 */
	private static final Map<Class<?>, Class<? extends TypeAdapter<?>>> TYPE_HIERARCHY_ADAPTERS = Map.of(
		GemItem.class, GemItemAdapter.class,
		Prefix.class, PrefixAdapter.class,
		ItemRequirement.class, RequirementAdapter.class
	);
	// 完全一致のクラス(ジェネリクスまで含む)
	private static final Map<Type, Class<? extends TypeAdapter<?>>> TYPE_ADAPTERS = Map.of(
	);


	public static void applyGsonBuilder(Injector injector, GsonBuilder builder) {
		for (Map.Entry<Class<?>, Class<? extends TypeAdapter<?>>> adapterEntry : TYPE_HIERARCHY_ADAPTERS.entrySet()) {
			// Hierarchy Adapter を使うことで、指定されたクラスの子クラスにもこのAdapterが適応されるようになる
			builder.registerTypeHierarchyAdapter(adapterEntry.getKey(), injector.getInstance(adapterEntry.getValue()));
		}
		for (Map.Entry<Type, Class<? extends TypeAdapter<?>>> adapterEntry : TYPE_ADAPTERS.entrySet()) {
			// こっちはそのクラスだけ、子クラスは関係ない
			builder.registerTypeAdapter(adapterEntry.getKey(), injector.getInstance(adapterEntry.getValue()));
		}
	}

	private static class GemItemAdapter extends TypeAdapter<GemItem> {
		private final GemRegistry registry;

		@Inject
		private GemItemAdapter(GemRegistry registry) {
			this.registry = registry;
		}

		@Override
		public void write(JsonWriter jsonWriter, GemItem gemItem) throws IOException {
			jsonWriter.value(gemItem.getId());
		}

		@Override
		public GemItem read(JsonReader jsonReader) throws IOException {
			return registry.get(jsonReader.nextString());
		}
	}

	private static class PrefixAdapter extends TypeAdapter<Prefix> {
		private final PrefixRegistry registry;

		@Inject
		private PrefixAdapter(PrefixRegistry registry) {
			this.registry = registry;
		}

		@Override
		public void write(JsonWriter jsonWriter, Prefix prefix) throws IOException {
			jsonWriter.value(prefix.getId());
		}

		@Override
		public Prefix read(JsonReader jsonReader) throws IOException {
			return registry.get(jsonReader.nextString());
		}
	}

	private static class RequirementAdapter extends TypeAdapter<ItemRequirement<?>> {
		private final RequirementRegistry registry;
		private final Provider<Gson> gsonProvider;

		@Inject
		private RequirementAdapter(RequirementRegistry registry, Provider<Gson> gsonProvider) {
			this.registry = registry;
			this.gsonProvider = gsonProvider;
		}

		@Override
		public void write(JsonWriter out, ItemRequirement<?> req) throws IOException {
			out.beginObject();

			out.name("type").value(req.type().getKeyName());
			out.name("value");
			gsonProvider.get().toJson(req.value(), req.type().getDataType(), out);

			out.endObject();
		}

		@Override
		public ItemRequirement<?> read(JsonReader in) {
			// ★ ストリームの順序に依存しないよう、一度 JsonObject（ツリー）として読み込む
			JsonElement tree = JsonParser.parseReader(in);
			if (!tree.isJsonObject()) {
				throw new JsonParseException("ItemRequirement must be a JSON Object");
			}
			JsonObject obj = tree.getAsJsonObject();

			// 1. "type" のID文字列を取得
			String typeId = obj.get("type").getAsString();

			// 2. Registryから条件の定義（RequirementType）を引っ張ってくる
			RequirementType<?> reqType = registry.getType(typeId);
			if (reqType == null) {
				throw new JsonParseException("Unknown Requirement Type: " + typeId);
			}

			// 3. "value" の中身を、reqType が指定する型（Integerなど）として Gson に復元してもらう！
			JsonElement valueElement = obj.get("value");
			Object parsedValue = gsonProvider.get().fromJson(valueElement, reqType.getDataType());

			// 4. 無事にインスタンス化して返す！
			return createRequirement(reqType, parsedValue);
		}

		// ジェネリクスのワイルドカード(?)を安全にキャストするためのヘルパーメソッド
		@SuppressWarnings("unchecked")
		private <T> ItemRequirement<T> createRequirement(RequirementType<T> type, Object value) {
			return new ItemRequirement<>(type, (T) value);
		}
	}
}
