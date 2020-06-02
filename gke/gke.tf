variable "project" {
  type = string
}

variable "clusterName" {
  type = string
}

variable "region" {
  type = string
  default = "us-east1"
}

variable "clusterVersion" {
  type = string
  default = "1.14"
}

variable "tier" {
  type = string
  default = "n1-standard-1"
}

variable "diskSize" {
  type = string
  default = "20"
}

variable "machineType" {
  type = string
  default = "UBUNTU"
}

variable "saId" {
  type = string
}

provider "google" {
  credentials = file("../account.json")
  project     = var.project
  region      = var.region
}

data "google_service_account" "saId" {
  account_id = var.saId
}

resource "google_container_cluster" "gke-cluster" {
  name     = var.clusterName
  location = var.region
  remove_default_node_pool = true
  initial_node_count       = 1
  description = "GKE cluster created by terraform"
  ip_allocation_policy {
    cluster_ipv4_cidr_block = ""
  }
  min_master_version = var.clusterVersion
  network = google_compute_network.vpc.name
  subnetwork = google_compute_subnetwork.subnet.name
}

resource "google_container_node_pool" "node-pool" {
  name = "custom-node-pool"
  location   = var.region
  cluster    = google_container_cluster.gke-cluster.name
  initial_node_count = 1
  version = var.clusterVersion
  management {
    auto_upgrade = false
  }

  autoscaling {
    max_node_count = 2
    min_node_count = 1
  }

  node_config {
    preemptible  = false
    machine_type = var.tier
    disk_size_gb = var.diskSize
    image_type = var.machineType
    service_account = data.google_service_account.saId.email
    metadata = {
      disable-legacy-endpoints = "true"
    }

    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/ndev.clouddns.readwrite"
    ]
  }
}