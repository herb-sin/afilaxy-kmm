package com.afilaxy.domain.model

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getCurrentTimeMillis(): Long {
    // timeIntervalSince1970 retorna Double em segundos; multiplicar por 1000 e converter
    // para Long via toLong() é seguro pois Double tem precisão suficiente para timestamps
    // em milissegundos até o ano 2255 sem overflow de Long (max ~9.2 * 10^18 ms).
    val seconds = NSDate().timeIntervalSince1970
    return (seconds * 1000.0).toLong()
}
