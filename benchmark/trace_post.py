#!/usr/bin/python

import urllib
import urllib2
import random

if __name__ == '__main__':
    url1 = 'http://10.1.1.4:80/update.php'
    url2 = 'http://10.1.1.6:80/update.php'
    with open("synthetic-video1.txt") as f:
        keys = f.readline().split('\t')
        record = f.readline()
        cnt = 0
        while record:
            values = record.split('\t')
            data = dict(zip(keys, values))
            data['score'] = random.randint(0,100)
            if cnt == 0:
                cnt = 1
                url = url1
            else:
                cnt = 0
                url = url2
            post_data = urllib.urlencode(data)
            req = urllib2.Request(url, post_data)
            con = urllib2.urlopen(req)
            print con.read()
            record = f.readline()
