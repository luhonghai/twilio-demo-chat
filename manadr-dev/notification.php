<?php
include('./vendor/autoload.php');
include('./config.php');

use Twilio\Jwt\AccessToken;
use Twilio\Jwt\Grants\ConversationsGrant;


/// Initialize the client
$client = new IPMessagingServices_Twilio($TWILIO_ACCOUNT_SID, $TWILIO_API_KEY);

// Update the service webhooks
$service = $client->services->get($TWILIO_IPM_SERVICE_SID);
$response = $service->update(array(
    "Notifications.NewMessage.Enabled" => "true",
    "Notifications.NewMessage.Template" => "A New message in ${CHANNEL} from ${USER}: ${MESSAGE}",
));
?>
