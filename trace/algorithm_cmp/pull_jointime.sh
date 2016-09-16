#!/bin/bash

for file_type in res sepa; do
    for host_no in 0612 0440 0408; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/jointime/\*.$file_type ./result0916/jointime/ucb/
    done
    for host_no in 0132 0416 0639; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/jointime/\*.$file_type ./result0916/jointime/newucb/
    done
    for host_no in 0230 0236 0143; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/jointime/\*.$file_type ./result0916/jointime/c3/
    done
    for host_no in 0413 0614 0623; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/jointime/\*.$file_type ./result0916/jointime/akamai/
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie2/jointime/\*.$file_type ./result0916/jointime/level3/
    done
    for host_no in 0602 0432 0423 0632; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/jointime/\*.$file_type ./result0916/jointime/eg/
    done
done
