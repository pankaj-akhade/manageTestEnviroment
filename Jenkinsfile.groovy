#!/usr/bin/env groovy

def flowForCreateEnv = ["mysql","filestore","gke","gsbucket","secret","extdns","nfs"]
def flowForDeleteEnv = ["mysql","filestore","gke","gsbucket"]

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

def getDbInstancesList(){
    def getDbInstancesListCmd = "gcloud sql instances list --format=\"json(name)\" --filter=\"name:" +
      params.envName + "\""
    return sh(script: getDbInstancesListCmd, returnStdout: true)
}

def getDbInstanceCount(){
    def getDbInstances = getDbInstancesList()
    return sh (script: "echo '$getDbInstances' | jq '. | length'", returnStdout: true)
}

def getValidDbInstanceName(){
    def dbInstanceCount = getDbInstanceCount().trim()
    if (dbInstanceCount.toInteger() == 0){
        if (params.action == "delete"){
            throw new Exception("Did not find any instances")
        } else if (params.action == "create"){
            return params.envName
        }
    } else if (dbInstanceCount.toInteger() != 1){
        throw new Exception("Found more than one instances")
    } else {
        def getDbInstances = getDBInstancesList()
        return sh (script: "echo '$getDbInstances' | jq '.[].name'", returnStdout: true)
    }
}

def manageMysql(String action, String resource){
    def instanceName = getValidDbInstanceName().trim()
    if(action == "create"){
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

def manageFilestore(String action, String resource){
    if(action == "create"){
        def createFilestoreCmd = "gcloud filestore instances create " + params.envName + "-" + resource + " --project=" +
          params.project + " --zone=" + params.region + "-b --tier=STANDARD --file-share=name=\"filestore\",capacity=1TB" +
          " --network=name=\"" + params.vpc + "\""
        println("Creating Filestore instance")
        sh createFilestoreCmd
    } else if (action == "delete"){
        def filestoreDeleteCmd = "gcloud filestore instances delete " + params.envName + "-" + resource + " --project=" +
          params.project + " --zone=" + params.region + "-b --quiet"
        println("Deleting Filestore instance")
        sh filestoreDeleteCmd
    }
}

def manageMssql(String action, String resource){
    def instanceName = getValidMysqlInstanceName().trim()
    if(action == "create"){
        def mssqlApiEnableCmd = "gcloud services enable sqladmin.googleapis.com"
        def createMssqlCmd = "gcloud beta sql instances create " + instanceName + "-" + resource + " --database-version=" +
        params.mssqlDbVersion + " --root-password=admincommander --region=" + params.region + " --network=" + params.vpc + " --tier=" + params.mssqlDbTier +
        " --storage-size=10 --storage-auto-increase --quiet"
        def createMssqlUserCmd = "gcloud sql users create commander --instance=" + instanceName + "-" +
          resource + " --password=commander"
        println("Enabling sql admin api")
        sh mssqlApiEnableCmd
        println("Sleeping for 10 seconds")
        sh "sleep 10"
        println("Creating Mssql database")
        sh createMssqlCmd
        println("Creating commander user")
        sh createMssqlUserCmd
    } else if (action == "delete"){
        def mysqlDeleteCmd = "gcloud sql instances delete " + instanceName + " --quiet"
        println("Deleting Mssql database")
        sh mysqlDeleteCmd
    }
}

def manageGsbucket(String action, String resource){
    if (action == "create"){
        def createGsBucketCmd = "gsutil mb -p " + params.project + " -l " + params.region + " gs://" + params.envName + "-" +
          resource + "/"
        def uploadConfigCmd = "gsutil cp $HOME/.kube/config gs://" + params.envName + "-" + resource + "/metadata/kube_config"
        sh createGsBucketCmd
        sh uploadConfigCmd
    } else if (action == "delete"){
        def deleteGsBucketCmd = "gsutil rm -r gs://" + params.envName + "-" + resource
        sh deleteGsBucketCmd
    }
}

def manageSecret(String action, String resource){
    if(action == "create"){
        def mysqlEndpoint = sh (script: "gcloud sql instances describe " + params.envName + "-" + "mysql --format=\"json(ipAddresses)\" " +
          "| jq .ipAddresses[].ipAddress | tail -1 | tr -d '\"'", returnStdout: true).trim()
        def createSecretCmd = "kubectl create secret generic gcp-sql-endpoint --from-literal=sql_endpoint=$mysqlEndpoint"
        sh createSecretCmd
    }
}

def manageExtdns(String action, String resource){
    if(action == "create"){
        sh "sed 's/<project-id>/" + params.project + "/g' external-dns.yaml"
        sh "kubectl create -f external-dns"
    }
}

def manageNfs(String action, String resource){
    if(action == "create"){
        def fsIp = sh (script: "gcloud filestore instances describe " + params.envName + "-filestore --zone=" + params.region +
          "-b --format=\"json(networks)\" | jq .networks[0].ipAddresses[0] | tr -d '\"'", returnStdout: true).trim()
        sh "sed 's/<project-id>/$fsIp/g' deployment.yaml"
        sh "kubectl create namespace storage"
        sh "kubectl create -f nfs-client/serviceaccount.yaml"
        sh "kubectl create -f nfs-client/clusterrole.yaml"
        sh "kubectl create -f nfs-client/clusterrolebinding.yaml"
        sh "kubectl create -f nfs-client/serviceaccount.yaml"
        sh "kubectl create -f nfs-client/serviceaccount.yaml"
    }
}

node{
    def skipResourcesList = params.skipResources.split(',')
    if (params.action == "create"){
        flowResourceList = flowForCreateEnv.minus(skipResourcesList)
    } else if (params.action == "delete"){
        flowResourceList = flowForDeleteEnv.minus(skipResourcesList)
    }
    for(String resource in flowResourceList){
        stage(params.action + ' ' + resource){
            def methodName = 'manage' + resource.capitalize()
            "$methodName"(params.action, resource)
        }
    }
}