package io.github.maaasu.astralRecord.feature.player.model

import io.github.maaasu.astralRecord.feature.account.model.AccountModel
import io.github.maaasu.astralRecord.feature.player.PlayerMsgId
import io.github.maaasu.astralRecord.feature.player.PlayerMsgResource
import io.github.maaasu.astralRecord.feature.user.model.UserModel
import io.github.maaasu.astralRecord.infrastructure.logging.LogId
import io.github.maaasu.astralRecord.infrastructure.logging.Logger
import org.bukkit.entity.Player

/**
 * AstralRecord プロジェクト独自のプレイヤーモデル。
 * Bukkit の [Player] と区別するため `Ast` プレフィックスを付与しています。
 *
 * プレイヤー一人に紐づく各種データを一元的に保持します。
 *
 * @property bukkit   Bukkit プレイヤーインスタンス
 * @property user     dbo.user に対応するユーザーデータ（permission 変更時は [applyPermission] 経由で更新すること）
 * @property account  現在アクティブな dbo.account に対応するアカウントデータ
 */
data class AstPlayer(
    val bukkit: Player,
    var user: UserModel,
    val account: AccountModel,
) {
    init {
        applyPermission(user)
    }

    companion object {
        /** この値以上の permission を持つプレイヤーに Minecraft OP 権限を付与する */
        const val OP_PERMISSION_THRESHOLD = 99
    }

    /**
     * [UserModel] の permission を新しい値で更新し、Minecraft OP 権限を同期します。
     * <p>
     * permission >= [OP_PERMISSION_THRESHOLD] の場合は [bukkit].setOp(true)、
     * それ以外の場合は [bukkit].setOp(false) を設定します。
     *
     * @param newUser permission が更新された新しい [UserModel]
     */
    fun applyPermission(newUser: UserModel) {
        user = newUser
        if (newUser.permission >= OP_PERMISSION_THRESHOLD) {
            bukkit.isOp = true
            Logger.log(LogId.I_5070, bukkit.name, newUser.permission)
            sendMessage(PlayerMsgId.P_5070, newUser.permission)
        } else {
            bukkit.isOp = false
        }
    }

    /**
     * プレイヤーにメッセージを送信します。
     * メッセージ文言は [PlayerMsgId] 経由で [PlayerMsgResource] から取得されます。
     *
     * @param msgId メッセージID
     * @param args  フォーマット引数（省略可）
     */
    fun sendMessage(msgId: PlayerMsgId, vararg args: Any) {
        if (!bukkit.isOnline) return
        val message = PlayerMsgResource.format(msgId.id, *args)
        bukkit.sendMessage(message)
    }
}
