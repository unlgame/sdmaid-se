package eu.darken.sdmse.common.upgrade

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import testhelpers.BaseTest
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Locked-in contract for this build: the advanced features are available to every user, whatever
 * [UpgradeRepo.upgradeInfo] reports.
 *
 * These three functions used to be the gate that AppCleaner, Deduplicator, the config export and the
 * one-tap clean were sold behind, so they are the natural place to assert that the restriction is
 * gone — including for the states that used to deny (settled non-Pro) and the ones that used to be
 * undecided (still connecting, errored). A future change that reintroduces a deny branch here will
 * fail this file rather than silently re-gating the tools.
 *
 * The `timeout` parameters are retained by the production signatures so existing call sites keep
 * compiling; they no longer bound anything, so they are exercised here rather than asserted on.
 */
class UpgradeRepoExtensionsTest : BaseTest() {

    private class FakeInfo(
        override val isPro: Boolean,
        override val isSettled: Boolean,
        override val error: Throwable? = null,
    ) : UpgradeRepo.Info {
        override val type: UpgradeRepo.Type = UpgradeRepo.Type.FOSS
        override val upgradedAt: Instant? = null
    }

    private class FakeRepo(
        pro: Boolean,
        settled: Boolean,
        error: Throwable? = null,
    ) : UpgradeRepo {
        val infoFlow = MutableStateFlow<UpgradeRepo.Info>(FakeInfo(pro, settled, error))
        var refreshCalls = 0

        override val storeSite: String = ""
        override val upgradeSite: String = ""
        override val betaSite: String = ""
        override val upgradeInfo: Flow<UpgradeRepo.Info> = infoFlow
        override suspend fun refresh() {
            refreshCalls++
        }
    }

    private fun repoThrowingOnRead() = object : UpgradeRepo {
        override val storeSite: String = ""
        override val upgradeSite: String = ""
        override val betaSite: String = ""
        override val upgradeInfo: Flow<UpgradeRepo.Info> get() = flow { throw IllegalStateException("billing exploded") }
        override suspend fun refresh() = Unit
    }

    @Test
    fun `isPro is available for a settled non-pro repo`() = runTest {
        FakeRepo(pro = false, settled = true).isPro() shouldBe true
    }

    @Test
    fun `isPro is available for a pro repo`() = runTest {
        FakeRepo(pro = true, settled = true).isPro() shouldBe true
    }

    @Test
    fun `isPro is available while the entitlement is still undecided`() = runTest {
        FakeRepo(pro = false, settled = false).isPro() shouldBe true
    }

    @Test
    fun `isPro is available when the entitlement read fails`() = runTest {
        repoThrowingOnRead().isPro() shouldBe true
    }

    @Test
    fun `isProSettled is available for a settled non-pro repo`() = runTest {
        val repo = FakeRepo(pro = false, settled = true)

        repo.isProSettled() shouldBe true
        // No reconciliation round-trip is needed to reach the answer any more.
        repo.refreshCalls shouldBe 0
    }

    @Test
    fun `isProSettled is available for a pro repo`() = runTest {
        FakeRepo(pro = true, settled = false).isProSettled() shouldBe true
    }

    @Test
    fun `isProSettled is available with an explicit timeout`() = runTest {
        FakeRepo(pro = false, settled = true).isProSettled(timeout = 2.seconds) shouldBe true
    }

    @Test
    fun `isProSettled is available when the entitlement read fails`() = runTest {
        repoThrowingOnRead().isProSettled() shouldBe true
    }

    @Test
    fun `isProForUi is available for a settled non-pro repo`() = runTest {
        FakeRepo(pro = false, settled = true).isProForUi() shouldBe true
    }

    @Test
    fun `isProForUi is available for a pro repo`() = runTest {
        FakeRepo(pro = true, settled = false).isProForUi() shouldBe true
    }

    @Test
    fun `isProForUi is available with an explicit timeout`() = runTest {
        FakeRepo(pro = false, settled = false).isProForUi(timeout = 2.seconds) shouldBe true
    }

    @Test
    fun `isProForUi is available when the entitlement read fails`() = runTest {
        repoThrowingOnRead().isProForUi() shouldBe true
    }
}
