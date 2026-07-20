package ashlib.shmo.aotd.lib.api.campaign;

import ashlib.shmo.aotd.lib.api.general.Option;
import ashlib.shmo.aotd.lib.api.general.Utilities;

public interface CampaignData {
    <T> T getOrConstruct(String key, Utilities.Constructor<T> constructor);
    <T> Option<T> getAs(String key);
    Option<Object> getAs(String key, Class<?> clazz);
    <T> void set(String key, T value);
    boolean contains(String key);
    boolean containsAs(String key, Class<?> clazz);
}
