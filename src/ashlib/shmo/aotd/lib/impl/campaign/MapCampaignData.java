package ashlib.shmo.aotd.lib.impl.campaign;

import ashlib.shmo.aotd.lib.api.campaign.CampaignData;
import ashlib.shmo.aotd.lib.api.general.Option;
import ashlib.shmo.aotd.lib.api.general.Utilities;

import java.util.HashMap;
import java.util.Map;

public class MapCampaignData implements CampaignData {
    private final Map<String, Object> data = new HashMap<>();

    @Override @SuppressWarnings("unchecked")
    public <T> T getOrConstruct(String key, Utilities.Constructor<T> constructor) {
        try {
            return (T) Utilities.getOrInsert(data, key, constructor::construct);
        } catch (ClassCastException ignored) {
            final T newValue = constructor.construct();
            data.put(key, newValue);
            return newValue;
        }
    }

    @Override @SuppressWarnings("unchecked")
    public <T> Option<T> getAs(String key) {
        try {
            return Option.of(data.get(key)).map((o) -> (T)o);
        } catch (ClassCastException ignored) {
            return Option.none();
        }
    }

    @Override
    public Option<Object> getAs(String key, Class<?> clazz) {
        try {
            return Option.of(data.get(key)).map(clazz::cast);
        } catch (ClassCastException ignored) {
            return Option.none();
        }
    }

    @Override
    public <T> void set(String key, T value) {
        data.put(key, value);
    }

    @Override
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsAs(String key, Class<?> clazz) {
        return getAs(key, clazz).isSome();
    }
}
