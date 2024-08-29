# HOW TO setup you development environment 

## JAVA
- SAP Commerce 2211 requires JDK 17 or later. It is fully compatible with SAP Machine 17.
 
## Folder Structure

The root directory structure should be:
> * sagaji
>   * custom-cx
>   * hybris

## Clone the repository  
  
1. Create a folder in your computer with called 'sagaji'.
2. Clone the repository using http
   > git clone git@github.com:Seidor-Digital/sagaji.git custom-cx
3. Checkout the "dev" branch of the project in order to get all the latest code in your local repository. For that purpose, open the project in Source Tree (or GitKraken) and double click the "dev" branch or run `git checkout dev` on the `custom-cx` folder of the project.

## Setup the environment  
  
1. Extract the `hybris` folder of proper SAP Commerce Cloud version (2211.XX) into your project folder.
2. Extract the contents of the `hybris\bin\modules` folder of proper SAP Commerce Cloud Extension Pack version (2211.XX) into the `hybris\bin\modules` folder of your project.
3. Open the console, navigate to `hybris\bin\platform` and execute `. ./setantenv.sh` or `. setantenv.bat` then `ant`. Next press Enter to apply the "develop" configuration to your project (default). This will create several configuration folders into your hybris folder.
4. Now move to the folder `custom-cx` of your project and run `ant` in order to copy all configuration files related to the local environment.
5. Next move to `hybris\bin\platform` and run `ant clean all`. This will compile all your code.
6. Now run the server: `./hybrisserver.sh` or `hybrisserver.bat`.
7. Initialize the server: https://localhost:9002/ -> Platform -> Initialization -> Initialize
8. If you want to connect and use Cloud hotfolder, then copy the following properties into the local.properties file and restart the server.

#Hotfolder
cluster.node.groups=integration,yHotfolderCandidate
azure.hotfolder.storage.account.connection-string=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;QueueEndpoint=http://127.0.0.1:10001/devstoreaccount1;TableEndpoint=http://127.0.0.1:10002/devstoreaccount1
azure.hotfolder.storage.account.name=devstoreaccount1

azure.hotfolder.storage.container.hotfolder=${tenantId}/hotfolder
azure.hotfolder.storage.container.name=hybris


To modify initialization default sites change `initialization.import.stores`
  
## Develop  
  
1. Create a branch from "dev" called "feature/".
2. Open the project in IntelliJ. Go to "idea" folder and go to File -> New -> Project from existing sources.
2. Write your code (always in English) and commit your changes. Give your commits significant names.
3. When you finish a feature, create a Merge Request to branch "dev" and assign it to the person in charge of reviewing the code.

### Basic considerations  
  
#### General  
  
If you change to another branch and want to guarantee that everything is perfectly clean, run all the following commands:
1. ant development (from the `custom-cx` folder)
2. ant clean all (from hybris/bin/platform)

In order to start your sever in Debug mode, run `./hybrisserver.sh debug` or `hybrisserver.bat debug`. Next, create a Debug configuration in IntelliJ:
1. Run -> Edit Configurations...
2. New (+) -> Remote.
3. Give it a name (ex. hybris debug). Set host: localhost, port: 8000. Click OK.
4. Run your Debug configuration in order to attach the debugger to the running server.
  
Always try to keep your code well-formatted: `Ctrl + Alt + L` in IntelliJ and remove unnecessary imports in Java files: `Ctrl + Alt + O`.
  
  
Cloud hotfolders local setup - in order to work with Cloud hotflder locally there is a need to set up Azure Blobstorage, the easiest way is to use Docker image:
1. Execute -> docker run -p 10000:10000 -p 10001:10001 mcr.microsoft.com/azure-storage/azurite
2. In local.properties -> cluster.node.groups=integration,yHotfolderCandidate

#### Backend  
  
* All new items and attributes should be have an unique attribute and index.
* All custom Java classes and interfaces should start by `Sgj*` or `DefaultSgj*`
