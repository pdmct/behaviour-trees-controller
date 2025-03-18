## Behaviour Trees example
A small example program for playing around with behaviour trees.

This is an implementation of behaviour trees for controlling a home battery and EV charger.

The behaviour tree is intended to look something like this:
![Battery Controller Tree Diagram](docs/Battery_controller.png)

### Install

`git clone https://github.com/pdmct/behaviour-trees-controller.git`

You will also need to have in your environment the ENV VARS listed in the `config.edn` file.
Use as your own risk ... there are better (more secure) ways to do this.

### ToDo

1. Implement the better ways


Uses the `aido` library: https://github.com/mmower/aido/tree/master.

MIT License -- see LICENSE file for details.
