#!/usr/bin/env groovy
node{
    try {
        stageLabel = params.action + " Network"
        stage(stageLabel){
            def networksCmdMap = ["create": "gcloud compute networks create my-vpc --project=" + params.project +
                   " --subnet-mode=custom --bgp-routing-mode=regional",
                   "delete": "gcloud compute networks delete my-vpc --project=" + params.project + " --quiet"]
            sh networksCmdMap[params.action]
        }
    } catch(Exception e){
        println(e)
    }
}