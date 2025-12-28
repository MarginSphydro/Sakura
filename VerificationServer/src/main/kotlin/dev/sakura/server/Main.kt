package dev.sakura.server

import dev.sakura.server.data.UserDatabase
import dev.sakura.server.gui.AdminPanel
import dev.sakura.server.network.VerificationServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main(): Unit = runBlocking {
    UserDatabase.load()

    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        AdminPanel().isVisible = true
    }

    launch(Dispatchers.IO) {
        val server = VerificationServer(54188)
        server.start()
    }.join()
}
