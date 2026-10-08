package com.terralink.iotfarm.domain.usecase

import com.terralink.iotfarm.data.network.UdpDiscoveryManager
import com.terralink.iotfarm.domain.repository.FarmRepository
import javax.inject.Inject

class ConnectFarmUseCase @Inject constructor(
    private val repository: FarmRepository,
    private val discoveryManager: UdpDiscoveryManager
) {
    suspend operator fun invoke(ipAddress: String? = null) {
        val targetIp = if (ipAddress.isNull_or_blank()) {
            discoveryManager.discoverFarmIp()
        } else {
            ipAddress
        }

        if (targetIp != null) {
            repository.connect(targetIp)
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()