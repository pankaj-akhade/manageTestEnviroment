#!/usr/bin/env groovy
node{
    try {
        stage(params.action + ' Network'){
            def networksCmdMap = ["create": "gcloud compute networks create " +  params.clusterName + "-vpc --project=" +
                   params.project + " --subnet-mode=custom --bgp-routing-mode=regional",
                   "delete": "gcloud compute networks delete " +  params.clusterName + "-vpc --project=" +
                   params.project + " --quiet"]
            sh networksCmdMap[params.action]
        }

        stage(params.action + ' Subnetwork'){
            def networksCmdMap = ["create": "gcloud compute networks subnets create " +  params.clusterName +
                   "-subnet --project=" + params.project + " --range=10.34.0.0/20 --network=" +
                   params.clusterName + "-vpc --region=us-east1",
                   "delete": "gcloud compute networks delete my-vpc --project=" + params.project + " --quiet"]
            sh networksCmdMap[params.action]
        }

    } catch(Exception e){
        println(e)
    }
}