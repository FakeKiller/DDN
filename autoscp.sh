#!/bin/bash


if [ $# -lt 1  ];
then
    echo "Usage: sudo $0 host_name"
    exit 1
fi

scp -r ~/CMU_Lab/DDN/spark $1:~/
scp -r ~/CMU_Lab/DDN/kafka $1:~/
scp -r ~/CMU_Lab/DDN/front_server $1:~/
scp -r ~/CMU_Lab/DDN/trace/algorithm_cmp/trace_parser.py $1:~/
scp -r ~/Downloads/trace-to-shijie.zip $1:~/
scp ./onehost_deploy.sh $1:~/
scp ./start_tmux.sh $1:~/
