<?php
include('./vendor/autoload.php');
include('./config.php');
include('./randos.php');

use Twilio\Jwt\AccessToken;
use Twilio\Jwt\Grants\IpMessagingGrant;
use Twilio\Jwt\Grants\ConversationsGrant;

// An identifier for your app - can be anything you'd like
$appName = 'TwilioChatDemo';
// choose a random username for the connecting user
$identity = $_GET['identity'];
// A device ID is passed as a query string parameter to this script
$deviceId = $_GET['device'];
$endpointId = $_GET['endpointId'];

// Create access token, which we will serialize and send to the client
$token = new AccessToken(
    $TWILIO_ACCOUNT_SID, 
    $TWILIO_API_KEY, 
    $TWILIO_API_SECRET, 
    3600, 
    $identity
);

// Grant access to IP Messaging
$grant = new IpMessagingGrant();
$grant->setServiceSid($TWILIO_IPM_SERVICE_SID);
$grant->setEndpointId($endpointId);
$grant->setPushCredentialSid('CR652d1ad71f1e86cca89dbc9a9f2fd4c1');
$token->addGrant($grant);

// Grant access to Conversation
$grantConversation = new ConversationsGrant();
$grantConversation->setConfigurationProfileSid($TWILIO_CONFIGURATION_SID);
$token->addGrant($grantConversation);

// return serialized token and the user's randomly generated ID
echo json_encode(array(
    'identity' => $identity,
    'token' => $token->toJWT(),
));
