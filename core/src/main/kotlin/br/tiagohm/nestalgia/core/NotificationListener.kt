package br.tiagohm.nestalgia.core

interface NotificationListener {

    fun processNotification(type: NotificationType, vararg data: Any?)
}
