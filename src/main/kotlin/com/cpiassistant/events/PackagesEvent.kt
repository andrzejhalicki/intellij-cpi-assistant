package com.cpiassistant.events

import com.cpiassistant.nodes.CpiPackage

data class PackagesEvent(
    val tenantId: String,
    val packages: List<CpiPackage>
)