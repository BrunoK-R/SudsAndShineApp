package com.sudsmobile.shared.loyalty

data class LoyaltyProgress(
    val totalWashes: Int,
    val currentWashes: Int,
    val targetWashes: Int,
    val remainingWashes: Int,
    val progress: Float,
    val rewardReady: Boolean,
    val completedRewards: Int,
    val claimedRewards: Int,
    val availableRewards: Int,
) {
    val completedWashes: Int
        get() = currentWashes
}

fun Int.toLoyaltyProgress(
    rewardInterval: Int = DefaultRewardInterval,
    claimedRewards: Int = 0,
): LoyaltyProgress {
    val target = rewardInterval.coerceAtLeast(1)
    val total = coerceAtLeast(0)
    val completedRewards = total / target
    val claimed = claimedRewards.coerceAtLeast(0)
    val availableRewards = (completedRewards - claimed).coerceAtLeast(0)
    val currentCycle = total % target
    val rewardReady = availableRewards > 0
    val currentWashes = if (rewardReady) target else currentCycle

    return LoyaltyProgress(
        totalWashes = total,
        currentWashes = currentWashes,
        targetWashes = target,
        remainingWashes = if (rewardReady) 0 else target - currentWashes,
        progress = currentWashes.toFloat() / target.toFloat(),
        rewardReady = rewardReady,
        completedRewards = completedRewards,
        claimedRewards = claimed,
        availableRewards = availableRewards,
    )
}

const val DefaultRewardInterval: Int = 10
