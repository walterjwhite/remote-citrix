package com.walterjwhite.remotecitrix

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.auth.oauth2.GoogleCredentials
import com.walterjwhite.remotecitrix.conf.GooglePubSubConf
import com.walterjwhite.remotecitrix.service.RemoteStatusSubscriber
import com.walterjwhite.remotecitrix.service.TokenPublisher
import com.walterjwhite.remotecitrix.ui.theme.RemoteCitrixTheme
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val credentials = getCredentials(resources, packageName)

        // TODO: do not hardcode this, configure this on installation
        val configuration = GooglePubSubConf(
            PROJECT_NAME,
            TOKEN_TOPIC,
            TOKEN_SUBSCRIPTION,
            STATUS_TOPIC,
            STATUS_SUBSCRIPTION
        )

        // TODO: when terminating the app, this needs to be cleaned up
        val remoteStatusSubscriber = RemoteStatusSubscriber(configuration, credentials)


        // TODO: cleanup
        val tokenPublisher = TokenPublisher(configuration, credentials)

        enableEdgeToEdge()
        setContent {
            RemoteCitrixTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RSA SecurID Token", modifier = Modifier.padding(innerPadding)
                        )
                        val model = viewModel<Model>()
                        remoteStatusSubscriber.model = model

                        remoteStatusSubscriber.startAsync()

                        val token by model.token.collectAsStateWithLifecycle()
                        val status by model.status.collectAsStateWithLifecycle()
                        val error by model.error.collectAsStateWithLifecycle()
                        val waitingFromServer by model.waitingFromServer.collectAsStateWithLifecycle()

                        TextField(value = token,
                            onValueChange = {
                                model.updateToken(it)
                            },
                            label = { Text("Enter token") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                Text(
                                    text = status,
                                )
                            },
                            isError = error
                        )

                        Button(enabled = !error && !waitingFromServer, onClick = {
                            tokenPublisher.sendToken(model)
                        }) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}

fun getCredentials(resources: Resources, packageName: String): GoogleCredentials {
    val credentialResourceId = resources.getIdentifier("credentials", "raw", packageName)
    val jsonCredentials = resources.openRawResource(credentialResourceId)

    try {
        return GoogleCredentials.fromStream(jsonCredentials)
    } finally {
        try {
            jsonCredentials.close()
        } catch (e: IOException) {
            Log.e("Google Credentials", "Error closing input stream", e)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RemoteCitrixTheme {
        Text(
            text = "RSA SecurID Token"
        )
    }
}
