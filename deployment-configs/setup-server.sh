#!/bin/bash

# Define directories
BASE_DIR="/var/www/dms-production"
BACKEND_DIR="$BASE_DIR/backend"
FRONTEND_DIR="$BASE_DIR/frontend"

echo "Setting up server directories..."
sudo mkdir -p $BACKEND_DIR
sudo mkdir -p $FRONTEND_DIR

echo "Setting permissions..."
sudo chown -R $USER:$USER $BASE_DIR

echo "Checking for Java 21..."
if ! type -p java > /dev/null; then
    echo "Java not found. Installing OpenJDK 21..."
    sudo apt update
    sudo apt install -y openjdk-21-jdk
else
    echo "Java already installed."
fi

echo "Checking for Nginx..."
if ! type -p nginx > /dev/null; then
    echo "Nginx not found. Installing Nginx..."
    sudo apt install -y nginx
else
    echo "Nginx already installed."
fi

echo "Setup complete! Please copy the config files from 'deployment-configs/' to their respective locations:"
echo "1. sudo cp nginx-dms.conf /etc/nginx/sites-available/dms"
echo "2. sudo ln -s /etc/nginx/sites-available/dms /etc/nginx/sites-enabled/"
echo "3. sudo cp dms-backend.service /etc/systemd/system/"
echo "4. sudo systemctl daemon-reload"
echo "5. sudo systemctl enable dms-backend"
