#!/usr/bin/python

import urllib
import urllib2
import random

if __name__ == '__main__':
    url = 'http://10.1.1.4:80/proxy.php'
    score = str(random.randint(0,100))
    print "score" + score
    values = {'os':'os_x', 'isp':'comcast', 'score':score, 'omit':'yy'*100}
    data = urllib.urlencode(values)
    #outfile = open('post.data','w')
    #outfile.write(data)
    #outfile.close()
    req = urllib2.Request(url, data)
    con = urllib2.urlopen(req)
    print con.read()
