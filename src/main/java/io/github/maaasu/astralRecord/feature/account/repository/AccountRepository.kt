package io.github.maaasu.astralRecord.feature.account.repository

import io.github.maaasu.astralRecord.feature.account.model.AccountMode
import io.github.maaasu.astralRecord.feature.account.model.AccountModel
import io.github.maaasu.astralRecord.infrastructure.database.sqlserver.SqlServerManager
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

/**
 * dbo.account テーブルへのデータアクセスを担うリポジトリ。
 * 生 JDBC（PreparedStatement）を使用します。
 */
class AccountRepository {

    private val ds get() = SqlServerManager.getInstance().dataSource

    // -------------------------------------------------------
    // SELECT
    // -------------------------------------------------------

    /**
     * プレイヤー UUID に紐付くアカウント一覧を取得します（論理削除除外）。
     */
    fun findByUserId(userId: UUID): List<AccountModel> {
        val sql = """
            SELECT * FROM ${AccountTable.TABLE_NAME}
            WHERE ${AccountTable.USER_ID} = ? AND ${AccountTable.IS_DELETED} = 0
        """.trimIndent()
        return ds.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId.toString())
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AccountModel>()
                    while (rs.next()) list.add(rs.toAccountModel())
                    list
                }
            }
        }
    }

    /**
     * アカウント UUID でアカウントを取得します（論理削除除外）。
     */
    fun findByUuid(uuid: UUID): AccountModel? {
        val sql = """
            SELECT * FROM ${AccountTable.TABLE_NAME}
            WHERE ${AccountTable.UUID} = ? AND ${AccountTable.IS_DELETED} = 0
        """.trimIndent()
        return ds.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toAccountModel() else null
                }
            }
        }
    }

    // -------------------------------------------------------
    // INSERT
    // -------------------------------------------------------

    /**
     * 新規アカウントを登録します。
     */
    fun insert(model: AccountModel) {
        val sql = """
            INSERT INTO ${AccountTable.TABLE_NAME} (
                ${AccountTable.UUID}, ${AccountTable.USER_ID}, ${AccountTable.ACCOUNT_NAME},
                ${AccountTable.SLOT_INDEX}, ${AccountTable.IS_ACTIVE}, ${AccountTable.MODE},
                ${AccountTable.CREATED_AT}, ${AccountTable.UPDATED_AT},
                ${AccountTable.CREATED_BY}, ${AccountTable.UPDATED_BY}, ${AccountTable.IS_DELETED}
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ds.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                var i = 1
                stmt.setString(i++, model.uuid.toString())
                stmt.setString(i++, model.userId.toString())
                stmt.setString(i++, model.accountName)
                stmt.setInt(i++,    model.slotIndex)
                stmt.setBoolean(i++, model.isActive)
                stmt.setByte(i++,   model.mode.value)
                stmt.setTimestamp(i++, Timestamp.valueOf(model.createdAt))
                stmt.setTimestamp(i++, Timestamp.valueOf(model.updatedAt))
                stmt.setString(i++, model.createdBy.toString())
                stmt.setString(i++, model.updatedBy.toString())
                stmt.setBoolean(i,  model.isDeleted)
                stmt.executeUpdate()
            }
        }
    }

    // -------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------

    /**
     * 指定プレイヤーの選択中アカウントを切り替えます。
     * 既存の is_active を全て false → 対象を true に設定します。
     */
    fun switchActiveAccount(userId: UUID, targetUuid: UUID, updatedBy: UUID) {
        val now = Timestamp.valueOf(LocalDateTime.now())
        ds.connection.use { conn ->
            // 全アカウントを非アクティブ
            conn.prepareStatement(
                """
                UPDATE ${AccountTable.TABLE_NAME}
                SET ${AccountTable.IS_ACTIVE}  = 0,
                    ${AccountTable.UPDATED_AT} = ?,
                    ${AccountTable.UPDATED_BY} = ?
                WHERE ${AccountTable.USER_ID} = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setTimestamp(1, now)
                stmt.setString(2, updatedBy.toString())
                stmt.setString(3, userId.toString())
                stmt.executeUpdate()
            }
            // 対象アカウントをアクティブ
            conn.prepareStatement(
                """
                UPDATE ${AccountTable.TABLE_NAME}
                SET ${AccountTable.IS_ACTIVE}  = 1,
                    ${AccountTable.UPDATED_AT} = ?,
                    ${AccountTable.UPDATED_BY} = ?
                WHERE ${AccountTable.UUID} = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setTimestamp(1, now)
                stmt.setString(2, updatedBy.toString())
                stmt.setString(3, targetUuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    // -------------------------------------------------------
    // Mapping
    // -------------------------------------------------------

    private fun ResultSet.toAccountModel() = AccountModel(
        uuid        = UUID.fromString(getString(AccountTable.UUID)),
        userId      = UUID.fromString(getString(AccountTable.USER_ID)),
        accountName = getString(AccountTable.ACCOUNT_NAME),
        slotIndex   = getInt(AccountTable.SLOT_INDEX),
        isActive    = getBoolean(AccountTable.IS_ACTIVE),
        mode        = AccountMode.fromValue(getByte(AccountTable.MODE)),
        createdAt   = getTimestamp(AccountTable.CREATED_AT).toLocalDateTime(),
        updatedAt   = getTimestamp(AccountTable.UPDATED_AT).toLocalDateTime(),
        createdBy   = UUID.fromString(getString(AccountTable.CREATED_BY)),
        updatedBy   = UUID.fromString(getString(AccountTable.UPDATED_BY)),
        isDeleted   = getBoolean(AccountTable.IS_DELETED),
    )
}
