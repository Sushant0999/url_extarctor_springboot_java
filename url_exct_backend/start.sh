#!/bin/bash

# Print the container's IP address
IP_ADDRESS=$(hostname -I | awk '{print $1}')
echo "Container IP Address: $IP_ADDRESS"

# Start supervisord to manage Java application and Nginx server
exec /usr/bin/supervisord -c /etc/supervisord.conf
