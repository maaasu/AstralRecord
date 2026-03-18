package io.github.maaasu.astralRecord.feature.account.repository

/**
 * dbo.account テーブルのカラム名定数。
 */
internal object AccountTable {
    const val TABLE_NAME    = "[dbo].[account]"
    const val UUID          = "uuid"
    const val USER_ID       = "user_id"
    const val ACCOUNT_NAME  = "account_name"
    const val SLOT_INDEX    = "slot_index"
    const val IS_ACTIVE     = "is_active"
    const val MODE          = "mode"
    const val CREATED_AT    = "created_at"
    const val UPDATED_AT    = "updated_at"
    const val CREATED_BY    = "created_by"
    const val UPDATED_BY    = "updated_by"
    const val IS_DELETED    = "is_deleted"
}
