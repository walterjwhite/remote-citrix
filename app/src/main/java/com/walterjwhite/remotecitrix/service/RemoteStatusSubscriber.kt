package com.walterjwhite.remotecitrix.service

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import com.google.api.core.ApiService
import com.google.api.core.ApiService.Listener
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.common.util.concurrent.MoreExecutors
import com.google.pubsub.v1.SubscriptionName
import com.walterjwhite.remotecitrix.Model
import com.walterjwhite.remotecitrix.conf.GooglePubSubConf

class RemoteStatusSubscriber(
    configuration: GooglePubSubConf,
    credentials: GoogleCredentials
) {
    lateinit var model: Model
    private var subscriber = buildSubscriber(configuration, credentials)
    private var started = false

    private fun buildSubscriber(configuration: GooglePubSubConf,
                                credentials: GoogleCredentials): Subscriber {
        val subscription =
            SubscriptionName.of(configuration.project, configuration.statusSubscription)

        val subscriber = Subscriber.newBuilder(subscription.toString(), buildReceiver()).setCredentialsProvider{credentials}.build()
        subscriber.addListener(buildListener(), MoreExecutors.directExecutor())

        return subscriber
    }

    private fun buildReceiver(): MessageReceiver {
        return MessageReceiver { message, consumer ->
            consumer.ack()

            val messageParts = message.data.toStringUtf8().replace("\"", "").split("|")

            model._status.value = messageParts[0]
            model._error.value = !messageParts[1].toBoolean()

            model._waitingFromServer.value = false
        }
    }

    private fun buildListener(): Listener {
        return object : Listener() {
            override fun failed(from: ApiService.State, failure: Throwable) {
                model._status.value = "Failed: " + failure.message
                model._error.value = true
                model._waitingFromServer.value = false
            }
        }
    }

    fun startAsync() {
        if(!started) {
            subscriber.startAsync()
            started = true
        }
    }

    fun stopAsync() {
        if(started) {
            subscriber.stopAsync()
            started = false
        }
    }
}