#!/usr/bin/env groovy

def flowForCreateEnv = ["gke", "secret"]
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
        def kubeConfigFile = "${HOME}/.kube/config"
        def file = new File(kubeConfigFile)
        if (file.exists()){
            println("Deleting already existing kubeconfig file")
            file.delete()
        }
        sh gkeCreateCmd
    } else if (action == "delete"){
        def gkeDeleteCmd = "gcloud container clusters delete " + params.clusterName + "-" + resource + " --region " +
         params.region + " --quiet"
        sh gkeDeleteCmd
    }
}
/*


def manageSecret(String action, String resource){
    if(action == "create"){
        def createSecretCmd =
    }
}*/

node{
    if (params.action == "create"){
        flowResourceList = flowForCreateEnv
    } else if (params.action == "delete"){
        flowResourceList = flowForDeleteEnv
    }
    for(String resource in flowResourceList){
        stage(params.action + ' ' + resource){
            def methodName = 'manage' + resource.capitalize()
            "$methodName"(params.action, resource)
        }
    }
}