<?php
// handle the request from proxy server
// Write info of each request into file and Read response from file
//
// author: shijie sun
// email: septimus145@gmail.com
// july, 2016

error_reporting(E_ALL);

// Encode payload of request with json and write it into file
$in = json_encode($_POST).PHP_EOL;
file_put_contents('payload.txt',$in,FILE_APPEND|LOCK_EX);

// Get response from file as response
echo $out = file_get_contents('result.txt');

?>
