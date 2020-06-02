resource "google_compute_network" "vpc" {
  name                    = "${var.clusterName}-vpc"
  auto_create_subnetworks = "false"
}