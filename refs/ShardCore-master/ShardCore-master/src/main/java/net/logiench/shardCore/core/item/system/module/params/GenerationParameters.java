package net.logiench.shardCore.core.item.system.module.params;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Getter;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ※ 注意
 * このクラスはGsonによって変換されます。
 * 内部のデータも含めて変換されるため、<b>ParamKeyを追加した際には必ず検証を行ってください。</b>
 * もし変換が正常にできなかった場合はJsonAdapterの実装を検討してください
 */
@JsonAdapter(GenerationParameters.Serializer.class)
public class GenerationParameters {

	/**
	 * ステータスを生成するための乱数のシードです。
	 * 鑑定前と後で同じステータスを出力するために使用します。
	 */
	public static final GenParamKey<Long> SEED =
		new GenParamKey<>("seed", Long.class);

	// ----------  CustomModel  ----------
	/**
	 * アイテムのカスタムモデルを上書きするのに使用します。
	 * この値は {@link ShardItem#getId()}を100倍した値に加算され、アイテムに適応されます。
	 * 0~99までは安全に扱えますが、それ以上の値では他のアイテムのモデルを参照してしまう可能性があります。
	 * <br>ex: <code>id: 101, offset: 5 -> customModel: 10105</code>
	 */
	public static final GenParamKey<NamespacedKey> MODEL =
		new GenParamKey<>("model", NamespacedKey.class);
	/**
	 * アイテムのカスタムモデルの色を指定します。
	 * (例: 革装備、ポーション、モデルの特定部位)
	 */
	public static final GenParamKey<List<Color>> MODEL_COLORS = new GenParamKey<>("model_colors", new TypeToken<>() {});
	/**
	 * アイテムのカスタムモデルのフラグを指定します。
	 * (例: 覚醒状態、エフェクトON/OFF)
	 */
	public static final GenParamKey<List<Boolean>> MODEL_FLAGS = new GenParamKey<>("model_flags", new TypeToken<>() {});
	/**
	 * アイテムのカスタムモデルの数値を指定します。
	 * (例: チャージ率、進行度)
	 */
	public static final GenParamKey<List<Float>> MODEL_FLOATS = new GenParamKey<>("model_floats", new TypeToken<>() {});
	/**
	 * アイテムのカスタムモデルの文字列を指定します。
	 * (例: 属性、タイプ)
	 */
	public static final GenParamKey<List<String>> MODEL_STRINGS = new GenParamKey<>("model_strings", new TypeToken<>() {});

	// ----------------------------------------------------------------------------------------------------

	private static final GenerationParameters EMPTY = new GenerationParameters().setImmutable();

	// 内部データ
	private final Map<GenParamKey<?>, Object> params = new HashMap<>();
	@Getter
	private transient boolean immutable = false;

	@Unmodifiable
	@NotNull
	public static GenerationParameters empty() {
		return EMPTY;
	}

	@NotNull
	public static GenerationParameters of() {
		return new GenerationParameters();
	}

	@NotNull
	public static <T> GenerationParameters of(@NotNull GenParamKey<T> key, T value) {
		var p = of();
		p.put(key, value);
		return p;
	}

	/**
	 * 指定されたインスタンスの内容をコピーし、編集可能な状態で返します。
	 * このコピーは浅いコピーで、GenerationParametersの内容はコピー元と同様のインスタンスです。
	 *
	 * @param parameters コピー元パラメータ
	 */
	@NotNull
	public static GenerationParameters of(@Nullable GenerationParameters parameters) {
		GenerationParameters p = of();
		if (parameters != null) {
			p.params.putAll(parameters.params);
		}
		return p;
	}

	/**
	 * 指定されたインスタンスの内容をコピーし、編集不可な状態で返します。
	 * このコピーは浅いコピーで、GenerationParametersの内容はコピー元と同様のインスタンスです。
	 */
	public static GenerationParameters ofImmutable(@Nullable GenerationParameters parameters) {
		if (parameters == null) {
			return EMPTY;
		}
		return of(parameters).setImmutable();
	}

	private GenerationParameters() {
	}

	public boolean isEmpty() {
		return params.isEmpty();
	}

	public void merge(@NotNull GenerationParameters parameters) {
		if (parameters.isEmpty()) {
			return;
		}
		params.putAll(parameters.params);
	}

	public <T> void put(@NotNull GenParamKey<T> key, T value) {
		if (immutable) {
			throw new IllegalStateException("このクラスは編集できません");
		}
		params.put(key, value);
	}

	@Nullable
	public <T> T get(@NotNull GenParamKey<T> key) {
		return key.cast(params.get(key));
	}

	@Contract(value = "_, !null -> !null")
	public <T> T get(@NotNull GenParamKey<T> key, T defaultValue) {
		Object val = params.get(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return key.cast(val);
		} catch (ClassCastException e) {
			return defaultValue;
		}
	}

	public <T> T getOrGet(@NotNull GenParamKey<T> key, Supplier<T> defaultValueSupplier) {
		Object val = params.get(key);
		if (val == null) {
			return defaultValueSupplier.get();
		}
		try {
			return key.cast(val);
		} catch (ClassCastException e) {
			return defaultValueSupplier.get();
		}
	}

	/**
	 * 指定されたキーのデータがnullではない場合のみConsumerの処理を実行します。
	 *
	 * @param key      取得するデータのキー
	 * @param consumer nullではない場合に実行する内容
	 * @return データがnullではなく、処理が実行できた場合はtrue, そうではない場合はfalse
	 */
	public <T> boolean ifPresent(@NotNull GenParamKey<T> key, @NotNull Consumer<T> consumer) {
		T value = get(key);
		if (value != null) {
			consumer.accept(value);
			return true;
		}
		return false;
	}

	public boolean has(GenParamKey<?> key) {
		return params.containsKey(key);
	}

	public GenerationParameters setImmutable() {
		this.immutable = true;
		return this;
	}


	// Gsonの変換処理に使用する
	static class Serializer implements TypeAdapterFactory {
		@SuppressWarnings("unchecked")
		@Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			// 対象が GenerationParameters でない場合は null を返して Gson に任せる
			if (type.getRawType() != GenerationParameters.class) {
				return null;
			}

			return (TypeAdapter<T>) new GenerationParametersAdapter(gson);
		}

		static class GenerationParametersAdapter extends TypeAdapter<GenerationParameters> {
			private final Gson gson;

			public GenerationParametersAdapter(Gson gson) {
				this.gson = gson;
			}

			@Override
			public void write(JsonWriter out, GenerationParameters src) throws IOException {
				if (src == null) {
					out.nullValue();
					return;
				}
				out.beginObject();

				// フラットなJSONとして書き出す
				for (Map.Entry<GenParamKey<?>, Object> entry : src.params.entrySet()) {
					out.name(entry.getKey().getKey()); // キー名
					// 受け取った gson を使って書き出し
					gson.toJson(entry.getValue(), entry.getKey().getType(), out);
				}

				out.endObject();
			}

			@Override
			public GenerationParameters read(JsonReader in) throws IOException {
				GenerationParameters dest = new GenerationParameters();
				in.beginObject();

				while (in.hasNext()) {
					String name = in.nextName();
					GenParamKey<?> key = GenParamKey.getByKey(name);
					if (key != null) {
						// 受け取った gson を使って読み込み
						Object value = gson.fromJson(in, key.getType());
						dest.params.put(key, value);
					} else {
						in.skipValue();
					}
				}
				in.endObject();
				return dest;
			}
		}
	}
}
