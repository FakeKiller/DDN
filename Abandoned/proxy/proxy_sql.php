<?php
// PHP Proxy
// Proxy table is stord in MySQL

// Author: Shijie Sun
// Email: septimus145@gmail.com
// July, 2016

// MySQL configuration
$dbhost = 'localhost';
$dbuser = 'root';
$dbpass = '123';
$conn = mysql_connect($dbhost, $dbuser, $dbpass);
if(! $conn)
{
  die('Could not connect: ' . mysql_error());
}

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

// Query
$sql = 'SELECT server_ip, server_port
        FROM proxy';
mysql_select_db('httpd_proxy');
$retval = mysql_query($sql, $conn);
if(! $retval)
{
  die('Could not get data: ' . mysql_error());
}
// Get first match entry
$row = mysql_fetch_array($retval, MYSQL_ASSOC);
mysql_close($conn);

$url = "http://" . $row['server_ip'] . ":" . (string)$row['server_port'] . "/update.php";
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
