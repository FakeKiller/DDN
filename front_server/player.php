<?php
// Response to the request and forward the update
//
// Author: Shijie Sun
// Email: septimus145@gmail.com
// August, 2016

$path = '/var/www/info';
set_include_path(get_include_path() . PATH_SEPARATOR . $path);
$features = explode("\t", $_POST["payload"]);

include 'match.php';

$group_id = $features[4]; // take asn as group

// request
if ($_POST['method'] == 'request') {
    $decision_list = array_slice($features, 15);
    $decision = file_get_contents($path . '/d_' . $group_id);
    if (empty($decision) || !in_array($decision, $decision_list)) {
        $decision = $decision_list[array_rand($decision_list, 1)];
    }
    echo $decision;
}

// update
if ($_POST['method'] == 'update') {
    // Encode the info with json and write it into file
    $info = array(
        "update" => $_POST["payload"],
        "group_id" => $group_id
    );
    $in = json_encode($info, JSON_UNESCAPED_SLASHES).PHP_EOL;
    file_put_contents($path . '/info_queue',$in,FILE_APPEND|LOCK_EX);
}

?>
