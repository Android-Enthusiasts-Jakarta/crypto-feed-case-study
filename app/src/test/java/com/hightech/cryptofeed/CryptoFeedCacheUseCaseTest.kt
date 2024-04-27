package com.hightech.cryptofeed

import app.cash.turbine.test
import com.hightech.cryptofeed.cache.CryptoFeedCacheUseCase
import com.hightech.cryptofeed.cache.CryptoFeedStore
import com.hightech.cryptofeed.domain.CoinInfo
import com.hightech.cryptofeed.domain.CryptoFeed
import com.hightech.cryptofeed.domain.Raw
import com.hightech.cryptofeed.domain.Usd
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

class CryptoFeedCacheUseCaseTest {
    private val store = spyk<CryptoFeedStore>()
    private lateinit var sut: CryptoFeedCacheUseCase

    private val feeds = listOf(uniqueCryptoFeed(), uniqueCryptoFeed())

    private val timestamp = Date()

    @Before
    fun setUp() {
        sut = CryptoFeedCacheUseCase(store = store, timestamp)
    }

    @Test
    fun testInitDoesNotDeletionCacheUponCreation() = runBlocking {
        verify(exactly = 0) {
            store.deleteCache()
        }

        confirmVerified(store)
    }

    @Test
    fun testSaveRequestsCacheDeletion() = runBlocking {
        every {
            store.deleteCache()
        } returns flowOf()

        sut.save(feeds).test {
            awaitComplete()
        }

        verify(exactly = 1) {
            store.deleteCache()
        }

        confirmVerified(store)
    }

    @Test
    fun testSaveDoestNotRequestsCacheInsertionOnDeletionError() = runBlocking {
        every {
            store.deleteCache()
        } returns flowOf(Exception())

        sut.save(feeds).test {
            awaitComplete()
        }

        verify(exactly = 1) {
            store.deleteCache()
        }

        verify(exactly = 0) {
            store.insert(feeds, timestamp)
        }

        confirmVerified(store)
    }

    @Test
    fun testSaveRequestsNewCacheInsertionWithTimestampOnSuccessfulDeletion() = runBlocking {
        val captureFeed = slot<List<CryptoFeed>>()
        val captureTimeStamp = slot<Date>()

        every {
            store.deleteCache()
        } returns flowOf(null)

        every {
            store.insert(capture(captureFeed), capture(captureTimeStamp))
        } returns flowOf()

        sut.save(feeds).test {
            assertEquals(feeds, captureFeed.captured)
            assertEquals(timestamp, captureTimeStamp.captured)
            awaitComplete()
        }

        verify(exactly = 1) {
            store.deleteCache()
        }

        verify(exactly = 1) {
            store.insert(feeds, timestamp)
        }

        confirmVerified(store)
    }

    private fun uniqueCryptoFeed(): CryptoFeed {
        return CryptoFeed(
            CoinInfo(
                UUID.randomUUID().toString(),
                "any",
                "any",
                "any-url"
            ),
            Raw(
                Usd(
                    1.0,
                    1F,
                )
            )
        )
    }
}