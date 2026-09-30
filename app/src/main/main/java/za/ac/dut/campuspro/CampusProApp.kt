package za.ac.dut.campuspro

import android.app.Application
import za.ac.dut.campuspro.data.CampusRepository
import za.ac.dut.campuspro.notifications.NotificationHelper

/** Application class: creates the notification channel and the shared repository. */
class CampusProApp : Application() {

    lateinit var repository: CampusRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val notifications = NotificationHelper(this)
        notifications.createChannel()
        repository = CampusRepository(this, notifications)
    }
}
