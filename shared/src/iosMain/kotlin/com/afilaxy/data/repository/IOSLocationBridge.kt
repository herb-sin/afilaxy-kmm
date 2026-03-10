package com.afilaxy.data.repository

/**
 * Bridge unidirecional: Swift escreve aqui, Kotlin lê.
 * Resolve a limitação de KMM onde iosMain não pode importar código Swift.
 * Swift acessa via: IOSLocationBridge.shared.latitude = ...
 */
object IOSLocationBridge {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var hasPermission: Boolean = false
}
