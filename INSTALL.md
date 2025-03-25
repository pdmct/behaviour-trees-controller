## Installation on Raspberry Pi

1. Export the JAVA OPTS so the the graphviz component compiles

`export _JAVA_OPTIONS="-Djava.awt.headless=true"`

2. Build uberjar

`clj -T:build uber`

copy the jar file into the following directory

`/home/pi/bt-controller/`

3. Install as service

Copy the bt-controller.service file into `/etc/systemd/system/` directory

Run the following commands:

```
# reload config
sudo systemctl daemon-reload

#start the service
sudo systemctl start bt-controller
sudo systemctl status bt-controller

# enable it to start on boot
sudo systemctl enable bt-controller
```

4. View the logs

`journalctl -u bt-controller.service -f`

5. Useful commands

Stop the service:
`sudo systemctl stop bt-controller`

Restart the service:
`sudo systemctl restart bt-controller`

Disable auto-start:
`sudo systemctl disable bt-controller`
