$(function() {
    var conversationsClient;
    var activeConversation;
    var previewMedia;
    var identity;
    var username;

// Check for WebRTC
    if (!navigator.webkitGetUserMedia && !navigator.mozGetUserMedia) {
        alert('WebRTC is not available in your browser.');
    }

// Successfully connected!
    function clientConnected() {
        document.getElementById('invite-controls').style.display = 'block';
        log("Connected to Twilio. Listening for incoming Invites as '" + conversationsClient.identity + "'");

        conversationsClient.on('invite', function (invite) {
            log('Incoming invite from: ' + invite.from);
            invite.accept().then(conversationStarted);
        });

        // Bind button to create conversation
        document.getElementById('button-invite').onclick = function () {
            var inviteTo = document.getElementById('invite-to').value;
            if (activeConversation) {
                // Add a participant
                activeConversation.invite(inviteTo);
            } else {
                // Create a conversation
                var options = {};
                if (previewMedia) {
                    options.localMedia = previewMedia;
                }

                conversationsClient.inviteToConversation(inviteTo, options).then(conversationStarted, function (error) {
                    log('Unable to create conversation');
                    console.error('Unable to create conversation', error);
                });
            }
        };
    }

// Conversation is live
    function conversationStarted(conversation) {
        log('In an active Conversation');
        activeConversation = conversation;
        // Draw local video, if not already previewing
        if (!previewMedia) {
            conversation.localMedia.attach('#local-media');
        }

        // When a participant joins, draw their video on screen
        conversation.on('participantConnected', function (participant) {
            log("Participant '" + participant.identity + "' connected");
            participant.media.attach('#remote-media');
        });

        // When a participant disconnects, note in log
        conversation.on('participantDisconnected', function (participant) {
            log("Participant '" + participant.identity + "' disconnected");
        });

        // When the conversation ends, stop capturing local video
        conversation.on('disconnected', function (conversation) {
            log("Connected to Twilio. Listening for incoming Invites as '" + conversationsClient.identity + "'");
            conversation.localMedia.stop();
            conversation.disconnect();
            activeConversation = null;
        });
    }

//  Local video preview
    document.getElementById('button-preview').onclick = function () {
        if (!previewMedia) {
            previewMedia = new Twilio.Conversations.LocalMedia();
            Twilio.Conversations.getUserMedia().then(
                function (mediaStream) {
                    previewMedia.addStream(mediaStream);
                    previewMedia.attach('#local-media');
                },
                function (error) {
                    console.error('Unable to access local media', error);
                    log('Unable to access Camera and Microphone');
                });
        };
    };

// Activity log
    function log(message) {
        document.getElementById('log-content').innerHTML = message;
    }


    // Get handle to the chat div
    var $chatWindow = $('#messages');

    // Manages the state of our access token we got from the server
    var accessManager;

    // Our interface to the IP Messaging service
    var messagingClient;

    // A handle to the "general" chat channel - the one and only channel we
    // will have in this sample app
    var generalChannel;

    // Helper function to print info messages to the chat window
    function print(infoMessage, asHtml) {
        var $msg = $('<div class="info">');
        if (asHtml) {
            $msg.html(infoMessage);
        } else {
            $msg.text(infoMessage);
        }
        $chatWindow.append($msg);
    }

    // Helper function to print chat message to the chat window
    function printMessage(fromUser, message) {
        var $user = $('<span class="username">').text(fromUser + ':');
        if (fromUser === username) {
            $user.addClass('me');
        }
        var $message = $('<span class="message">').text(message);
        var $container = $('<div class="message-container">');
        $container.append($user).append($message);
        $chatWindow.append($container);
        $chatWindow.scrollTop($chatWindow[0].scrollHeight);
    }

    // Alert the user they have been assigned a random username
    print('Logging in...');

    // Get an access token for the current user, passing a username (identity)
    // and a device ID - for browser-based apps, we'll always just use the
    // value "browser"

    // Set up channel after it has been found
    function setupChannel() {
        // Join the general channel
        generalChannel.join().then(function(channel) {
            print('Joined channel as '
                + '<span class="me">' + username + '</span>.', true);
        });

        // Listen for new messages sent to the channel
        generalChannel.on('messageAdded', function(message) {
            printMessage(message.author, message.body);
        });
    }

    // Send a new message to the general channel
    var $input = $('#chat-input');
    $input.on('keydown', function(e) {
        if (e.keyCode == 13) {
            generalChannel.sendMessage($input.val())
            $input.val('');
        }
    });

    $('#btnLogin').on('click', function(e) {
        $('#login-form').hide();
        loginTwilio($('#txtUsername').val());
    });



    function loginTwilio(id) {
        $('#remote-media').show();
        $('#controls').show();
        $('#mes-container').show();
        username = id;
        console.log("Login username " + id);
        $.getJSON('/token_ipm.php', {
            identity: username,
            endpointId: username + '-browser'
        }, function(data) {
            console.log("Token : " + data.token);
            print('You have been assigned a random username of: '
                + '<span class="me">' + username + '</span>', true);

            // Initialize the IP messaging client
            accessManager = new Twilio.AccessManager(data.token);
            messagingClient = new Twilio.IPMessaging.Client(accessManager);

            // Get the general chat channel, which is where all the messages are
            // sent in this simple application
            print('Attempting to join "general" chat channel...');
            var promise = messagingClient.getChannelByUniqueName('general');
            promise.then(function(channel) {
                generalChannel = channel;
                if (!generalChannel) {
                    // If it doesn't exist, let's create it
                    messagingClient.createChannel({
                        uniqueName: 'general',
                        friendlyName: 'General Chat Channel'
                    }).then(function(channel) {
                        console.log('Created general channel:');
                        console.log(channel);
                        generalChannel = channel;
                        setupChannel();
                    });
                } else {
                    console.log('Found general channel:');
                    console.log(generalChannel);
                    setupChannel();
                }
            });
        });
        $.getJSON('/token.php', {
            identity: username
        }, function(data) {
            identity = data.identity;
            var accessManager = new Twilio.AccessManager(data.token);

            // Check the browser console to see your generated identity.
            // Send an invite to yourself if you want!
            console.log(identity);

            // Create a Conversations Client and connect to Twilio
            conversationsClient = new Twilio.Conversations.Client(accessManager);
            conversationsClient.listen().then(clientConnected, function (error) {
                log('Could not connect to Twilio: ' + error.message);
            });
        });

    }
});