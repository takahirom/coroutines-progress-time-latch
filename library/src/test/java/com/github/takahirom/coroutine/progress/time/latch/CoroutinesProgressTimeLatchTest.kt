package com.github.takahirom.coroutine.progress.time.latch

import androidx.lifecycle.*
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Test


class TestLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}

class CoroutinesProgressTimeLatchTest {

    @Test
    fun test() {
        test("normal story") { progressTimeLatch: CoroutinesProgressTimeLatch,
                               visibilityCalls: List<Boolean>
            ->
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(1000)
            launch { progressTimeLatch.refreshLogic(false) }
            advanceTimeBy(1000)

            visibilityCalls shouldBe listOf(false, true, false)
        }

        test("unstable normal story") { progressTimeLatch: CoroutinesProgressTimeLatch,
                                        visibilityCalls: List<Boolean>
            ->
            launch { progressTimeLatch.refreshLogic(true) }
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(1000)
            launch { progressTimeLatch.refreshLogic(false) }
            launch { progressTimeLatch.refreshLogic(false) }
            advanceTimeBy(1000)

            visibilityCalls shouldBe listOf(false, true, false)
        }


        test("immediately load finish") { progressTimeLatch: CoroutinesProgressTimeLatch,
                                          visibilityCalls: List<Boolean>
            ->
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(false) }

            visibilityCalls shouldBe listOf(false)
        }

        test("unstable immediately load finish") { progressTimeLatch: CoroutinesProgressTimeLatch,
                                                   visibilityCalls: List<Boolean>
            ->
            launch { progressTimeLatch.refreshLogic(true) }
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(false) }
            launch { progressTimeLatch.refreshLogic(false) }
            launch { progressTimeLatch.refreshLogic(true) }
            launch { progressTimeLatch.refreshLogic(true) }
            launch { progressTimeLatch.refreshLogic(true) }
            launch { progressTimeLatch.refreshLogic(true) }
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(false) }
            launch { progressTimeLatch.refreshLogic(true) }
            launch { progressTimeLatch.refreshLogic(false) }
            advanceTimeBy(100)
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(false) }
            launch { progressTimeLatch.refreshLogic(false) }
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(false) }

            visibilityCalls shouldBe listOf(false)
        }



        test("not end show when started") { progressTimeLatch: CoroutinesProgressTimeLatch,
                                            visibilityCalls: List<Boolean>
            ->
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(1000)
            launch { progressTimeLatch.refreshLogic(false) }

            visibilityCalls shouldBe listOf(false, true)
        }

        test("unstable not end show when started") { progressTimeLatch: CoroutinesProgressTimeLatch,
                                                     visibilityCalls: List<Boolean>
            ->
            launch { progressTimeLatch.refreshLogic(true) }
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(1000)
            launch { progressTimeLatch.refreshLogic(false) }
            launch { progressTimeLatch.refreshLogic(false) }

            visibilityCalls shouldBe listOf(false, true)
        }


        test("immediately load finish and normal load") { progressTimeLatch: CoroutinesProgressTimeLatch,
                                                          visibilityCalls: List<Boolean>
            ->
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(false) }

            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(1000)
            launch { progressTimeLatch.refreshLogic(false) }
            advanceTimeBy(1000)

            visibilityCalls shouldBe listOf(false, true, false)
        }

        test("reload multiple") { progressTimeLatch: CoroutinesProgressTimeLatch,
                                  visibilityCalls: List<Boolean>
            ->
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(false) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(true) }
            advanceTimeBy(100)
            launch { progressTimeLatch.refreshLogic(true) }

            advanceTimeBy(400)

            launch { progressTimeLatch.refreshLogic(false) }
            launch { progressTimeLatch.refreshLogic(false) }
            advanceTimeBy(10000)

            visibilityCalls shouldBe listOf(false, true, false)
        }
    }

    fun test(
        title: String,
        block:
        (TestCoroutineScope.(
            progressTimeLatch: CoroutinesProgressTimeLatch,
            visibilityCalls: List<Boolean>
        ) -> Unit)
    ) {
        println("start: $title")
        val testCoroutineDispatcher = TestCoroutineDispatcher()
        Dispatchers.setMain(testCoroutineDispatcher)
        testCoroutineDispatcher.runBlockingTest {
            val testLifecycleOwner = TestLifecycleOwner()
            val lifecycleScope: LifecycleCoroutineScope = testLifecycleOwner.lifecycleScope
            val visibilityCalls = mutableListOf<Boolean>()
            val progressTimeLatch =
                CoroutinesProgressTimeLatch(
                    lifecycleScope,
                    currentTimeProvider = { testCoroutineDispatcher.currentTime }) { viewRefreshing: Boolean ->
                    println("${testCoroutineDispatcher.currentTime} changed: $viewRefreshing")
                    visibilityCalls.add(viewRefreshing)
                }

            block(progressTimeLatch, visibilityCalls)
        }
    }

}