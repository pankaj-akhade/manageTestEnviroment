#!/usr/bin/env groovy

def flowForCreateEnv = ["mysql"]
def flowForDeleteEnv = ["mysql"]

def manageGke(String action, String resource){
    if(action == "create"){
        def gkeCreateCmd = "gcloud beta container --project \"" + params.project  + "\" clusters create \"" + params.envName +
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
        println("Creating GKE cluster")
        sh gkeCreateCmd
    } else if (action == "delete"){
        def gkeDeleteCmd = "gcloud container clusters delete " + params.envName + "-" + resource + " --region " +
         params.region + " --quiet"
        println("Deleting GKE cluster")
        sh gkeDeleteCmd
    }
}

def getMysqlInstancesList(){
    def getMysqlInstancesListCmd = "gcloud sql instances list --format=\"json(name)\" --filter=\"name:" +
      params.envName + "\""
    return sh(script: getMysqlInstancesListCmd, returnStdout: true)
}

def getMysqlInstanceCount(){
    def getMysqlInstances = getMysqlInstancesList()
    return sh (script: "echo '$getMysqlInstances' | jq '. | length'", returnStdout: true)
}

def getValidMysqlInstanceName(){
    def mysqlInstanceCount = getMysqlInstanceCount().trim()
    if (mysqlInstanceCount.toInteger() == 0){
        if (params.action == "delete"){
            throw new Exception("Did not find any instances")
        } else if (params.action == "create"){
            return params.envName
        }
    } else if (mysqlInstanceCount.toInteger() != 1){
        throw new Exception("Found more than one instances")
    } else {
        def getMysqlInstances = getMysqlInstancesList()
        return sh (script: "echo '$getMysqlInstances' | jq '.[].name'", returnStdout: true)
    }
}

def manageMysql(String action, String resource){
    def instanceName = getValidMysqlInstanceName().trim()
    if(action == "create"){
        //def mysqlDbPostfix = new Date().format("ddMMHHmm")
        def mysqlApiEnableCmd = "gcloud services enable sqladmin.googleapis.com"
        def createMysqlCmd = "gcloud beta sql instances create " + instanceName + "-" + resource + " --database-version " +
        params.mysqlDbVersion + " --region " + params.region + " --network " + params.vpc + " --tier " + params.mysqlDbTier +
        " --storage-size 10 --storage-auto-increase --quiet"
        def createMysqlUserCmd = "gcloud sql users create commander --host=% --instance=" + instanceName + "-" +
          resource + " --password=commander"
        println("Enabling sql admin api")
        sh mysqlApiEnableCmd
        println("Sleeping for 10 seconds")
        sh "sleep 10"
        println("Creating Mysql database")
        sh createMysqlCmd
        //println("Sleeping for 20 seconds")
        //sh "sleep 20"
        println("Deleting root user")
        sh "gcloud sql users delete root --host=% --instance=" + instanceName + "-" + resource + " --quiet"
        println("Creating commander user")
        sh createMysqlUserCmd
    } else if (action == "delete"){
        def mysqlDeleteCmd = "gcloud sql instances delete " + instanceName + " --quiet"
        println("Deleting Mysql database")
        sh mysqlDeleteCmd
    }
}

/*def manageSecret(String action, String resource){
    if(action == "create"){
        def createSecretCmd = "kubectl create secret generic gcp-sql-endpoint --from-literal="
    } else if (params.action == "delete"){

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