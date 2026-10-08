package com.terralink.iotfarm.ui.dashboard

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.terralink.iotfarm.data.debug.DebugLogManager
import com.terralink.iotfarm.data.notification.NotificationHelper
import com.terralink.iotfarm.data.network.UdpDiscoveryManager
import com.terralink.iotfarm.domain.model.TelemetryState
import com.terralink.iotfarm.domain.repository.FarmRepository
import com.terralink.iotfarm.domain.usecase.ConnectFarmUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val context: Context = mockk(relaxed = true)
    private val connectFarmUseCase: ConnectFarmUseCase = mockk(relaxed = true)
    private val repository: FarmRepository = mockk(relaxed = true)
    private val notificationHelper: NotificationHelper = mockk(relaxed = true)
    private val udpDiscoveryManager: UdpDiscoveryManager = mockk(relaxed = true)
    private val debugLogManager: DebugLogManager = mockk(relaxed = true)

    private val connectionStatusFlow = MutableStateFlow(false)
    private val telemetryStateFlow = MutableStateFlow(TelemetryState())

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.connectionStatus } returns connectionStatusFlow
        every { repository.telemetryState } returns telemetryStateFlow

        viewModel = DashboardViewModel(
            context,
            connectFarmUseCase,
            repository,
            notificationHelper,
            udpDiscoveryManager,
            debugLogManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleLed sends command and updates state`() = runTest {
        viewModel.toggleLed(true)

        assert(viewModel.isLedOn.value)
        verify { repository.sendCommand("LED:ON") }
    }

    @Test
    fun `togglePump sends command and updates state`() = runTest {
        viewModel.togglePump(true)

        assert(viewModel.isPumpOn.value)
        verify { repository.sendCommand("PUMP:ON") }
    }

    @Test
    fun `updateServoAngle clamps and sends command`() = runTest {
        viewModel.updateServoAngle(200)

        assert(viewModel.servoAngle.value == 180)
        verify { repository.sendCommand("SERVO:180") }
    }

    @Test
    fun `updateLcdText updates text and sends command`() = runTest {
        viewModel.updateLcdText("Hello Farm")

        assert(viewModel.lcdText.value == "Hello Farm")
        verify { repository.sendCommand("LCD:Hello Farm") }
    }
}
