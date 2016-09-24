#!/bin/bash

for file_type in res sepa; do
    for host_no in 0612 0440 0408; do
        scp ~/Downloads/trace-to-shijie-load.zip junchenj@ms$host_no.utah.cloudlab.us:~/
    done
    for host_no in 0132 0416 0639; do
        scp ~/Downloads/trace-to-shijie-load.zip junchenj@ms$host_no.utah.cloudlab.us:~/
    done
    for host_no in 0230 0236 0143; do
        scp ~/Downloads/trace-to-shijie-load.zip junchenj@ms$host_no.utah.cloudlab.us:~/
    done
    for host_no in 0413 0614 0623; do
        scp ~/Downloads/trace-to-shijie-load.zip junchenj@ms$host_no.utah.cloudlab.us:~/
    done
    for host_no in 0602 0432 0423 0632; do
        scp ~/Downloads/trace-to-shijie-load.zip junchenj@ms$host_no.utah.cloudlab.us:~/
    done
done
