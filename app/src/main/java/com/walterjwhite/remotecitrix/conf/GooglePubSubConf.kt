package com.walterjwhite.remotecitrix.conf

class GooglePubSubConf(
    var project: String,
    var requestTopic: String,
    var requestSubscription: String,
    var statusTopic: String,
    var statusSubscription: String
)
