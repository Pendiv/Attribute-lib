package net.logiench.logienchlibv2.api.database.level;

import net.logiench.logienchlibv2.api.database.nbt.NbtMapView;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import org.bukkit.persistence.PersistentDataContainer;
import org.cloudburstmc.nbt.NbtMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class LDBManager {

	private final LDBConnection ldb;
	private final LDBManager root;
	private final String path;

	protected LDBManager(LDBConnection ldb) {
		this(ldb, null, "");
	}

	private LDBManager(LDBConnection ldb, LDBManager root, String path) {
		this.ldb = ldb;
		this.root = root;
		this.path = path;
	}

	/**
	 * 指定されたグループに移動します。
	 *
	 * @param pos 移動先の相対パス
	 */
	public LDBManager goTo(String pos) {
		return new LDBManager(ldb, this, path + "/" + pos);
	}

	/**
	 * このディレクトリの一つ上のLDBManagerを取得します
	 *
	 * @return root, これがルートパスの場合はnullになります
	 */
	public LDBManager getRoot() {
		return root;
	}

	/**
	 * 指定されたキーが存在するかどうかを確認します。
	 *
	 * @param key キー
	 * @return キーが存在する場合は true, 存在しない場合は false
	 */
	public boolean contains(String key) {
		return ldb.contains(path + "/" + key);
	}

	/**
	 * 指定されたキーを削除します。
	 */
	public void remove(String key) {
		ldb.remove(this.path + "/" + key);
	}

	public byte[] get(String key) {
		return ldb.get(this.path + "/" + key);
	}

	public PersistentDataContainer getAsPdc(String key) {
		try {
			byte[] bytes = get(key);
			if (bytes == null) {
				return null;
			}
			PersistentDataContainer pdc = DataContainer.createNewContainer();
			pdc.readFromBytes(bytes);
			return pdc;
		} catch (IOException ignored) {
		}
		return null;
	}

	public NbtMapView getAsNbtMap(String key) {
		byte[] bytes = get(key);
		if (bytes == null) {
			return null;
		}

		try {
			return new NbtMapView(bytes);
		} catch (RuntimeException ignored) {
		}
		return null;
	}

	public PersistentDataContainer getOrCreateAsPdc(String key) {
		PersistentDataContainer view = getAsPdc(key);
		if (view == null) {
			return DataContainer.createNewContainer();
		}

		return view;
	}

	public NbtMapView getOrCreateAsNbtMap(String key) {
		NbtMapView view = getAsNbtMap(key);
		if (view == null) {
			return new NbtMapView(NbtMap.builder().build());
		}

		return view;
	}

	/**
	 * 指定されたキーのデータを String として取得します。
	 * (格納されているデータが何であろうとStringとして解釈しようとします)
	 *
	 * @param key キー
	 * @return データが存在する場合は String, 存在しない場合は null
	 */
	public String getAsString(String key) {
		byte[] bytes = get(key);
		if (bytes == null) {
			return null;
		}

		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * 指定されたキーのデータを String として取得します。
	 * (格納されているデータが何であろうとStringとして解釈しようとします)
	 *
	 * @param key          キー
	 * @param defaultValue デフォルト値
	 * @return データが存在する場合は String, 存在しない場合は defaultValue
	 */
	public String getAsString(String key, String defaultValue) {
		String string = getAsString(key);
		if (string == null) {
			return defaultValue;
		}

		return string;
	}

	/**
	 * 指定されたキーのデータを Integer として取得します。
	 * (格納されているデータが何であろうとIntegerとして解釈しようとします)
	 *
	 * @param key キー
	 * @return データが存在する場合は Integer, 存在しない場合は null
	 */
	public Integer getAsInteger(String key) {
		byte[] bytes = get(key);
		if (bytes == null) {
			return null;
		}

		return (bytes[0] & 0xFF) << 24 |
			(bytes[1] & 0xFF) << 16 |
			(bytes[2] & 0xFF) << 8 |
			(bytes[3] & 0xFF);
	}

	/**
	 * 指定されたキーのデータを Integer として取得します。
	 * (格納されているデータが何であろうとIntegerとして解釈しようとします)
	 *
	 * @param key          キー
	 * @param defaultValue デフォルト値
	 * @return データが存在する場合は Integer, 存在しない場合は defaultValue
	 */
	public int getAsInteger(String key, int defaultValue) {
		Integer integer = getAsInteger(key);
		if (integer == null) {
			return defaultValue;
		}

		return integer;
	}

	/**
	 * 指定されたキーのデータを Long として取得します。
	 * (格納されているデータが何であろうとLongとして解釈しようとします)
	 *
	 * @param key キー
	 * @return データが存在する場合は Long, 存在しない場合は null
	 */
	public Long getAsLong(String key) {
		byte[] bytes = get(key);
		if (bytes == null) {
			return null;
		}

		return ((long) (bytes[0] & 0xFF) << 56 |
			(long) (bytes[1] & 0xFF) << 48 |
			(long) (bytes[2] & 0xFF) << 40 |
			(long) (bytes[3] & 0xFF) << 32 |
			(long) (bytes[4] & 0xFF) << 24 |
			(bytes[5] & 0xFF) << 16 |
			(bytes[6] & 0xFF) << 8 |
			(bytes[7] & 0xFF));
	}

	/**
	 * 指定されたキーのデータを Long として取得します。
	 * (格納されているデータが何であろうとLongとして解釈しようとします)
	 *
	 * @param key          キー
	 * @param defaultValue デフォルト値
	 * @return データが存在する場合は Long, 存在しない場合は defaultValue
	 */
	public long getAsLong(String key, long defaultValue) {
		Long longValue = getAsLong(key);
		if (longValue == null) {
			return defaultValue;
		}

		return longValue;
	}

	/**
	 * 指定されたキーのデータを Float として取得します。
	 * (格納されているデータが何であろうとFloatとして解釈しようとします)
	 *
	 * @param key キー
	 * @return データが存在する場合は Float, 存在しない場合は null
	 */
	public Float getAsFloat(String key) {
		Integer integer = getAsInteger(key);
		if (integer == null) {
			return null;
		}

		return Float.intBitsToFloat(integer);
	}

	/**
	 * 指定されたキーのデータを Float として取得します。
	 * (格納されているデータが何であろうとFloatとして解釈しようとします)
	 *
	 * @param key          キー
	 * @param defaultValue デフォルト値
	 * @return データが存在する場合は Float, 存在しない場合は defaultValue
	 */
	public float getAsFloat(String key, float defaultValue) {
		Float floatValue = getAsFloat(key);
		if (floatValue == null) {
			return defaultValue;
		}

		return floatValue;
	}

	/**
	 * 指定されたキーのデータを Double として取得します。
	 * (格納されているデータが何であろうとDoubleとして解釈しようとします)
	 *
	 * @param key キー
	 * @return データが存在する場合は Double, 存在しない場合は null
	 */
	public Double getAsDouble(String key) {
		Long longValue = getAsLong(key);
		if (longValue == null) {
			return null;
		}

		return Double.longBitsToDouble(longValue);
	}

	/**
	 * 指定されたキーのデータを Double として取得します。
	 * (格納されているデータが何であろうとDoubleとして解釈しようとします)
	 *
	 * @param key          キー
	 * @param defaultValue デフォルト値
	 * @return データが存在する場合は Double, 存在しない場合は defaultValue
	 */
	public double getAsDouble(String key, double defaultValue) {
		Double doubleValue = getAsDouble(key);
		if (doubleValue == null) {
			return defaultValue;
		}

		return doubleValue;
	}

	/**
	 * 指定されたキーのデータを Byte として取得します。
	 * (格納されているデータが何であろうとByteとして解釈しようとします)
	 *
	 * @param key キー
	 * @return データが存在する場合は Byte, 存在しない場合は null
	 */
	public Byte getAsByte(String key) {
		byte[] bytes = get(key);
		if (bytes == null) {
			return null;
		}

		return bytes[0];
	}

	/**
	 * 指定されたキーのデータを Byte として取得します。
	 * (格納されているデータが何であろうとByteとして解釈しようとします)
	 *
	 * @param key          キー
	 * @param defaultValue デフォルト値
	 * @return データが存在する場合は Byte, 存在しない場合は defaultValue
	 */
	public byte getAsByte(String key, byte defaultValue) {
		Byte byteValue = getAsByte(key);
		if (byteValue == null) {
			return defaultValue;
		}

		return byteValue;
	}

	/**
	 * 指定されたキーのデータを Byte[] として取得します。
	 * (格納されているデータが何であろうとByte[]として解釈しようとします)
	 *
	 * @param key キー
	 * @return データが存在する場合は Byte[], 存在しない場合は null
	 */
	public byte[] getAsByteArray(String key) {
		return get(key);
	}

	/**
	 * 指定されたキーのデータを Byte[] として取得します。
	 * (格納されているデータが何であろうとByte[]として解釈しようとします)
	 *
	 * @param key          キー
	 * @param defaultValue デフォルト値
	 * @return データが存在する場合は Byte[], 存在しない場合は defaultValue
	 */
	public byte[] getAsByteArray(String key, byte[] defaultValue) {
		byte[] byteArrayValue = getAsByteArray(key);
		if (byteArrayValue == null) {
			return defaultValue;
		}

		return byteArrayValue;
	}

	public void put(String key, byte[] value) {
		ldb.put(path + "/" + key, value);
	}

	public boolean putPdc(String key, PersistentDataContainer pdc) {
		try {
			put(key, pdc.serializeToBytes());

			return true;
		} catch (IOException ignored) {
		}

		return false;
	}

	public boolean putNbtMap(String key, NbtMapView nbtMap) {
		try {
			put(key, nbtMap.serializeToBytes());

			return true;
		} catch (RuntimeException ignored) {
		}

		return false;
	}

	public boolean putString(String key, String value) {
		put(key, value.getBytes(StandardCharsets.UTF_8));

		return true;
	}

	public boolean putInteger(String key, int value) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (value >> 24);
		bytes[1] = (byte) (value >> 16);
		bytes[2] = (byte) (value >> 8);
		bytes[3] = (byte) value;

		put(key, bytes);

		return true;
	}

	public boolean putLong(String key, long value) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) (value >> 56);
		bytes[1] = (byte) (value >> 48);
		bytes[2] = (byte) (value >> 40);
		bytes[3] = (byte) (value >> 32);
		bytes[4] = (byte) (value >> 24);
		bytes[5] = (byte) (value >> 16);
		bytes[6] = (byte) (value >> 8);
		bytes[7] = (byte) value;

		put(key, bytes);

		return true;
	}

	public boolean putFloat(String key, float value) {
		return putInteger(key, Float.floatToIntBits(value));
	}

	public boolean putDouble(String key, double value) {
		return putLong(key, Double.doubleToLongBits(value));
	}

	public boolean putByte(String key, byte value) {
		put(key, new byte[]{value});
		return true;
	}

	public boolean putByteArray(String key, byte[] value) {
		put(key, value);
		return true;
	}
}
