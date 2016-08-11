<?php
// Match the request into groups, called by update.php
//
// Author: Shijie Sun
// Email: septimus145@gmail.com
// August, 2016

// Hash the features and find the match
$label = md5($os . '&' . $isp);
if ($label == "09e9519d6364de2fd40906f6c8b3b3b7")
{
    $group_id = "001";
    $cluster_id = "001";
}
?>
