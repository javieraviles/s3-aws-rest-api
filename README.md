# s3-aws-rest-api

## Run API Locally
 - paste your AWS credentials in /c/users/<user>/.aws/credentials (create file if not there)
 - build and run application (mvn clean package && jar -jar <name>.jar)
 - postman requests should work against https://localhost/...

## Run API in AWS EC2
 - create new instance using ubuntu server AMI
 - t2.micro free tier is fine
 - Assign IAM role
 - Default storage (8Gb) is fine
 - In security groups, add Rule for HTTPS (limit inbound IP range if desired)
 - Finish creation
 - Connect instance using ssh (right click - connect - copy command) and execute following steps:
   - install OpenJDK8
   - install maven
   - git clone repo with code
   - build and run application (mvn clean package && sudo jar -jar <name>.jar). Running jar with sudo is important as port 443 is protected.
 - Now postman requests should work against your instance public DNS

## Make it run as Linux service (application running on instance startup)
 - $ sudo vi /usr/local/bin/java_s3_api.sh

    ```
        #!/bin/sh 
        SERVICE_NAME=java_s3_api
        PATH_TO_JAR=/home/ubuntu/s3-aws-rest-api/target/p1-computacion-nube-1.0.0.jar 
        PID_PATH_NAME=/tmp/java_s3_api-pid 
        case $1 in 
        start)
            echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then 
            sudo nohup java -jar $PATH_TO_JAR /tmp 2>> /dev/null >>/dev/null &      
                        echo $! > $PID_PATH_NAME  
            echo "$SERVICE_NAME started ..."         
        else 
            echo "$SERVICE_NAME is already running ..."
        fi
        ;;
        stop)
        if [ -f $PID_PATH_NAME ]; then
                PID=$(cat $PID_PATH_NAME);
                echo "$SERVICE_NAME stoping ..." 
                kill $PID;         
                echo "$SERVICE_NAME stopped ..." 
                rm $PID_PATH_NAME       
        else          
                echo "$SERVICE_NAME is not running ..."   
        fi    
        ;;    
        restart)  
        if [ -f $PID_PATH_NAME ]; then 
            PID=$(cat $PID_PATH_NAME);    
            echo "$SERVICE_NAME stopping ..."; 
            kill $PID;           
            echo "$SERVICE_NAME stopped ...";  
            rm $PID_PATH_NAME     
            echo "$SERVICE_NAME starting ..."  
            sudo nohup java -jar $PATH_TO_JAR /tmp 2>> /dev/null >> /dev/null &            
            echo $! > $PID_PATH_NAME  
            echo "$SERVICE_NAME started ..."    
        else           
            echo "$SERVICE_NAME is not running ..."    
            fi     ;;
        esac
    ```
 - $ sudo chmod +x /usr/local/bin/java_s3_api.sh
 - $ sudo vi /etc/systemd/system/java_s3_api.service
    
    ```
        [Unit]
        Description = s3 java rest api
        After network.target = java_s3_api.service
        [Service]
        Type = forking
        Restart=always
        RestartSec=1
        SuccessExitStatus=143 
        ExecStart = /usr/local/bin/java_s3_api.sh start
        ExecStop = /usr/local/bin/java_s3_api.sh stop
        ExecReload = /usr/local/bin/java_s3_api.sh reload
        [Install]
        WantedBy=multi-user.target
    ```
 - $ sudo systemctl daemon-reload
 - $ sudo systemctl enable java_s3_api


## Create AMI from your EC2 instance
 - Select instance -> actions -> image -> create image
 - Give name and description to new AMI
 - Adjust volume if needed
 - New AMI will now appear under Images -> AMIs list
 - When launching a new EC2 instance, left menu -> My AMIs -> can select the created one
 - Another workaround would be create an instance template and launh new instance from such template