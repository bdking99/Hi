package com.example.data.model

// 🪙 USER WALLET - Stored in Firestore `wallets/{uid}`
data class UserWallet(
    val uid: String = "",
    val publicUserId: String = "",
    val coinBalance: Long = 2500L,
    val lifetimePurchasedCoins: Long = 0L,
    val lifetimeReceivedCoins: Long = 0L,
    val lifetimeSpentCoins: Long = 0L,
    val lifetimeGiftValue: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 💳 WALLET TRANSACTION RECORD - Stored in Firestore `walletTransactions/{transactionId}`
data class WalletTransaction(
    val transactionId: String = "",
    val uid: String = "",
    val publicUserId: String = "",
    val type: String = "COIN_RECHARGE", // COIN_PURCHASE, COIN_RECHARGE, GIFT_SENT, GIFT_RECEIVED, ADMIN_ADJUSTMENT, REFUND, BONUS, PROMOTIONAL_REWARD, FRAME_PURCHASE
    val amount: Long = 0L,
    val balanceBefore: Long = 0L,
    val balanceAfter: Long = 0L,
    val referenceId: String = "",
    val status: String = "SUCCESS", // SUCCESS, PENDING, FAILED
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val serverTimestamp: Long = System.currentTimeMillis()
)

// 🎁 CATALOG GIFT - Stored in Firestore `gifts/{giftId}`
data class CatalogGift(
    val giftId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val animationUrl: String = "",
    val previewUrl: String = "",
    val coinPrice: Long = 100L,
    val category: String = "Classic", // Classic, Romantic, Luxury, VIP, Super VIP
    val emoji: String = "🎁",
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val isFullScreenEffect: Boolean = false,
    val soundUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 💸 GIFT TRANSACTION RECORD - Stored in Firestore `giftTransactions/{giftTransactionId}`
data class GiftTransactionRecord(
    val giftTransactionId: String = "",
    val operationId: String = "",
    val senderUid: String = "",
    val senderPublicUserId: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val recipientUid: String = "",
    val recipientPublicUserId: String = "",
    val recipientDisplayName: String = "",
    val recipientAvatarUrl: String = "",
    val giftId: String = "",
    val giftName: String = "",
    val giftEmoji: String = "🎁",
    val giftImageUrl: String = "",
    val coinPrice: Long = 0L,
    val quantity: Int = 1,
    val totalCoins: Long = 0L,
    val roomId: String? = null,
    val roomTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS"
)

// 👑 PROFILE FRAME ITEM - Stored in Firestore `profileFrames/{frameId}`
data class ProfileFrameItem(
    val frameId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val animationUrl: String = "",
    val previewUrl: String = "",
    val type: String = "VIP", // "VIP", "LUXURY", "EVENT", "DEFAULT"
    val rarity: String = "Common", // "Common", "Rare", "Epic", "Legendary"
    val requiredCoins: Long = 0L,
    val requiredVipLevel: Int = 0,
    val durationType: String = "PERMANENT", // "PERMANENT", "DAYS"
    val durationDays: Int = 0,
    val glowColorHex: Long = 0xFFFFD700,
    val secondaryColorHex: Long = 0xFFFFA000,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 🛡️ USER OWNED FRAME - Stored in Firestore `userFrames/{uid}/owned/{frameId}`
data class OwnedFrame(
    val frameId: String = "",
    val uid: String = "",
    val acquiredAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val source: String = "PURCHASE", // PURCHASE, VIP_REWARD, ADMIN_GRANT, EVENT_REWARD, PROMOTION, DEFAULT
    val isEquipped: Boolean = false
)

// 📜 AUDIT LOG - Stored in Firestore `auditLogs/{logId}`
data class AuditLogRecord(
    val logId: String = "",
    val actorUid: String = "",
    val actorRole: String = "USER", // USER, ADMIN, SYSTEM
    val action: String = "", // COIN_ADJUSTMENT, GIFT_SENT, FRAME_PURCHASED, FRAME_GRANTED, FRAME_REMOVED, REFUND, ADMIN_ACTION
    val targetUid: String = "",
    val referenceId: String = "",
    val metadata: Map<String, Any?> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

sealed class GiftSendResult {
    data class Success(val transactionRecord: GiftTransactionRecord, val newSenderBalance: Long) : GiftSendResult()
    data class Failure(val errorMessage: String) : GiftSendResult()
}

sealed class FramePurchaseResult {
    data class Success(val frame: ProfileFrameItem, val newBalance: Long) : FramePurchaseResult()
    data class Failure(val errorMessage: String) : FramePurchaseResult()
}

// 📦 COIN PURCHASE PACKAGE - Stored in Firestore `coinPackages/{packageId}`
data class CoinPackage(
    val packageId: String = "",
    val coinAmount: Long = 100000L,
    val bonusCoins: Long = 0L,
    val priceBDT: Double = 550.0,
    val currency: String = "BDT",
    val isActive: Boolean = true,
    val sortOrder: Int = 1,
    val popularBadge: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalCoins: Long get() = coinAmount + bonusCoins
}

// 🇧🇩 COIN PURCHASE REQUEST - Stored in Firestore `coinPurchaseRequests/{requestId}`
data class CoinPurchaseRequest(
    val requestId: String = "",
    val uid: String = "",
    val publicUserId: String = "",
    val packageId: String = "",
    val coinAmount: Long = 0L,
    val bonusCoins: Long = 0L,
    val totalCoins: Long = 0L,
    val priceBDT: Double = 0.0,
    val paymentMethod: String = "bKash", // "bKash", "Nagad", "Rocket", "Bank"
    val paymentReference: String = "", // User-submitted TrxID
    val senderAccountPhone: String = "",
    val status: String = "PENDING", // PENDING, VERIFYING, APPROVED, REJECTED, EXPIRED, CANCELLED
    val rejectReason: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 💸 WITHDRAWAL REQUEST - Stored in Firestore `withdrawalRequests/{requestId}`
data class WithdrawalRequest(
    val requestId: String = "",
    val uid: String = "",
    val publicUserId: String = "",
    val requestedCoins: Long = 0L,
    val exchangeRate: Double = 0.0055, // 1 coin = 0.0055 BDT (100,000 coins = 550 BDT)
    val withdrawalAmountBDT: Double = 0.0,
    val paymentMethod: String = "bKash", // bKash, Nagad
    val accountNumber: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, PROCESSING, PAID, REJECTED, CANCELLED
    val rejectReason: String? = null,
    val transactionRef: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 🔄 COIN EXCHANGE REQUEST - Stored in Firestore `coinExchangeRequests/{exchangeId}`
data class CoinExchangeRequest(
    val exchangeId: String = "",
    val uid: String = "",
    val publicUserId: String = "",
    val fromCoins: Long = 0L,
    val toDiamonds: Long = 0L,
    val exchangeRate: Double = 1.0,
    val status: String = "SUCCESS",
    val createdAt: Long = System.currentTimeMillis()
)

// ⚙️ PAYMENT & WALLET CONFIG - Stored in Firestore `system_settings/payment_config`
data class PaymentSystemConfig(
    val bkashNumber: String = "01700000000",
    val bkashAccountType: String = "Personal (Send Money)",
    val bkashInstructions: String = "bKash অ্যাপ থেকে Send Money করুন। রেফারেন্সে আপনার Public User ID দিন এবং পেমেন্টের পর প্রাপ্ত ট্রানজেকশন আইডি (TrxID) নিচে লিখে সাবমিট করুন।",
    val nagadNumber: String = "01900000000",
    val nagadAccountType: String = "Personal (Send Money)",
    val nagadInstructions: String = "Nagad অ্যাপ থেকে Send Money করুন এবং ট্রানজেকশন আইডি নিচে সাবমিট করুন।",
    val rocketNumber: String = "01800000000",
    val upayNumber: String = "01600000000",
    val manualApprovalEnabled: Boolean = true,
    val coinExchangeRateBDT: Double = 0.0055, // 100,000 Coins = 550 BDT
    val minWithdrawalCoins: Long = 50000L,
    val isAutoPaymentApiConfigured: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
