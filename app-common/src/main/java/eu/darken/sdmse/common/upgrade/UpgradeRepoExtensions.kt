package eu.darken.sdmse.common.upgrade

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Entitlement checks are unconditional in this build.
 *
 * Upstream SD Maid SE sold the advanced tools (AppCleaner, Deduplicator, Squeezer, Swiper,
 * scheduling, custom filters, storage trends, config export, the one-tap clean, and a handful of
 * settings) behind a sponsor/Play purchase. The three functions below were the single choke point
 * every one of those gates funnelled through, so they are also the single place where that
 * restriction is lifted: they now report the advanced features as available to everyone.
 *
 * Deliberately kept as real functions with their original signatures rather than having each call
 * site edited. The ~120 call sites are ordinary UI routing and task-submit logic; rewriting them
 * individually would have touched feature code far outside the gate itself, and would have left
 * nothing behind to re-gate if that is ever wanted again.
 *
 * These are local-only decisions. Nothing here fabricates a purchase receipt, calls a store API, or
 * talks to any server — the GPlay billing stack and the FOSS sponsorship record are untouched and
 * still report what they actually know (see `UpgradeRepo.upgradeInfo`, which drives the supporter
 * status and "supporter since" displays).
 */

/** Always available. Formerly: whether the current [UpgradeRepo.Info] reported an active purchase. */
@Suppress("UNUSED_PARAMETER")
suspend fun UpgradeRepo.isPro(): Boolean = true

/**
 * Always available.
 *
 * Formerly the task-submit safety net: it allowed a Pro user through immediately, otherwise nudged
 * a billing refresh and denied only when the result was settled, error-free and still unowned.
 * That deny branch is what refused AppCleaner/Deduplicator submits and config export.
 *
 * [timeout] is retained so existing call sites keep compiling unchanged; there is no longer a
 * reconciliation window to bound.
 */
@Suppress("UNUSED_PARAMETER")
suspend fun UpgradeRepo.isProSettled(timeout: Duration = 5.seconds): Boolean = true

/**
 * Always available.
 *
 * Formerly the UI routing gate: tap handlers asked this before doing the work, and a non-Pro user
 * was sent to the upgrade screen instead. That routing is what made a tap on the gated features
 * open the sponsorship pitch.
 *
 * [timeout] is retained so existing call sites keep compiling unchanged.
 */
@Suppress("UNUSED_PARAMETER")
suspend fun UpgradeRepo.isProForUi(timeout: Duration = 3.seconds): Boolean = true
