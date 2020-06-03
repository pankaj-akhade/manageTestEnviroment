#!/usr/bin/env groovy
node{
    try {
        stage('Create Network'){
            cmd = "gcloud compute networks create my-vpc --project=" + params.project +
                   " --subnet-mode=custom --bgp-routing-mode=regional"
            sh cmd
        }
    } catch(Exception e){
        println(e)
    }
}