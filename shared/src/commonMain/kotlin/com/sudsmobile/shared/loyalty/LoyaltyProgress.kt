package com.sudsmobile.shared.loyalty

data class LoyaltyProgress(
    val totalWashes: Int,
    val currentWashes: Int,
    val targetWashes: Int,
    val remainingWashes: Int,
    val progress: Float,
    val rewardReady: Boolean,
    val completedRewards: Int,
) {
    val completedWashes: Int
        get() = currentWashes
}

fun Int.toLoyaltyProgress(rewardInterval: Int = DefaultRewardInterval): LoyaltyProgress {
    val target = rewardInterval.coerceAtLeast(1)
    val total = coerceAtLeast(0)
    val currentCycle = total % target
    val rewardReady = total > 0 && currentCycle == 0
    val currentWashes = if (rewardReady) target else currentCycle

    return LoyaltyProgress(
        totalWashes = total,
        currentWashes = currentWashes,
        targetWashes = target,
        remainingWashes = if (rewardReady) 0 else target - currentWashes,
        progress = currentWashes.toFloat() / target.toFloat(),
        rewardReady = rewardReady,
        completedRewards = total / target,
    )
}

const val DefaultRewardInterval: Int = 10
