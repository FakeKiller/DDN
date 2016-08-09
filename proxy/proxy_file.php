<?php
// PHP Proxy
// Proxy table is stored in file named 'proxy_table.txt'
// The table file should have one record each line
// Format: HASH_String;IP_Address

// Author: Shijie Sun
// Email: septimus145@gmail.com
// July, 2016

// Get features
if(! get_magic_quotes_gpc())
{
   $os = addslashes ($_POST['os']);
   $isp = addslashes ($_POST['isp']);
}
else
{
   $os = $_POST['os'];
   $isp = $_POST['isp'];
}
// Hash the features
$label = md5($os . '&' . $isp);

// Load the proxy table and find the match
$entries = file('proxy_table.txt', FILE_IGNORE_NEW_LINES);
foreach ($entries as $entry) {
    $enpair = explode(';', $entry, 2);
    if ($label == $enpair[0]) {
        $url = 'http://' . $enpair[1] . '/update.php';
        break;
    }
}
//echo $url;

// Open the Curl session
$session = curl_init($url);

// If it's a POST, put the POST data in the body
if (true) {
    $postvars = '';
    while ($element = current($_POST)) {
        $postvars .= urlencode(key($_POST)).'='.urlencode($element).'&';
        next($_POST);
    }
    curl_setopt ($session, CURLOPT_POST, true);
    curl_setopt ($session, CURLOPT_POSTFIELDS, $postvars);
}

// Don't return HTTP headers. Do return the contents of the call
curl_setopt($session, CURLOPT_HEADER, false);
curl_setopt($session, CURLOPT_RETURNTRANSFER, true);

// Make the call
$result = curl_exec($session);

echo $result;
curl_close($session);

?>
