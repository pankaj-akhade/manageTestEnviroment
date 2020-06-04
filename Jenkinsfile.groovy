#!/usr/bin/env groovy

def flowForCreateEnv = ["gke"]
def flowForDeleteEnv = ["gke"]

def manageGke(String action, String resource){
    if(action == "create"){
        def gkeCreateCmd = "gcloud beta container --project \"" + params.project  + "\" clusters create \"" + params.clusterName +
         "-" + resource + "\" --region \"" + params.region + "\" --no-enable-basic-auth --cluster-version \"" + params.clusterVersion +
         "\" --machine-type \"" + params.instanceType + "\" --image-type \"UBUNTU\" --disk-size \"20\" --service-account \"" +
         params.serviceAccount + "\" --num-nodes \"1\" --enable-stackdriver-kubernetes --enable-ip-alias --network " +
         "\"projects/flow-on-k8s-test/global/networks/" + params.vpc + "\" --subnetwork \"projects/flow-on-k8s-test/regions/" +
         params.region + "/subnetworks/" + params.subnet + "\" --enable-autoscaling --min-nodes \"1\" --max-nodes \"3\" " +
         "--no-enable-master-authorized-networks --addons HorizontalPodAutoscaling,HttpLoadBalancing --no-enable-autoupgrade " +
         "--enable-autorepair --max-surge-upgrade 1 --max-unavailable-upgrade 0 --scopes \"https://www.googleapis.com/auth/ndev.clouddns.readwrite\""
        sh gkeCreateCmd
    } else if (action == "delete"){
        def gkeDeleteCmd = "gcloud container clusters delete " + params.clusterName + "-" + resource + " --region " +
         params.region + " --quiet"
        sh gkeDeleteCmd
    }
}

node{
    flowResourceList = 'flowFor' + params.action.capitalize() + 'Env'
    for (String resource in 'flowFor' + params.action.capitalize() + 'Env'){
        stage(params.action + ' ' + resource){
            def methodName = 'manage' + resource.capitalize()
            "$methodName"(params.action, resource)
        }
    }/*
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
    }*/
}