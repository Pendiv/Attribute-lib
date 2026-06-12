package net.logiench.logienchlibv2.api.database.nbt;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class NbtMapView {
	private final NbtMapBuilder builder;
	private NbtMap nbtMap;
	private boolean isChanged = false;

	public NbtMapView() {
		builder = NbtMap.builder();
		nbtMap = builder.build();
	}

	/// @throws RuntimeException IOExceptionが発生した際
	public NbtMapView(byte[] bytes) {
		try {
			builder = NbtMapBuilder.from((NbtMap) NbtUtils.createReader(new ByteArrayInputStream(bytes)).readTag());
			nbtMap = builder.build();
		} catch (IOException e) {
			throw new RuntimeException("IOException", e);
		}
	}

	public NbtMapView(NbtMap map) {
		builder = NbtMapBuilder.from(map);
		nbtMap = builder.build();
	}

	public void putString(String key, String value) {
		builder.putString(key, value);
		isChanged = true;
	}

	public void putInt(String key, int value) {
		builder.putInt(key, value);
		isChanged = true;
	}

	public void putLong(String key, long value) {
		builder.putLong(key, value);
		isChanged = true;
	}

	public void putFloat(String key, float value) {
		builder.putFloat(key, value);
		isChanged = true;
	}

	public void putDouble(String key, double value) {
		builder.putDouble(key, value);
		isChanged = true;
	}

	public void putBoolean(String key, boolean value) {
		builder.putBoolean(key, value);
		isChanged = true;
	}

	public void putByte(String key, byte value) {
		builder.putByte(key, value);
		isChanged = true;
	}

	public void putByteArray(String key, byte[] value) {
		builder.putByteArray(key, value);
		isChanged = true;
	}

	public void putShort(String key, short value) {
		builder.putShort(key, value);
		isChanged = true;
	}

	public void putNbtMap(String key, NbtMapView value) {
		builder.putCompound(key, value.build());
		isChanged = true;
	}

	public <T> void putList(String key, NbtType<T> type, List<T> value) {
		builder.putList(key, type, value);
		isChanged = true;
	}

	@SafeVarargs
	public final <T> void putList(String key, NbtType<T> type, T... value) {
		builder.putList(key, type, value);
		isChanged = true;
	}

	public void remove(String key) {
		builder.remove(key);
		isChanged = true;
	}

	public void remove(String key, Object value) {
		builder.remove(key, value);
		isChanged = true;
	}

	public String getString(String key) {
		updateOnChanged();
		return nbtMap.getString(key);
	}

	public String getString(String key, String defaultValue) {
		updateOnChanged();
		return nbtMap.getString(key, defaultValue);
	}

	public int getInt(String key) {
		updateOnChanged();
		return nbtMap.getInt(key);
	}

	public int getInt(String key, int defaultValue) {
		updateOnChanged();
		return nbtMap.getInt(key, defaultValue);
	}

	public long getLong(String key) {
		updateOnChanged();
		return nbtMap.getLong(key);
	}

	public long getLong(String key, long defaultValue) {
		updateOnChanged();
		return nbtMap.getLong(key, defaultValue);
	}

	public float getFloat(String key) {
		updateOnChanged();
		return nbtMap.getFloat(key);
	}

	public float getFloat(String key, float defaultValue) {
		updateOnChanged();
		return nbtMap.getFloat(key, defaultValue);
	}

	public double getDouble(String key) {
		updateOnChanged();
		return nbtMap.getDouble(key);
	}

	public double getDouble(String key, double defaultValue) {
		updateOnChanged();
		return nbtMap.getDouble(key, defaultValue);
	}

	public boolean getBoolean(String key) {
		updateOnChanged();
		return nbtMap.getBoolean(key);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		updateOnChanged();
		return nbtMap.getBoolean(key, defaultValue);
	}

	public byte getByte(String key) {
		updateOnChanged();
		return nbtMap.getByte(key);
	}

	public byte getByte(String key, byte defaultValue) {
		updateOnChanged();
		return nbtMap.getByte(key, defaultValue);
	}

	public byte[] getByteArray(String key) {
		updateOnChanged();
		return nbtMap.getByteArray(key);
	}

	public byte[] getByteArray(String key, byte[] defaultValue) {
		updateOnChanged();
		return nbtMap.getByteArray(key, defaultValue);
	}

	public short getShort(String key) {
		updateOnChanged();
		return nbtMap.getShort(key);
	}

	public short getShort(String key, short defaultValue) {
		updateOnChanged();
		return nbtMap.getShort(key, defaultValue);
	}

	public NbtMapView getNbtMap(String key) {
		updateOnChanged();
		return new NbtMapView(nbtMap.getCompound(key));
	}

	public NbtMapView getNbtMap(String key, NbtMapView defaultValue) {
		updateOnChanged();
		return new NbtMapView(nbtMap.getCompound(key, defaultValue.build()));
	}

	public <T> List<T> getList(String key, NbtType<T> type) {
		updateOnChanged();
		return nbtMap.getList(key, type);
	}

	public <T> List<T> getList(String key, NbtType<T> type, List<T> defaultValue) {
		updateOnChanged();
		return nbtMap.getList(key, type, defaultValue);
	}

	public Set<Map.Entry<String, Object>> entrySet() {
		updateOnChanged();
		return nbtMap.entrySet();
	}

	public Set<String> keySet() {
		updateOnChanged();
		return nbtMap.keySet();
	}

	public Collection<Object> values() {
		updateOnChanged();
		return nbtMap.values();
	}

	public boolean containsKey(String key) {
		updateOnChanged();
		return nbtMap.containsKey(key);
	}

	public boolean containsValue(Object value) {
		updateOnChanged();
		return nbtMap.containsValue(value);
	}

	public boolean isEmpty() {
		updateOnChanged();
		return nbtMap.isEmpty();
	}

	public int size() {
		updateOnChanged();
		return nbtMap.size();
	}

	public boolean equals(Object o) {
		updateOnChanged();
		if (o == this) {
			return true;
		}
		if (!(o instanceof NbtMapView nbt)) {
			return false;
		}
		return nbtMap.equals(nbt.nbtMap);
	}

	public NbtMap build() {
		return builder.build();
	}

	/// @throws RuntimeException IOExceptionが発生した際
	public byte[] serializeToBytes() {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			NbtUtils.createWriter(out).writeTag(build());
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("IOException", e);
		}
	}

	public static NbtMapView getEmpty() {
		return new NbtMapView(NbtMap.builder().build());
	}

	private void updateOnChanged() {
		if (!isChanged) {
			return;
		}
		nbtMap = build();
		isChanged = false;
	}
}