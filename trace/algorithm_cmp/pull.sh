#!/bin/bash
for feature_type in jointime; do
    for file_type in res sepa; do
        for host_no in 0612 0440 0408; do
            scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie-load/$feature_type/\*.$file_type ./result0917/$feature_type/ucb/
        done
        for host_no in 0132 0416 0639; do
            scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie-load/$feature_type/\*.$file_type ./result0917/$feature_type/newucb/
        done
        for host_no in 0230 0236 0143; do
            scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie-load/$feature_type/\*.$file_type ./result0917/$feature_type/c3/
        done
        for host_no in 0413 0614 0623; do
            scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie-load/$feature_type/\*.$file_type ./result0917/$feature_type/akamai/
            scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie-load2/$feature_type/\*.$file_type ./result0917/$feature_type/level3/
        done
        for host_no in 0602 0432 0423 0632; do
            scp -r junchenj@ms$host_no.utah.cloudlab.us:~/trace-to-shijie-load/$feature_type/\*.$file_type ./result0917/$feature_type/eg/
        done
    done
done
