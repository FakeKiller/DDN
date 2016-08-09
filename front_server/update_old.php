<?php
// Handle the request from proxy server
// Send info of each request to java server and get response
//
// Author: Shijie Sun
// Email: septimus145@gmail.com
// July, 2016

error_reporting(E_ALL);

// Create TCP connection
$service_port = 8666;
$address = "10.1.1.2";
$socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
if ($socket === false) {
    echo "socket_create() failed: reason: " . socket_strerror(socket_last_error()) . "\n";
}
$result = socket_connect($socket, $address, $service_port);
if ($result === false) {
    echo "socket_connect() failed.\nReason: ($result) " . socket_strerror(socket_last_error($socket)) . "\n";
}

// Encode payload of request with json and send it to java server
$in = json_encode($_POST);
socket_write($socket, $in, strlen($in));

// Get response from java server as response
$out = '';
while ($out = socket_read($socket, 2048)) {
     echo $out;
}

socket_close($socket);
?>
