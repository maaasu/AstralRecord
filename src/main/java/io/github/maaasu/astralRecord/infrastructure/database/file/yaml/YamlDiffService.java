package io.github.maaasu.astralRecord.infrastructure.database.file.yaml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * YAML差分検出サービス
 * 前回ロード時のYAMLデータと比較し、差分をデバッグ出力します
 */
public class YamlDiffService {

    private final YamlSnapshotRepository snapshotRepository;
    private final Gson gson;

    public YamlDiffService() {
        this.snapshotRepository = new YamlSnapshotRepository();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * YAMLファイルをロードし、前回との差分を検出してデバッグ出力します
     * @param file YAMLファイル
     * @param yaml ロードされたYamlConfiguration
     * @return 差分があった場合true
     */
    public CompletableFuture<Boolean> checkAndLogDiff(File file, YamlConfiguration yaml) {
        String filePath = file.getPath();
        String currentHash = calculateHash(yaml.saveToString());
        Map<String, Object> currentContent = yamlToMap(yaml);
        String currentJson = gson.toJson(currentContent);

        return snapshotRepository.findByFilePath(filePath).thenCompose(previousSnapshot -> {
            if (previousSnapshot == null) {
                // 初回ロード - スナップショットを保存
                //LoggerUtil.debug(Messages.YAML_FIRST_LOAD.format(filePath));
                YamlSnapshot newSnapshot = YamlSnapshot.Companion.create(filePath, currentHash, currentJson);
                return snapshotRepository.save(newSnapshot).thenApply(saved -> false);
            }

            // ハッシュが同じなら差分なし
            if (previousSnapshot.getFileHash().equals(currentHash)) {
                //LoggerUtil.debug(Messages.YAML_NO_CHANGE.format(filePath));
                return CompletableFuture.completedFuture(false);
            }

            // 差分を検出してログ出力
            Map<String, Object> previousContent = gson.fromJson(
                    previousSnapshot.getContentJson(),
                    new TypeToken<Map<String, Object>>() {}.getType()
            );
            logDifferences(filePath, previousContent, currentContent);

            // スナップショットを更新
            YamlSnapshot updatedSnapshot = YamlSnapshot.Companion.create(filePath, currentHash, currentJson);
            return snapshotRepository.save(updatedSnapshot).thenApply(saved -> true);
        });
    }

    /**
     * 差分をログ出力します
     * @param filePath ファイルパス
     * @param previous 前回の内容
     * @param current 現在の内容
     */
    private void logDifferences(String filePath, Map<String, Object> previous, Map<String, Object> current) {
        //LoggerUtil.info(Messages.YAML_DIFF_START.format(filePath));
        compareAndLogMap(null, previous, current);
        //LoggerUtil.info(Messages.YAML_DIFF_END.getMessage());
    }

    /**
     * マップの差分を比較してログ出力します
     * @param parentKey 親キー（トップレベルの場合はnull）
     * @param previous 前回の内容
     * @param current 現在の内容
     */
    private void compareAndLogMap(String parentKey, Map<String, Object> previous, Map<String, Object> current) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(previous.keySet());
        allKeys.addAll(current.keySet());

        for (String key : allKeys) {
            String fullKey = parentKey == null ? key : parentKey + "." + key;
            Object prevValue = previous.get(key);
            Object currValue = current.get(key);

            compareAndLogValue(fullKey, prevValue, currValue);
        }
    }

    /**
     * 値を比較してログ出力します
     * @param key キー
     * @param prevValue 前回の値
     * @param currValue 現在の値
     */
    private void compareAndLogValue(String key, Object prevValue, Object currValue) {
        if (prevValue == null && currValue != null) {
            //LoggerUtil.info(Messages.YAML_DIFF_ADDED.format(key, formatValue(currValue)));
        } else if (prevValue != null && currValue == null) {
            //LoggerUtil.info(Messages.YAML_DIFF_DELETED.format(key, formatValue(prevValue)));
        } else if (prevValue != null && !prevValue.equals(currValue)) {
            if (prevValue instanceof Map<?, ?> prevMap && currValue instanceof Map<?, ?> currMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> prevMapTyped = (Map<String, Object>) prevMap;
                @SuppressWarnings("unchecked")
                Map<String, Object> currMapTyped = (Map<String, Object>) currMap;
                compareAndLogMap(key, prevMapTyped, currMapTyped);
            } else {
                //LoggerUtil.info(Messages.YAML_DIFF_CHANGED.format(key, formatValue(prevValue), formatValue(currValue)));
            }
        }
    }

    /**
     * 値をフォーマットします
     * @param value 値
     * @return フォーマットされた文字列
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String str) {
            return "\"" + str + "\"";
        }
        return value.toString();
    }

    /**
     * YamlConfigurationをMapに変換します
     * @param yaml YamlConfiguration
     * @return Map
     */
    private Map<String, Object> yamlToMap(YamlConfiguration yaml) {
        return sectionToMap(yaml);
    }

    /**
     * ConfigurationSectionをMapに変換します
     * @param section ConfigurationSection
     * @return Map
     */
    private Map<String, Object> sectionToMap(org.bukkit.configuration.ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof org.bukkit.configuration.ConfigurationSection nestedSection) {
                map.put(key, sectionToMap(nestedSection));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * 文字列のSHA-256ハッシュを計算します
     * @param content 内容
     * @return ハッシュ文字列
     */
    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            //LoggerUtil.error(Messages.HASH_CALCULATION_FAILED.getMessage(), e);
            return "";
        }
    }
}
