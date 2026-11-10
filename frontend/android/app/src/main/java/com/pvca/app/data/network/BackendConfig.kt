package com.pvca.app.data.network

/**
 * Configurable backend connection settings.
 *
 * For physical-device testing on a local network, change BASE_URL to your
 * laptop's LAN IP address (e.g., "http://192.168.1.10:8000").
 *
 * To find your LAN IP on Windows, run: ipconfig
 * Look for "IPv4 Address" under your Wi-Fi adapter.
 */
object BackendConfig {

    /**
     * Base URL of the PVCA FastAPI backend.
     *
     * Default uses 10.0.2.2 (Android emulator → host loopback).
     * For a physical device on the same Wi-Fi network, replace with your
     * laptop's LAN IP address, e.g.: "http://192.168.1.10:8000"
     */
    var BASE_URL: String = "http://10.0.2.2:8000"

    /** Connection timeout in milliseconds. */
    const val CONNECT_TIMEOUT_MS = 3000

    /** Read timeout in milliseconds. */
    const val READ_TIMEOUT_MS = 5000
}
