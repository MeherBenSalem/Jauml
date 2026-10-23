package tn.naizo.jauml;

import tn.naizo.jauml.api.ConfigFile;
import tn.naizo.jauml.api.JaumlConfig;
import java.util.List;

/**
 * Legacy static configuration utility class.
 * <p>
 * Kept with unchanged signatures for the whole 3.x line: released mods link against these exact methods,
 * some by reflection.
 *
 * @deprecated Use the modern, cached, and thread-safe instance-based API via
 * {@link JaumlConfig#open(String, String)}, or {@link JaumlConfig#bind(String, String, Class)} for typed configs.
 */
@Deprecated(since = "2.0")
public class JaumlConfigLib {

    private JaumlConfigLib() {}

    public static boolean createConfigFile(String dir, String fileName) {
        try {
            JaumlConfig.open(dir, fileName).save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean stringExistsInArray(String dir, String fileName, String arrayKey, String targetString) {
        try {
            return JaumlConfig.open(dir, fileName).listContains(arrayKey, targetString);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean arrayKeyExists(String dir, String fileName, String key) {
        try {
            return JaumlConfig.open(dir, fileName).hasKey(key);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean addStringToArray(String dir, String fileName, String arrayKey, String stringToAdd) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            boolean added = file.addToList(arrayKey, stringToAdd);
            if (added) {
                file.save();
            }
            return added;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean configFileExists(String dir, String fileName) {
        try {
            return JaumlConfig.open(dir, fileName).exists();
        } catch (Exception e) {
            return false;
        }
    }

    public static int getArrayLength(String dir, String fileName, String arrayKey) {
        try {
            return JaumlConfig.open(dir, fileName).getStringList(arrayKey).size();
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getArrayElement(String dir, String fileName, String arrayKey, int index) {
        try {
            List<String> list = JaumlConfig.open(dir, fileName).getStringList(arrayKey);
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
        } catch (Exception e) {
            // silent catch
        }
        return null;
    }

    public static String getStringValue(String dir, String fileName, String key) {
        try {
            return JaumlConfig.open(dir, fileName).getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    public static double getNumberValue(String dir, String fileName, String key) {
        try {
            return JaumlConfig.open(dir, fileName).getDouble(key, 0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static boolean setStringValue(String dir, String fileName, String key, String value) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            file.set(key, value);
            file.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean setNumberValue(String dir, String fileName, String key, double value) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            file.set(key, value);
            file.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean getBooleanValue(String dir, String fileName, String key) {
        try {
            return JaumlConfig.open(dir, fileName).getBoolean(key, false);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean setBooleanValue(String dir, String fileName, String key, boolean value) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            file.set(key, value);
            file.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean clearArray(String dir, String fileName, String arrayKey) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            file.clearList(arrayKey);
            file.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean removeArrayElement(String dir, String fileName, String arrayKey, String valueToRemove) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            boolean removed = file.removeFromList(arrayKey, valueToRemove);
            if (removed) {
                file.save();
            }
            return removed;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> getArrayAsList(String dir, String fileName, String arrayKey) {
        try {
            return JaumlConfig.open(dir, fileName).getStringList(arrayKey);
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    public static String getStringValue(String dir, String fileName, String key, String defaultValue) {
        try {
            return JaumlConfig.open(dir, fileName).getString(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double getNumberValue(String dir, String fileName, String key, double defaultValue) {
        try {
            return JaumlConfig.open(dir, fileName).getDouble(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean getBooleanValue(String dir, String fileName, String key, boolean defaultValue) {
        try {
            return JaumlConfig.open(dir, fileName).getBoolean(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getIntValue(String dir, String fileName, String key) {
        try {
            return JaumlConfig.open(dir, fileName).getInt(key, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getIntValue(String dir, String fileName, String key, int defaultValue) {
        try {
            return JaumlConfig.open(dir, fileName).getInt(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getLongValue(String dir, String fileName, String key) {
        try {
            return JaumlConfig.open(dir, fileName).getLong(key, 0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    public static long getLongValue(String dir, String fileName, String key, long defaultValue) {
        try {
            return JaumlConfig.open(dir, fileName).getLong(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean setIntValue(String dir, String fileName, String key, int value) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            file.set(key, value);
            file.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean setLongValue(String dir, String fileName, String key, long value) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            file.set(key, value);
            file.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean deleteConfigFile(String dir, String fileName) {
        try {
            return JaumlConfig.open(dir, fileName).delete();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasKey(String dir, String fileName, String key) {
        try {
            return JaumlConfig.open(dir, fileName).hasKey(key);
        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> getAllKeys(String dir, String fileName) {
        try {
            return new java.util.ArrayList<>(JaumlConfig.open(dir, fileName).keys());
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    public static boolean removeKey(String dir, String fileName, String key) {
        try {
            ConfigFile file = JaumlConfig.open(dir, fileName);
            boolean removed = file.removeKey(key);
            if (removed) {
                file.save();
            }
            return removed;
        } catch (Exception e) {
            return false;
        }
    }
}
