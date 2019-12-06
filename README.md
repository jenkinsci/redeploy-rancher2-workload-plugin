# redeploy-rancher2-workload
A simple Jenkins plugin to redeploy Rancher2.x workload



## Prerequisite

- Jenkins version >= 2.7.3
- Rancher version >= 2.0
- Jenkins Credentials Plugin >= 2.1.0



## Limitation

- only support **redeploy** an exists workload in Rancher2.x



## Install

download hpi package from release page or search this plugin in jenkins 



## Usage

### 1. Get Rancher2 Bearer Token

- login into Rancher2.x web UI, click top right corner avatar and select "API & Keys"
- click "Add Key"
- in popup window, type some words in description, and set scope to "no scope", then click "Create"
- save the **Endpoint** and **Bearer Token** value in result dialog to another place. we will use these in Jenkins very soon.
- official document: https://rancher.com/docs/rancher/v2.x/en/user-settings/api-keys/



### 2. Add Jenkins Credential

- login Jenkins Dashboard
- on left navigation menu, find and click "Credentials"
- in store list, click "Jenkins" store
- in domain list, click "Global credentials" 
- you will find a " Add Credentials" menu in left navigation menu. click it!
- in Credentials page, change kind to "Rancher2.x API Keys"
- paste **Endpoint** and **Bearer Token** you just saved in step 1
- if you use self-signed SSL certification,  please check "Trust certification"
- click "Test Connection" button to test.
- type a well named ID, eg: rancher, and click "OK" button to save



### 3. For Jenkins Free Style Job

- create a freestyle job in Jenkins
- at **Build** section, click "Add build step" drop-down menu
- you should see a menu it named: "Redeploy Rancher2.x Workload", click it!
- click each field help button to see document



### 4. For Jenkins Pipeline Job

- create a pipeline job
- at **Pipeline** section, click "Pipeline Syntax" link
- in opened browser tab, select "rancherRedeploy" in "Sample Step"
- fill that form and click "Generate Pipeline Script"
- the output like:

```
rancherRedeploy alwaysPull: true, credential: 'rancher', images: 'busybox:lastest', workload: '/project/c-h4hxd:p-c9j8z/workloads/deployment:default:busybox'
```

- back to pipeline job, and edit pipeline content like:

  ```
  node {
     rancherRedeploy alwaysPull: true, credential: 'rancher', images: 'busybox:lastest', workload: '/project/c-h4hxd:p-c9j8z/workloads/deployment:default:busybox'
  }
  ```