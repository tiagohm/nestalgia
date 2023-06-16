package br.tiagohm.nestalgia.core

import java.util.concurrent.ThreadFactory

internal object EmulatorThreadFactory : ThreadFactory {

    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r, "Emulator Thread")
        thread.isDaemon = true
        return thread
    }
}
