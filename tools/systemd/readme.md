# Host Configuration Init (systemd) Setup

## Pre-requisite :
1. Cluster seed application is up and running.
2. IP address and Port number of Seed application is known and configured as environment variable in the systemd service definition
3. All the necessary binaries are available on the host machine 

## Steps to enable and start service
1. Create a service definition file as per the provided template per single host machine
2. Go to /etc/systemd/system/multi-user.target.wants directory
3. Use systemctl's enable command to activate service. For example: `sudo systemctl enable <absolute path of service>`
4. Start the service using following command `sudo systemctl start <name of service>`
5. Check the status of service with `sudo systemctl status <name of service>` 


## Troubleshooting
To view service specific logs, use this command `sudo journalctl -u <name of service>`