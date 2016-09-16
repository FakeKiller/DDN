#!/bin/bash

for file_type in res sepa; do
    for host_no in 0612 0440 0408; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/bufratio/\*.$file_type ./result0916/bufratio/ucb/
    done
    for host_no in 0132 0416 0639; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/bufratio/\*.$file_type ./result0916/bufratio/newucb/
    done
    for host_no in 0230 0236 0143; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/bufratio/\*.$file_type ./result0916/bufratio/c3/
    done
    for host_no in 0413 0614 0623; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/bufratio/\*.$file_type ./result0916/bufratio/akamai/
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie2/bufratio/\*.$file_type ./result0916/bufratio/level3/
    done
    for host_no in 0602 0432 0423 0632; do
        scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie/bufratio/\*.$file_type ./result0916/bufratio/eg/
    done
done
