<?php
include('./vendor/autoload.php');
include('./config.php');

// Initialize the client
$client = new \Twilio\Rest\Client($TWILIO_ACCOUNT_SID, $TWILIO_API_KEY);
$chatClient = new \Twilio\Rest\IpMessaging($client);
$userClient = new \Twilio\Rest\IpMessaging\V1\Service\UserList($chatClient->v1, $TWILIO_IPM_SERVICE_SID);
echo json_encode($userClient->read(10));