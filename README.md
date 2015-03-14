## Introduction
You may encounter such a situation:

* "somesite.com" to "192.168.2.3" and then visit "somesite.com"
* 5 minutes later, map "somesite.com" to "192.168.2.4" and then visit it

To change the ip-domain mapping in your machine, you have to edit your hosts file, do some changes, and save it. Frequently doing it may be annoying.

If you are a linux user, you can now make use of __dacuoxian__ to change the mappings with a single shell command.

"__dacuoxian(搭错线)__", which means "connect false line" in Chinese, is a linux shell script executed by /bin/sh. Make dacuoxian your friend so that you won't connect false lines (I mean not to do false ip-domain mappings, of course)



## Try it
* download all the files 
* `chmod +x dacuoxian.sh`
* `sudo ./dacuoxian.sh google-hosts-sample` # Note "sudo" may not be available in linux systems other than Ubuntu.
* Now check your /etc/hosts file and you can see what's going on
* Then try re-mapping the domains with another group of IPs: `sudo ./dacuoxian.sh google-another-group-of-hosts-sample`. And check your /etc/hosts again.
* And you can create your own hosts snippets such as /somepath/foo-hosts1, /somepath/foo-hosts2, /somepath/bar-hosts1 and /somepath/bar-hosts2, and do
* `sudo ./dacuoxian.sh /somepath/foo-hosts1` #add foo-hosts1 to /etc/hosts
* `sudo ./dacuoxian.sh /somepath/bar-hosts1` #add bar-hosts1. Doing this will not remove foo-hosts1
...
* To disable mappings added by dacuoxian, do this: `sudo ./dacuoxian.sh disable google-hosts-sample`
