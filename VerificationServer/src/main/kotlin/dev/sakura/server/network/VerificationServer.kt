package dev.sakura.server.network

import kotlinx.coroutines.*
import java.net.ServerSocket

class VerificationServer(private val port: Int) {
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun start() = withContext(Dispatchers.IO) {
        serverSocket = ServerSocket(port)
        println("[Server] Started on port $port")
        println("[Server] Waiting for connections...")

        while (true) {
            runCatching {
                val clientSocket = serverSocket!!.accept()
                scope.launch {
                    ClientHandler(clientSocket).handle()
                }
            }.onFailure {
                println("[Server] Error accepting connection: ${it.message}")
            }
        }
    }

    fun stop() {
        scope.cancel()
        serverSocket?.close()
        println("[Server] Stopped")
    }
}
