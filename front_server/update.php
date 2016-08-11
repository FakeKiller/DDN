<?php
// Match the request and write its info and group id into file
//
// Author: Shijie Sun
// Email: septimus145@gmail.com
// August, 2016

$path = '/var/www/info';
set_include_path(get_include_path() . PATH_SEPARATOR . $path);

// Get features
if(! get_magic_quotes_gpc())
{
   $os = addslashes ($_POST['os']);
   $isp = addslashes ($_POST['isp']);
   $score = addslashes ($_POST['score']);
}
else
{
   $os = $_POST['os'];
   $isp = $_POST['isp'];
   $score = $_POST['score'];
}

include 'match.php';

// Encode the info with json and write it into file
$info = array(
    "update" => array(
        "score" => $score
    ),
    "group_id" => $group_id,
    "cluster_id" => $cluster_id
);
$in = json_encode($info).PHP_EOL;
file_put_contents('/var/www/info/info_queue',$in,FILE_APPEND|LOCK_EX);

// Get result from file as response
$out = file_get_contents('/var/www/info/d_'.$group_id);
if (empty($out))
    echo "Oops";
else
    echo $out;
?>
