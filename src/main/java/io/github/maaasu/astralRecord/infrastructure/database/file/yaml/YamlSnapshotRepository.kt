package io.github.maaasu.astralRecord.infrastructure.database.file.yaml

import io.github.maaasu.astralRecord.infrastructure.database.sqlserver.SqlServerManager
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * YAMLスナップショットリポジトリ
 * SQLデータベースからスナップショットデータを読み書きします
 */
class YamlSnapshotRepository {

    private val sqlServerManager: SqlServerManager = SqlServerManager.getInstance()

    /**
     * ファイルパスからスナップショットを取得します
     * @param filePath ファイルパス
     * @return YamlSnapshot（存在しない場合はnull）
     */
    fun findByFilePath(filePath: String): CompletableFuture<YamlSnapshot?> {
        return CompletableFuture.supplyAsync {
            val sql = """
                SELECT snapshot_id, file_path, file_hash, content_json, created_at, updated_at
                FROM yaml_snapshot
                WHERE file_path = ?
            """.trimIndent()

            try {
                sqlServerManager.connection.use { conn ->
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, filePath)
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                mapResultSetToSnapshot(rs)
                            } else {
                                null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                //LoggerUtil.error(Messages.SNAPSHOT_GET_FAILED.format(filePath), e)
                null
            }
        }
    }

    /**
     * スナップショットを保存します（INSERT or UPDATE）
     * @param snapshot 保存するスナップショット
     * @return 成功した場合true
     */
    fun save(snapshot: YamlSnapshot): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val sql = """
                MERGE INTO yaml_snapshot AS target
                USING (SELECT ? AS file_path) AS source
                ON target.file_path = source.file_path
                WHEN MATCHED THEN
                    UPDATE SET 
                        file_hash = ?,
                        content_json = ?,
                        updated_at = GETDATE()
                WHEN NOT MATCHED THEN
                    INSERT (snapshot_id, file_path, file_hash, content_json, created_at, updated_at)
                    VALUES (?, ?, ?, ?, GETDATE(), GETDATE());
            """.trimIndent()

            try {
                sqlServerManager.connection.use { conn ->
                    conn.prepareStatement(sql).use { stmt ->
                        var idx = 1
                        // MERGE ON条件
                        stmt.setString(idx++, snapshot.filePath)
                        // UPDATE用
                        stmt.setString(idx++, snapshot.fileHash)
                        stmt.setString(idx++, snapshot.contentJson)
                        // INSERT用
                        stmt.setString(idx++, snapshot.snapshotId.toString())
                        stmt.setString(idx++, snapshot.filePath)
                        stmt.setString(idx++, snapshot.fileHash)
                        stmt.setString(idx, snapshot.contentJson)

                        stmt.executeUpdate()
                    }
                }
                true
            } catch (e: Exception) {
                //LoggerUtil.error(Messages.SNAPSHOT_SAVE_FAILED.format(snapshot.filePath), e)
                false
            }
        }
    }

    /**
     * スナップショットを削除します
     * @param filePath 削除するファイルパス
     * @return 成功した場合true
     */
    fun delete(filePath: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val sql = "DELETE FROM yaml_snapshot WHERE file_path = ?"

            try {
                sqlServerManager.connection.use { conn ->
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, filePath)
                        stmt.executeUpdate()
                    }
                }
                true
            } catch (e: Exception) {
                //LoggerUtil.error(Messages.SNAPSHOT_DELETE_FAILED.format(filePath), e)
                false
            }
        }
    }

    /**
     * 全てのスナップショットを取得します
     * @return YamlSnapshotのリスト
     */
    fun findAll(): CompletableFuture<List<YamlSnapshot>> {
        return CompletableFuture.supplyAsync {
            val sql = """
                SELECT snapshot_id, file_path, file_hash, content_json, created_at, updated_at
                FROM yaml_snapshot
            """.trimIndent()

            val snapshots = mutableListOf<YamlSnapshot>()
            try {
                sqlServerManager.connection.use { conn ->
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                snapshots.add(mapResultSetToSnapshot(rs))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                //LoggerUtil.error(Messages.SNAPSHOT_LIST_GET_FAILED.getMessage(), e)
            }
            snapshots
        }
    }

    private fun mapResultSetToSnapshot(rs: ResultSet): YamlSnapshot {
        return YamlSnapshot(
            snapshotId = UUID.fromString(rs.getString("snapshot_id")),
            filePath = rs.getString("file_path"),
            fileHash = rs.getString("file_hash"),
            contentJson = rs.getString("content_json"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
        )
    }
}
