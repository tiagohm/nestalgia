package br.tiagohm.nestalgia.core

class NotificationManager : Disposable {

    private val listeners = ArrayList<NotificationListener>()

    override fun dispose() {
        clear()
    }

    @Synchronized
    fun clear() {
        listeners.clear()
    }

    @Synchronized
    fun registerNotificationListener(listener: NotificationListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    @Synchronized
    fun unregisterNotificationListener(listener: NotificationListener) {
        listeners.remove(listener)
    }

    @Synchronized
    fun sendNotification(type: NotificationType, vararg data: Any?) {
        listeners.forEach { it.processNotification(type, *data) }
    }
}
