package io.github.maaasu.astralRecord.feature.account.service;

import io.github.maaasu.astralRecord.feature.account.model.AccountMode;
import io.github.maaasu.astralRecord.feature.account.model.AccountModel;
import io.github.maaasu.astralRecord.feature.account.repository.AccountRepository;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import io.github.maaasu.astralRecord.infrastructure.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * アカウント機能のビジネスロジックを担うサービスクラス。
 * アカウントの作成・選択・一覧取得を管理します。
 */
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * 指定プレイヤーのアカウント一覧を取得します。
     *
     * @param userId プレイヤー UUID
     * @return アカウントモデルのリスト
     */
    public List<AccountModel> getAccounts(UUID userId) {
        return accountRepository.findByUserId(userId);
    }

    /**
     * アカウント UUID でアカウントを取得します。
     *
     * @param accountUuid アカウント UUID
     * @return アカウントモデル、存在しない場合は null
     */
    public AccountModel getAccount(UUID accountUuid) {
        return accountRepository.findByUuid(accountUuid);
    }

    /**
     * 新規アカウントを作成します。
     * スロット番号の重複チェックを行い、問題なければ登録します。
     *
     * @param userId      プレイヤー UUID
     * @param accountName アカウント名（キャラクター名）
     * @param slotIndex   スロット番号（0始まり）
     * @return 作成したアカウントモデル
     * @throws IllegalArgumentException スロット番号が既に使用中の場合
     */
    public AccountModel createAccount(UUID userId, String accountName, int slotIndex) {
        List<AccountModel> existing = accountRepository.findByUserId(userId);
        boolean slotUsed = existing.stream().anyMatch(a -> a.getSlotIndex() == slotIndex);
        if (slotUsed) {
            throw new IllegalArgumentException("Slot " + slotIndex + " is already in use for user: " + userId);
        }

        LocalDateTime now = LocalDateTime.now();
        AccountModel model = new AccountModel(
            UUID.randomUUID(),
            userId,
            accountName,
            slotIndex,
            existing.isEmpty(),     // 初めてのアカウントであれば自動で is_active = true
            AccountMode.PLAYER,
            now,
            now,
            userId,
            userId,
            false
        );
        accountRepository.insert(model);
        Logger.log(LogId.I_5100, accountName, slotIndex, userId);
        return model;
    }

    /**
     * 選択中アカウントを切り替えます。
     *
     * @param userId      プレイヤー UUID
     * @param accountUuid 選択するアカウント UUID
     */
    public void switchAccount(UUID userId, UUID accountUuid) {
        accountRepository.switchActiveAccount(userId, accountUuid, userId);
        Logger.log(LogId.I_5101, accountUuid, userId);
    }
}

