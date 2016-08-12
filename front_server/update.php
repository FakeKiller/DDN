<?php
// Match the request and write its info and group id into file
//
// Author: Shijie Sun
// Email: septimus145@gmail.com
// August, 2016

$path = '/var/www/info';
set_include_path(get_include_path() . PATH_SEPARATOR . $path);

include 'match.php';

if (empty($group_id))
    $group_id = "group1";

// Encode the info with json and write it into file
$info = array(
    "update" => $_POST,
    "group_id" => $group_id
);

$in = json_encode($info, JSON_UNESCAPED_SLASHES).PHP_EOL;
//echo $in;
file_put_contents('/var/www/info/info_queue',$in,FILE_APPEND|LOCK_EX);

// Get result from file as response
$out = file_get_contents('/var/www/info/d_'.$group_id);
if (empty($out))
    echo "Oops";
else
    echo $out;
?>
