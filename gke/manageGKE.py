import googleapiclient.discovery
from google.cloud import container_v1
import os
import time
os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = "account.json"
client = container_v1.ClusterManagerClient()
container_api = googleapiclient.discovery.build('container', 'v1beta1')
project = "flow-on-k8s-test"
region = "us-east1"
clusterName = 'native'
version = "1.15"
cluster = {
  "name": clusterName,
  "description": "GKE cluster created by python sdk api",
  "initial_node_count": 1,
  "network": "default",
  "subnetwork": "default",
  "initialClusterVersion": version
}
#operation = client.delete_cluster(name='projects/' + project + '/locations/' + region + '/clusters/' + clusterName)
operation = client.create_cluster(cluster= cluster,parent='projects/' + project + '/locations/' + region)
while True:
    result = container_api.projects().locations().operations().get(name = 'projects/' +
             project + '/locations/' + region + '/operations/' + operation.name).execute()
    print("result: {}".format(result))
    if result['status'] == 'DONE':
        print("done.")
        exit(0)
    elif 'error' in result:
        raise Exception(result['error'])
    else:
        print('ongoing')
    time.sleep(1)