#!/usr/bin/env groovy

def flowForCreateEnv = ["Network", "Subnetwork"]
def flowForDeleteEnv = ["Subnetwork", "Network"]

def manageNetwork(String action){
    def networksCmdMap = ["create": "gcloud compute networks create " +  params.clusterName + "-vpc --project=" +
                   params.project + " --subnet-mode=custom --bgp-routing-mode=regional",
                   "delete": "gcloud compute networks delete " +  params.clusterName + "-vpc --project=" +
                   params.project + " --quiet"]
    sh networksCmdMap[action]
}

def manageSubnetwork(String action){
    def networksCmdMap = ["create": "gcloud compute networks subnets create " +  params.clusterName +
           "-subnet --project=" + params.project + " --range=10.34.0.0/20 --network=" +
           params.clusterName + "-vpc --region=us-east1",
           "delete": "gcloud compute networks delete " +  params.clusterName + "-subnet --project=" +
           params.project + " --quiet"]
    sh networksCmdMap[action]
}

node{
    //try {
    if (params.action == "create"){
        for(String resource in flowForCreateEnv){
            stage(params.action + ' ' + resource){
                def methodName = 'manage' + resource
                "$methodName"(params.action)
            }
        }
    } else if (params.action == "delete"){
        for(String resource in flowForDeleteEnv){
            stage(params.action + ' ' + resource){
                def methodName = 'manage' + resource
                "$methodName"(params.action)
            }
        }
    }
    /*} catch(Exception e){
        println(e)
    }*/
}