package com.walterjwhite.remotecitrix.service

import android.util.Log
import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.TopicAdminClient
import com.google.cloud.pubsub.v1.TopicAdminSettings
import com.google.common.util.concurrent.MoreExecutors
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import com.walterjwhite.remotecitrix.Model
import com.walterjwhite.remotecitrix.conf.GooglePubSubConf
import java.io.Closeable
import java.util.concurrent.TimeUnit


class TokenPublisher(
    configuration: GooglePubSubConf,
    credentials: GoogleCredentials
) : Closeable {

    private var topicAdminSettings: TopicAdminSettings = TopicAdminSettings.newBuilder()
        .setCredentialsProvider { credentials }.build()
    private var publisher: Publisher

    init {
        createTopic(topicAdminSettings, configuration.requestTopic)
        publisher = getPublisher(credentials, configuration.requestTopic)
    }

    fun sendToken(model: Model) {
        publish(
            model = model,
            contents = "publishing token",
            successful = true,
            waitingFromServer = true
        )

        try {
            val data = ByteString.copyFromUtf8("\"" + model.token.value + "\"")

            val pubsubMessage = PubsubMessage.newBuilder().setData(data).build()
            val messageIdFuture: ApiFuture<String> = publisher.publish(pubsubMessage)
            ApiFutures.addCallback(messageIdFuture, object : ApiFutureCallback<String> {
                override fun onSuccess(messageId: String) {
                    publish(
                        model = model,
                        contents = "published",
                        successful = true,
                        waitingFromServer = false
                    )
                }

                override fun onFailure(t: Throwable) {
                    Log.e("publishing token", "failed to published token: ", t)
                    publish(
                        model = model,
                        contents = "failed to publish token: " + t.message,
                        successful = false,
                        waitingFromServer = false
                    )
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.e("publishing token", e.message, e)
            publish(
                model = model,
                contents = "error publishing token: " + e.message,
                successful = true,
                waitingFromServer = true
            )
        }
    }

    private fun createTopic(settings: TopicAdminSettings, topicName: String) {
        TopicAdminClient.create(settings).use { topicAdminClient ->
            topicAdminClient.getTopic(topicName)
        }
    }

    private fun getPublisher(credentials: GoogleCredentials, topicName: String): Publisher {
        return Publisher.newBuilder(topicName).setCredentialsProvider { credentials }.build()
    }

    override fun close() {
        publisher.shutdown()
        publisher.awaitTermination(1, TimeUnit.MINUTES)
    }

    fun publish(model: Model, contents: String, successful: Boolean, waitingFromServer: Boolean) {
        model._error.value = !successful
        model._status.value = contents
        model._waitingFromServer.value = waitingFromServer

        if(successful) {
            Log.i("status", contents)
        } else {
            Log.e("status", contents)
        }
    }
}