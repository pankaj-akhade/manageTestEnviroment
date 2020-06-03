#!/usr/bin/env groovy

def flowForCreateEnv = ["vpc", "subnet"]
def flowForDeleteEnv = ["subnet", "vpc"]

def manageVpc(String action, String resource){
    def networksCmdMap = ["create": "gcloud compute networks create " +  params.clusterName + "-" + resource +
                   " --project=" + params.project + " --subnet-mode=custom --bgp-routing-mode=regional",
                   "delete": "gcloud compute networks delete " +  params.clusterName + "-" + resource + " --project=" +
                   params.project + " --quiet"]
    sh networksCmdMap[action]
}

def manageSubnet(String action, String resource){
    def networksCmdMap = ["create": "gcloud compute networks subnets create " +  params.clusterName + "-" + resource +
           " --project=" + params.project + " --range=10.34.0.0/20 --network=" +
           params.clusterName + "-vpc --region=us-east1",
           "delete": "gcloud compute networks subnets delete " +  params.clusterName + "-" + resource + " --project=" +
           params.project + " --region=us-east1 --quiet"]
    sh networksCmdMap[action]
}

node{
    if (params.action == "create"){
        for(String resource in flowForCreateEnv){
            stage(params.action + ' ' + resource){
                def methodName = 'manage' + resource.capitalize()
                "$methodName"(params.action, resource)
            }
        }
    } else if (params.action == "delete"){
        for(String resource in flowForDeleteEnv){
            stage(params.action + ' ' + resource){
                def methodName = 'manage' + resource.capitalize()
                "$methodName"(params.action, resource)
            }
        }
    }
}