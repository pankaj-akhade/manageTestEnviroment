#!/usr/bin/env groovy

try {
    stage('Hello World'){
        println("Hello World")
    }
} catch(Exception e){
    println(e)
}
