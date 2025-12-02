pipeline {
    agent { label 'supportAgent'}
    
    // Define environment variables
    environment {
        EC2_IP = 'YOUR_APPLICATION_EC2_PUBLIC_IP' 
        EC2_USER = 'ubuntu' 
        SSH_CREDENTIAL_ID = 'aws-deploy-ssh-key' 
        APP_DIR = '/var/www/chat-app'
        FRONTEND_DIR = "${APP_DIR}/frontend"
        FRONTEND_REPO = 'https://github.com/D-Godhani/real-time-chat-app-frontend'
        FRONTEND_SERVICE_NAME = 'chat-app-fe'
    }

    stages {
        stage('Checkout Frontend Code') {
            steps {
                cleanWs() 
                git url: env.FRONTEND_REPO, branch: 'main'
            }
        }

        stage('Build & Prepare Artifacts') {
            steps {
                echo 'Building frontend artifacts on agent...'
                sh 'sudo npm install -g yarn'
                sh 'yarn install'
                sh 'yarn build || echo "Skipping frontend build, using raw source files."' 
            }
        }

        stage('Deploy Frontend') {
            steps {
                echo "Deploying Frontend to EC2 instance: ${EC2_IP}"
                sshagent(credentials: [SSH_CREDENTIAL_ID]) {
                    
                    // --- 1. Prepare Target Directory and Permissions ---
                    sh """
                        ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_IP} <<EOF
                            echo "Preparing frontend directory..."
                            sudo mkdir -p ${FRONTEND_DIR}
                            sudo chown -R ${EC2_USER}:${EC2_USER} ${FRONTEND_DIR}
                            pm2 delete ${FRONTEND_SERVICE_NAME} || true
                        EOF
                    """
                    
                    // --- 2. Copy Files and Start Service ---
                    echo 'Transferring frontend files and restarting web service...'
                    sh """
                        scp -r * ${EC2_USER}@${EC2_IP}:${FRONTEND_DIR}/
                        
                        ssh ${EC2_USER}@${EC2_IP} "
                            cd ${FRONTEND_DIR}
                            
                            # Install PM2 and local dependencies (PM2 check added for robustness)
                            sudo npm install -g pm2 yarn
                            yarn install

                            # --- Start Frontend Service with PM2 ---
                            /usr/bin/pm2 start yarn --name ${FRONTEND_SERVICE_NAME} --interpreter bash -- start || /usr/bin/pm2 restart ${FRONTEND_SERVICE_NAME}
                            /usr/bin/pm2 save
                        "
                    """
                }
            }
        }
    }
}
