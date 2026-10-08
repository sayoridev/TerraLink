package com.terralink.iotfarm.domain.usecase

import com.terralink.iotfarm.data.network.UdpDiscoveryManager
import com.terralink.iotfarm.domain.repository.FarmRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConnectFarmUseCaseTest {

    private val repository: FarmRepository = mockk(relaxed = true)
    private val discoveryManager: UdpDiscoveryManager = mockk(relaxed = true)
    private val useCase = ConnectFarmUseCase(repository, discoveryManager)

    @Test
    fun `invoke with provided ip calls repository connect directly`() = runTest {
        useCase("192.168.1.50")

        coVerify(exactly = 0) { discoveryManager.discoverFarmIp() }
        coVerify(exactly = 1) { repository.connect("192.168.1.50") }
    }

    @Test
    fun `invoke without ip discovers ip then calls repository connect`() = runTest {
        coEvery { discoveryManager.discoverFarmIp() } returns "192.168.1.100"

        useCase(null)

        coVerify(exactly = 1) { discoveryManager.discoverFarmIp() }
        coVerify(exactly = 1) { repository.connect("192.168.1.100") }
    }
}
