package io.github.maaasu.astralRecord.feature.user.repository

import io.github.maaasu.astralRecord.feature.user.model.UserModel
import io.github.maaasu.astralRecord.infrastructure.database.sqlserver.SqlServerManager
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

/**
 * dbo.user テーブルへのデータアクセスを担うリポジトリ。
 * 生 JDBC（PreparedStatement）を使用します。
 */
class UserRepository {

    private val ds get() = SqlServerManager.getInstance().dataSource

    // -------------------------------------------------------
    // SELECT
    // -------------------------------------------------------

    /**
     * UUID でユーザーを取得します（論理削除除外）。
     */
    fun findByUuid(uuid: UUID): UserModel? {
        val sql = """
            SELECT * FROM ${UserTable.TABLE_NAME}
            WHERE ${UserTable.UUID} = ? AND ${UserTable.IS_DELETED} = 0
        """.trimIndent()
        return ds.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toUserModel() else null
                }
            }
        }
    }

    // -------------------------------------------------------
    // INSERT
    // -------------------------------------------------------

    /**
     * 新規ユーザーを登録します。
     */
    fun insert(model: UserModel) {
        val sql = """
            INSERT INTO ${UserTable.TABLE_NAME} (
                ${UserTable.UUID}, ${UserTable.MCID},
                ${UserTable.JOIN_DATE}, ${UserTable.LAST_JOIN_DATE}, ${UserTable.GLOBAL_IP},
                ${UserTable.ACCOUNT_ID}, ${UserTable.BAN_INDEFINITE}, ${UserTable.BAN_DATE},
                ${UserTable.KICK_IP}, ${UserTable.PERMISSION}, ${UserTable.CREATED_AT}, ${UserTable.UPDATED_AT},
                ${UserTable.CREATED_BY}, ${UserTable.UPDATED_BY}, ${UserTable.IS_DELETED}
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ds.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                var i = 1
                stmt.setString(i++, model.uuid.toString())
                stmt.setString(i++, model.mcid)
                stmt.setTimestamp(i++, Timestamp.valueOf(model.joinDate))
                stmt.setTimestamp(i++, Timestamp.valueOf(model.lastJoinDate))
                stmt.setString(i++, model.globalIp)
                if (model.accountId != null) stmt.setString(i++, model.accountId.toString())
                else stmt.setNull(i++, java.sql.Types.OTHER)
                stmt.setBoolean(i++, model.banIndefinite)
                stmt.setTimestamp(i++, model.banDate?.let { Timestamp.valueOf(it) })
                stmt.setBoolean(i++, model.kickIp)
                stmt.setInt(i++,     model.permission)
                stmt.setTimestamp(i++, Timestamp.valueOf(model.createdAt))
                stmt.setTimestamp(i++, Timestamp.valueOf(model.updatedAt))
                stmt.setString(i++, model.createdBy.toString())
                stmt.setString(i++, model.updatedBy.toString())
                stmt.setBoolean(i,   model.isDeleted)
                stmt.executeUpdate()
            }
        }
    }

    // -------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------

    /**
     * 選択中アカウント UUID のみを更新します。
     * 新規ユーザー登録フローの STEP3（account 作成後の account_id 紐付け）で使用します。
     */
    fun updateAccountId(uuid: UUID, accountId: UUID, updatedBy: UUID) {
        val sql = """
            UPDATE ${UserTable.TABLE_NAME}
            SET ${UserTable.ACCOUNT_ID} = ?,
                ${UserTable.UPDATED_AT} = ?,
                ${UserTable.UPDATED_BY} = ?
            WHERE ${UserTable.UUID} = ?
        """.trimIndent()
        val now = LocalDateTime.now()
        ds.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, accountId.toString())
                stmt.setTimestamp(2, Timestamp.valueOf(now))
                stmt.setString(3, updatedBy.toString())
                stmt.setString(4, uuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 最終参加日時・グローバル IP・選択アカウントを更新します。
     */
    fun updateJoinInfo(uuid: UUID, ip: String, accountId: UUID, updatedBy: UUID) {
        val sql = """
            UPDATE ${UserTable.TABLE_NAME}
            SET ${UserTable.LAST_JOIN_DATE} = ?,
                ${UserTable.GLOBAL_IP}      = ?,
                ${UserTable.ACCOUNT_ID}     = ?,
                ${UserTable.UPDATED_AT}     = ?,
                ${UserTable.UPDATED_BY}     = ?
            WHERE ${UserTable.UUID} = ?
        """.trimIndent()
        val now = LocalDateTime.now()
        ds.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setTimestamp(1, Timestamp.valueOf(now))
                stmt.setString(2, ip)
                stmt.setString(3, accountId.toString())
                stmt.setTimestamp(4, Timestamp.valueOf(now))
                stmt.setString(5, updatedBy.toString())
                stmt.setString(6, uuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    // -------------------------------------------------------
    // Mapping
    // -------------------------------------------------------

    private fun ResultSet.toUserModel() = UserModel(
        uuid          = UUID.fromString(getString(UserTable.UUID)),
        mcid          = getString(UserTable.MCID),
        joinDate      = getTimestamp(UserTable.JOIN_DATE).toLocalDateTime(),
        lastJoinDate  = getTimestamp(UserTable.LAST_JOIN_DATE).toLocalDateTime(),
        globalIp      = getString(UserTable.GLOBAL_IP),
        accountId     = getString(UserTable.ACCOUNT_ID)?.let { UUID.fromString(it) },
        banIndefinite = getBoolean(UserTable.BAN_INDEFINITE),
        banDate       = getTimestamp(UserTable.BAN_DATE)?.toLocalDateTime(),
        kickIp        = getBoolean(UserTable.KICK_IP),
        permission    = getInt(UserTable.PERMISSION),
        createdAt     = getTimestamp(UserTable.CREATED_AT).toLocalDateTime(),
        updatedAt     = getTimestamp(UserTable.UPDATED_AT).toLocalDateTime(),
        createdBy     = UUID.fromString(getString(UserTable.CREATED_BY)),
        updatedBy     = UUID.fromString(getString(UserTable.UPDATED_BY)),
        isDeleted     = getBoolean(UserTable.IS_DELETED),
    )
}
