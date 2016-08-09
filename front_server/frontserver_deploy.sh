#!/bin/bash

# Auto install httpd and configure the environment
#
# Author: Shijie Sun
# Email: septimus145@gmail.com
# August, 2016

if [[ $UID != 0  ]]; then
    echo "Please run this script with sudo:"
    echo "sudo $0 $*"
    exit 1
fi

# Install editor and httpd
sudo apt-get update
sudo apt-get install -y vim tmux
sudo apt-get install -y default-jre
JAVA_HOME=$(sudo update-java-alternatives -l | head -n 1 | cut -f3 -d' ')
echo JAVA_HOME=\"$JAVA_HOME\" | sudo tee --append /etc/environment
export JAVA_HOME=$JAVA_HOME
sudo apt-get install -y apache2 php5 libapache2-mod-php5

# Configure the httpd
sudo mv match.php /var/www/html
sudo mkdir /var/www/info
sudo chmod 777 /var/www/info
sudo mv groupEnv.conf /var/www/info
echo -e "\n# Include the group information\nIncludeOptional /var/www/info/*Env.conf" \
    | sudo tee --append /etc/apache2/apache2.conf
sudo sed -i -e "s/\(KeepAlive \).*/\1"Off"/" \
    /etc/apache2/apache2.conf
sudo service apache2 reload
